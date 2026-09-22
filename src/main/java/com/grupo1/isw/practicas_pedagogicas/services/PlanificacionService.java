package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.models.*;
import com.grupo1.isw.practicas_pedagogicas.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PlanificacionService {

    private final PlanificacionRepository planificacionRepository;
    private final LineaPracticaRepository lineaPracticaRepository;

    public PlanificacionService(PlanificacionRepository planificacionRepository, LineaPracticaRepository lineaPracticaRepository) {
        this.planificacionRepository = planificacionRepository;
        this.lineaPracticaRepository = lineaPracticaRepository;
    }

    @Transactional
    public void evaluarPlanificacion(Long idPlanificacion, Long idColaborador, String retroalimentacion, EstadoPlanificacion nuevoEstado) {
        Planificacion planificacion = obtenerPlanificacion(idPlanificacion);

        if (!planificacion.getLinea().getColaborador().getIdUsuario().equals(idColaborador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para evaluar esta planificación");
        }

        if (nuevoEstado != EstadoPlanificacion.APROBADA && nuevoEstado != EstadoPlanificacion.RECHAZADA) {
            throw new IllegalArgumentException("La decisión debe ser APROBADA o RECHAZADA");
        }

        if (planificacion.getEstado() != EstadoPlanificacion.ENVIADA) {
            throw new IllegalStateException("Solo se pueden evaluar planificaciones en estado ENVIADA");
        }

        String retro = (retroalimentacion == null || retroalimentacion.isBlank()) ? null : retroalimentacion.trim();

        if (nuevoEstado == EstadoPlanificacion.RECHAZADA && retro == null) {
            throw new IllegalArgumentException("Debe ingresar retroalimentación para rechazar la planificación");
        }

        planificacion.setRetroalimentacion(retro);
        planificacion.setEstado(nuevoEstado);
        planificacion.setFechaRetro(LocalDateTime.now());

        planificacionRepository.save(planificacion);
    }

    public Planificacion obtenerPlanificacion(Long id) {
        return planificacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Planificación no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<Planificacion> obtenerPendientesDelColaborador(Long idColaborador) {
        return planificacionRepository
                .findByEstadoAndLinea_Colaborador_IdUsuarioOrderByFechaEnvioAsc(
                        EstadoPlanificacion.ENVIADA, idColaborador);
    }

    public record LineaOpcion(Long idLinea, String etiqueta) {
    }

    public record PlanificacionVista(Long idPlanificacion, String etiquetaLinea, String contenido, EstadoPlanificacion estado, String retroalimentacion,
                                     LocalDateTime fechaEnvio, LocalDateTime fechaRetro) {

        public boolean puedeReenviar() {
            return estado == EstadoPlanificacion.RECHAZADA;
        }
    }

    @Transactional(readOnly = true)
    public List<LineaOpcion> obtenerLineasDelEstudiante(Long idEstudiante) {
        return lineaPracticaRepository.findByInscripcion_Estudiante_IdUsuario(idEstudiante).stream().map(l -> new LineaOpcion(l.getIdLinea(), etiquetaDe(l)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlanificacionVista> listarPlanificacionesDelEstudiante(Long idEstudiante) {
        return planificacionRepository
                .findByLinea_Inscripcion_Estudiante_IdUsuarioOrderByFechaEnvioDesc(idEstudiante).stream()
                .map(this::aVista)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanificacionVista obtenerVistaDelEstudiante(Long idEstudiante, Long idPlanificacion) {
        return aVista(obtenerPlanificacionDelEstudiante(idEstudiante, idPlanificacion));
    }

    /** Crea una planificación nueva (estado inicial ENVIADA) en una línea que pertenece al estudiante. */
    @Transactional
    public void crearPlanificacion(Long idEstudiante, Long idLinea, String contenido) {
        if (idLinea == null) {
            throw new IllegalArgumentException("Debe seleccionar una línea de práctica");
        }

        LineaPractica linea = lineaPracticaRepository.findById(idLinea)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Línea de práctica no encontrada"));

        if (!lineaPerteneceAlEstudiante(linea, idEstudiante)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para planificar en esta línea");
        }

        String texto = limpiar(contenido);
        if (texto == null) {
            throw new IllegalArgumentException("El contenido de la planificación no puede estar vacío");
        }

        planificacionRepository.save(new Planificacion(linea, texto, LocalDateTime.now()));
    }

    /**
     * Reenvía una planificación RECHAZADA. Sobrescribe la misma fila (sin historial de versiones,
     * decisión del MER): vuelve a ENVIADA y limpia la retroalimentación anterior, que es lo que
     * asume evaluarPlanificacion() al revisar una planificación ENVIADA.
     */
    @Transactional
    public void reenviarPlanificacion(Long idEstudiante, Long idPlanificacion, String contenido) {
        Planificacion planificacion = obtenerPlanificacionDelEstudiante(idEstudiante, idPlanificacion);

        if (planificacion.getEstado() != EstadoPlanificacion.RECHAZADA) {
            throw new IllegalStateException("Solo se puede editar y reenviar una planificación RECHAZADA");
        }

        String texto = limpiar(contenido);
        if (texto == null) {
            throw new IllegalArgumentException("El contenido de la planificación no puede estar vacío");
        }

        planificacion.setContenido(texto);
        planificacion.setEstado(EstadoPlanificacion.ENVIADA);
        planificacion.setFechaEnvio(LocalDateTime.now());
        planificacion.setRetroalimentacion(null);
        planificacion.setFechaRetro(null);

        planificacionRepository.save(planificacion);
    }

    // ---- helpers privados (HU-19) ----

    /** Devuelve la planificación solo si su línea pertenece al estudiante (404 si no existe, 403 si es ajena). */
    private Planificacion obtenerPlanificacionDelEstudiante(Long idEstudiante, Long idPlanificacion) {
        Planificacion planificacion = obtenerPlanificacion(idPlanificacion);
        if (!lineaPerteneceAlEstudiante(planificacion.getLinea(), idEstudiante)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para acceder a esta planificación");
        }
        return planificacion;
    }

    private boolean lineaPerteneceAlEstudiante(LineaPractica linea, Long idEstudiante) {
        return linea.getInscripcion().getEstudiante().getIdUsuario().equals(idEstudiante);
    }

    private PlanificacionVista aVista(Planificacion p) {
        return new PlanificacionVista(
                p.getIdPlanificacion(),
                etiquetaDe(p.getLinea()),
                p.getContenido(),
                p.getEstado(),
                p.getRetroalimentacion(),
                p.getFechaEnvio(),
                p.getFechaRetro());
    }

    private String etiquetaDe(LineaPractica linea) {
        String asignatura = linea.getInscripcion().getSeccion().getAsignatura().getNombre();
        return asignatura + " (" + linea.getTipo() + ")";
    }

    /** Mismo criterio que en evaluarPlanificacion: null si está vacío o en blanco, si no el texto sin espacios sobrantes. */
    private String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
