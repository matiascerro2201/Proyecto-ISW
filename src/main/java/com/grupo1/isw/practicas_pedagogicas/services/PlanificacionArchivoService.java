package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PlanificacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HU-31/32: subida del PDF de la planificación (formato A), su almacenamiento y la extracción de su texto,
 * que es lo que lee el asistente. Subir otro PDF reemplaza al anterior (no hay historial de versiones).
 */
@Service
public class PlanificacionArchivoService {
    /** Debe coincidir con spring.servlet.multipart.max-file-size en application.properties. */
    static final long TAMANO_MAXIMO_BYTES = 20L * 1024 * 1024;
    /** Por debajo de esto se asume un PDF escaneado o vacío: el asistente no tendría qué leer. */
    static final int MIN_CARACTERES = 100;
    /** Tope del texto guardado para el asistente (la columna es MEDIUMTEXT, hasta 16 MB). */
    static final int MAX_CARACTERES = 200_000;
    private static final String EXTENSION_PDF = "pdf";
    private static final byte[] CABECERA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    /** Secciones del formato A: palabra clave (sin tildes, minúsculas) -> nombre para mostrar. */
    private static final Map<String, String> SECCIONES = Map.of(
            "objetivo", "Objetivo de la clase",
            "inicio", "Inicio",
            "desarrollo", "Desarrollo",
            "cierre", "Cierre",
            "errores", "Errores y dificultades",
            "referencias", "Referencias bibliográficas");

    private final PlanificacionRepository planificacionRepository;
    private final LineaPracticaRepository lineaPracticaRepository;
    private final PlanificacionService planificacionService;
    private final AlmacenArchivos almacenArchivos;
    private final ExtractorTextoPdf extractor;

    public PlanificacionArchivoService(PlanificacionRepository planificacionRepository,
                                       LineaPracticaRepository lineaPracticaRepository,
                                       PlanificacionService planificacionService,
                                       AlmacenArchivos almacenArchivos,
                                       ExtractorTextoPdf extractor) {
        this.planificacionRepository = planificacionRepository;
        this.lineaPracticaRepository = lineaPracticaRepository;
        this.planificacionService = planificacionService;
        this.almacenArchivos = almacenArchivos;
        this.extractor = extractor;
    }

    public record ResultadoSubida(Long idPlanificacion,
                                  String nombreOriginal,
                                  long tamano,
                                  LocalDateTime fecha,
                                  boolean reemplazo,
                                  List<String> advertencias) {
    }

    /** Archivo que se entrega al navegador y el nombre con que se le ofrece. */
    public record ArchivoPlanificacion(Path ruta, String nombreSugerido) {
    }

    /**
     * Sube el PDF. Con idPlanificacion reemplaza el de esa planificación (debe ser BORRADOR o RECHAZADA);
     * sin ella crea un borrador nuevo en la línea indicada. No cambia el estado ni avisa a nadie.
     *
     * @throws IllegalArgumentException archivo inválido (vacío, grande, no PDF, sin texto...) o falta la línea
     * @throws IllegalStateException    la planificación ya no es editable
     * @throws ResponseStatusException  404 si algo no existe, 403 si no es del estudiante
     */
    public ResultadoSubida subirPdf(Long idEstudiante, Long idLinea, Long idPlanificacion, MultipartFile archivo) {
        return subirPdf(idEstudiante, idLinea, idPlanificacion, archivo, null);
    }

    /**
     * Igual que la anterior; al crear un borrador nuevo guarda además la fecha para la que el estudiante pide
     * la revisión (opcional aquí: se vuelve a exigir al enviar). Con idPlanificacion se ignora.
     */
    @Transactional
    public ResultadoSubida subirPdf(Long idEstudiante, Long idLinea, Long idPlanificacion, MultipartFile archivo,
                                    LocalDate fechaRevision) {
        Planificacion planificacion = null;
        LineaPractica linea = null;
        if (idPlanificacion != null) {
            planificacion = planificacionService.obtenerPlanificacionDelEstudiante(idEstudiante, idPlanificacion);
            if (!PlanificacionService.esEditable(planificacion.getEstado())) {
                throw new IllegalStateException("Solo se puede cambiar el PDF de una planificación en borrador o rechazada");
            }
        } else {
            linea = lineaDelEstudiante(idEstudiante, idLinea);
            if (fechaRevision != null) {
                PlanificacionService.validarFechaRevision(fechaRevision, LocalDate.now());
            }
        }

        // Se valida y se lee el PDF antes de tocar disco o base de datos.
        byte[] bytes = validarYLeer(archivo);
        ExtractorTextoPdf.Resultado lectura = extractor.extraer(bytes);
        String texto = lectura.texto();
        if (texto.length() < MIN_CARACTERES) {
            throw new IllegalArgumentException(
                    "No se pudo leer texto del PDF. Si es una foto o un escaneo, exporta la planificación como PDF desde Word o Google Docs");
        }
        if (texto.length() > MAX_CARACTERES) {
            texto = texto.substring(0, MAX_CARACTERES);
        }

        String nombreNuevo = almacenArchivos.guardar(archivo, EXTENSION_PDF);
        String nombreAnterior = planificacion == null ? null : planificacion.getArchivoNombre();
        String original = nombreVisible(archivo.getOriginalFilename());
        LocalDateTime ahora = LocalDateTime.now();

        try {
            if (planificacion == null) {
                planificacion = Planificacion.borrador(linea, null);
                planificacion.setFechaRevisionSolicitada(fechaRevision);
            }
            planificacion.adjuntarArchivo(nombreNuevo, original, archivo.getSize(), texto, ahora);
            planificacion = planificacionRepository.saveAndFlush(planificacion);
        } catch (RuntimeException e) {
            // Si la base falla, no dejamos un archivo huérfano en disco.
            almacenArchivos.eliminar(nombreNuevo);
            throw e;
        }

        if (nombreAnterior != null) {
            almacenArchivos.eliminar(nombreAnterior);
        }
        return new ResultadoSubida(planificacion.getIdPlanificacion(), original, archivo.getSize(), ahora,
                nombreAnterior != null, advertenciasDe(texto));
    }

    /** El estudiante ve el PDF de sus planificaciones en cualquier estado. */
    @Transactional(readOnly = true)
    public ArchivoPlanificacion obtenerParaEstudiante(Long idEstudiante, Long idPlanificacion) {
        return aArchivo(planificacionService.obtenerPlanificacionDelEstudiante(idEstudiante, idPlanificacion));
    }

    /** El colaborador de la línea lo ve desde que se envía; un borrador no existe para él (404). */
    @Transactional(readOnly = true)
    public ArchivoPlanificacion obtenerParaColaborador(Long idPlanificacion, Long idColaborador) {
        return aArchivo(planificacionService.obtenerPlanificacionParaColaborador(idPlanificacion, idColaborador));
    }

    /** Secciones del formato A que no aparecen en el texto. Es solo un aviso: no impide guardar ni enviar. */
    static List<String> advertenciasDe(String texto) {
        String normalizado = normalizar(texto);
        List<String> faltantes = new ArrayList<>();
        SECCIONES.forEach((clave, nombre) -> {
            if (!normalizado.contains(clave)) {
                faltantes.add("No se encontró la sección \"" + nombre + "\". ¿Usaste el formato estándar?");
            }
        });
        faltantes.sort(String::compareTo);
        return faltantes;
    }

    private ArchivoPlanificacion aArchivo(Planificacion planificacion) {
        if (!planificacion.tieneArchivo()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta planificación no tiene un PDF");
        }
        try {
            Path ruta = almacenArchivos.resolver(planificacion.getArchivoNombre());
            if (Files.isRegularFile(ruta)) {
                return new ArchivoPlanificacion(ruta, "Planificacion-" + planificacion.getIdPlanificacion() + ".pdf");
            }
        } catch (IllegalArgumentException e) {
            // Un nombre guardado inválido se trata igual que un archivo que no existe.
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo ya no está disponible");
    }

    private LineaPractica lineaDelEstudiante(Long idEstudiante, Long idLinea) {
        if (idLinea == null) {
            throw new IllegalArgumentException("Debe seleccionar una línea de práctica");
        }
        LineaPractica linea = lineaPracticaRepository.findById(idLinea)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Línea de práctica no encontrada"));
        if (!linea.getInscripcion().getEstudiante().getIdUsuario().equals(idEstudiante)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para planificar en esta línea");
        }
        return linea;
    }

    private byte[] validarYLeer(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar un archivo");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo de 20 MB");
        }
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !nombre.toLowerCase(Locale.ROOT).endsWith("." + EXTENSION_PDF)) {
            throw new IllegalArgumentException("Solo se permiten archivos PDF");
        }
        try {
            byte[] bytes = archivo.getBytes();
            // La extensión se puede falsificar; la cabecera "%PDF-" es más difícil de fingir.
            if (bytes.length < CABECERA_PDF.length
                    || !Arrays.equals(Arrays.copyOf(bytes, CABECERA_PDF.length), CABECERA_PDF)) {
                throw new IllegalArgumentException("El archivo no es un PDF válido");
            }
            return bytes;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo subido", e);
        }
    }

    /** Solo el nombre, sin carpetas, y de largo acotado: se muestra al usuario, nunca se usa como ruta. */
    private static String nombreVisible(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "planificacion.pdf";
        }
        String soloNombre = nombre.substring(Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\')) + 1).trim();
        if (soloNombre.isEmpty()) {
            return "planificacion.pdf";
        }
        return soloNombre.length() > 255 ? soloNombre.substring(soloNombre.length() - 255) : soloNombre;
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
