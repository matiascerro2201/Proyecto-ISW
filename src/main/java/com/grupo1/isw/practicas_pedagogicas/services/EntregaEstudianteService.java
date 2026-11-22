package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository.ContextoEntrega;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository.EntregaDelEstudiante;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * N-10: el estudiante sube o reemplaza el PDF de cada entrega hasta su fecha límite, y nada después.
 * Solo se pueden ver y entregar las entregas de semestres ya iniciados.
 */
@Service
public class EntregaEstudianteService {
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Estados que ve el estudiante en cada entrega. */
    public static final String ENTREGADA = "ENTREGADA";
    public static final String PENDIENTE = "PENDIENTE";
    public static final String NO_ENTREGADA = "NO_ENTREGADA";

    /** Una entrega con lo que el estudiante puede hacer hoy. La nota solo se muestra si está publicada. */
    public record EntregaVista(EntregaDelEstudiante datos, String estado, boolean puedeSubir, boolean vencida) {
        public boolean notaVisible() {
            return datos.publicada() && datos.nota() != null;
        }
    }

    /** Las entregas de una inscripción (una asignatura en un semestre). */
    public record GrupoEntregas(Long idInscripcion, String asignatura, String periodo, List<EntregaVista> entregas) {
    }

    public record DescargaEntrega(Path ruta, String nombreSugerido) {
    }

    private final EntregaEstudianteRepository repositorio;
    private final AlmacenArchivos almacen;
    private final Clock reloj;

    @Autowired
    public EntregaEstudianteService(EntregaEstudianteRepository repositorio, AlmacenArchivos almacen) {
        this(repositorio, almacen, Clock.systemDefaultZone());
    }

    public EntregaEstudianteService(EntregaEstudianteRepository repositorio, AlmacenArchivos almacen, Clock reloj) {
        this.repositorio = repositorio;
        this.almacen = almacen;
        this.reloj = reloj;
    }

    /** El plazo vence al terminar el día de la fecha límite. Sin fecha límite el plazo no vence. */
    public static boolean plazoVencido(LocalDate fechaLimite, LocalDate hoy) {
        return fechaLimite != null && hoy.isAfter(fechaLimite);
    }

    @Transactional(readOnly = true)
    public List<GrupoEntregas> listar(long idEstudiante) {
        LocalDate hoy = LocalDate.now(reloj);
        Map<Long, List<EntregaVista>> porInscripcion = new LinkedHashMap<>();
        Map<Long, EntregaDelEstudiante> primera = new LinkedHashMap<>();
        for (EntregaDelEstudiante e : repositorio.delEstudiante(idEstudiante)) {
            primera.putIfAbsent(e.idInscripcion(), e);
            porInscripcion.computeIfAbsent(e.idInscripcion(), k -> new ArrayList<>()).add(vista(e, hoy));
        }
        List<GrupoEntregas> grupos = new ArrayList<>();
        for (Map.Entry<Long, List<EntregaVista>> grupo : porInscripcion.entrySet()) {
            EntregaDelEstudiante base = primera.get(grupo.getKey());
            grupos.add(new GrupoEntregas(grupo.getKey(), base.asignatura(), base.periodo(), grupo.getValue()));
        }
        return grupos;
    }

    private EntregaVista vista(EntregaDelEstudiante e, LocalDate hoy) {
        boolean vencida = plazoVencido(e.fechaLimite(), hoy);
        String estado = e.tieneArchivo() ? ENTREGADA : (vencida ? NO_ENTREGADA : PENDIENTE);
        boolean puedeSubir = !vencida && "EN_CURSO".equals(e.estadoInscripcion()) && e.nota() == null;
        return new EntregaVista(e, estado, puedeSubir, vencida);
    }

    /**
     * Sube el PDF o reemplaza el anterior.
     *
     * @return true si reemplazó un archivo que ya existía
     * @throws IllegalStateException    si el plazo venció, el semestre no está iniciado o ya hay una nota
     * @throws IllegalArgumentException si el archivo no es un PDF válido
     */
    @Transactional
    public boolean subir(long idEstudiante, long idInscripcion, long idEntrega, MultipartFile archivo) {
        ContextoEntrega contexto = contexto(idInscripcion, idEntrega);
        exigirDelEstudiante(contexto, idEstudiante);

        if (!contexto.seccionIniciada()) {
            throw new IllegalStateException("El semestre todavía no comienza: aún no se reciben entregas");
        }
        if (!"EN_CURSO".equals(contexto.estadoInscripcion())) {
            throw new IllegalStateException("Tu inscripción ya no está en curso");
        }
        if (plazoVencido(contexto.fechaLimite(), LocalDate.now(reloj))) {
            throw new IllegalStateException("El plazo de esta entrega venció el "
                    + contexto.fechaLimite().format(FORMATO_FECHA) + ". Ya no se puede subir ni reemplazar el archivo");
        }
        if (contexto.nota() != null) {
            throw new IllegalStateException("Esta entrega ya fue calificada y no se puede reemplazar");
        }
        ArchivosPdf.validar(archivo);

        String nombreNuevo = almacen.guardar(archivo, ArchivosPdf.EXTENSION);
        String nombreAnterior = contexto.archivo();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        try {
            if (contexto.idRegistro() == null) {
                repositorio.insertarArchivo(idInscripcion, idEntrega, nombreNuevo, ahora);
            } else {
                repositorio.reemplazarArchivo(contexto.idRegistro(), nombreNuevo, ahora);
            }
        } catch (RuntimeException e) {
            almacen.eliminar(nombreNuevo);
            throw e;
        }
        if (nombreAnterior != null) {
            almacen.eliminar(nombreAnterior);
            return true;
        }
        return false;
    }

    /** El estudiante descarga su propio archivo. */
    @Transactional(readOnly = true)
    public DescargaEntrega descargar(long idEstudiante, long idInscripcion, long idEntrega) {
        ContextoEntrega contexto = contexto(idInscripcion, idEntrega);
        exigirDelEstudiante(contexto, idEstudiante);
        return descarga(contexto);
    }

    /** Resuelve el archivo en disco; 404 si no hay archivo o ya no está en la carpeta. */
    DescargaEntrega descarga(ContextoEntrega contexto) {
        if (!contexto.tieneArchivo()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay archivo en esta entrega");
        }
        try {
            Path ruta = almacen.resolver(contexto.archivo());
            if (Files.isRegularFile(ruta)) {
                return new DescargaEntrega(ruta, ArchivosPdf.nombreDeDescarga(contexto.nombreEntrega()));
            }
        } catch (IllegalArgumentException e) {
            // Un nombre guardado inválido se trata igual que un archivo que no existe.
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo ya no está disponible");
    }

    private ContextoEntrega contexto(long idInscripcion, long idEntrega) {
        ContextoEntrega contexto = repositorio.contexto(idInscripcion, idEntrega)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrega no encontrada"));
        if (!contexto.idSeccionInscripcion().equals(contexto.idSeccionEntrega()) || !contexto.entregaHabilitada()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta entrega no corresponde a tu inscripción");
        }
        return contexto;
    }

    private void exigirDelEstudiante(ContextoEntrega contexto, long idEstudiante) {
        if (!contexto.idEstudiante().equals(idEstudiante)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para esta entrega");
        }
    }
}
