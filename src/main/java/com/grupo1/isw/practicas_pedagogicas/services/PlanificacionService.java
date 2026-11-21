package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PlanificacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class PlanificacionService {
    private final PlanificacionRepository planificacionRepository;
    private final LineaPracticaRepository lineaPracticaRepository;
    private final NotificacionService notificacionService;

    public PlanificacionService(PlanificacionRepository planificacionRepository,
                                LineaPracticaRepository lineaPracticaRepository,
                                NotificacionService notificacionService) {
        this.planificacionRepository = planificacionRepository;
        this.lineaPracticaRepository = lineaPracticaRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public void evaluarPlanificacion(Long idPlanificacion, Long idColaborador, String retroalimentacion, EstadoPlanificacion nuevoEstado) {
        Planificacion planificacion = obtenerPlanificacion(idPlanificacion);
        ocultarSiEsBorrador(planificacion);

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

        LocalDateTime ahora = LocalDateTime.now();
        planificacion.setRetroalimentacion(retro);
        planificacion.setEstado(nuevoEstado);
        planificacion.setFechaRetro(ahora);
        // El estado y la retroalimentación se sobrescriben al reenviar; el historial conserva cada evaluación.
        planificacion.registrarEvaluacion(nuevoEstado, retro, ahora);

        planificacionRepository.save(planificacion);
        notificarAlEstudiante(planificacion, nuevoEstado);
    }

    public Planificacion obtenerPlanificacion(Long id) {
        return planificacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Planificación no encontrada"));
    }

    /**
     * Planificación vista por el colaborador de la línea. Un BORRADOR es trabajo privado del estudiante:
     * para el colaborador no existe (404, no 403, para no revelar que hay un borrador).
     */
    @Transactional(readOnly = true)
    public Planificacion obtenerPlanificacionParaColaborador(Long idPlanificacion, Long idColaborador) {
        Planificacion planificacion = obtenerPlanificacion(idPlanificacion);
        ocultarSiEsBorrador(planificacion);

        if (!planificacion.getLinea().getColaborador().getIdUsuario().equals(idColaborador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para ver esta planificación");
        }
        return planificacion;
    }

    private void ocultarSiEsBorrador(Planificacion planificacion) {
        if (planificacion.getEstado() == EstadoPlanificacion.BORRADOR) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Planificación no encontrada");
        }
    }

    /** Se puede editar (y conversar con el asistente) mientras es BORRADOR o RECHAZADA. */
    public static boolean esEditable(EstadoPlanificacion estado) {
        return estado == EstadoPlanificacion.BORRADOR || estado == EstadoPlanificacion.RECHAZADA;
    }

    @Transactional(readOnly = true)
    public List<Planificacion> obtenerPendientesDelColaborador(Long idColaborador) {
        return planificacionRepository
                .findByEstadoAndLinea_Colaborador_IdUsuarioOrderByFechaEnvioAsc(
                        EstadoPlanificacion.ENVIADA, idColaborador);
    }

    public record LineaOpcion(Long idLinea, String etiqueta) {
    }

    public record PlanificacionVista(Long idPlanificacion,
                                     String etiquetaLinea,
                                     String contenido,
                                     EstadoPlanificacion estado,
                                     String retroalimentacion,
                                     LocalDateTime fechaEnvio,
                                     LocalDateTime fechaRetro,
                                     String archivoOriginal,
                                     Long archivoTamano,
                                     LocalDateTime archivoFecha) {
        /** Planificación sin PDF (las anteriores a HU-31/32, que solo tienen texto). */
        public PlanificacionVista(Long idPlanificacion, String etiquetaLinea, String contenido,
                                  EstadoPlanificacion estado, String retroalimentacion,
                                  LocalDateTime fechaEnvio, LocalDateTime fechaRetro) {
            this(idPlanificacion, etiquetaLinea, contenido, estado, retroalimentacion,
                    fechaEnvio, fechaRetro, null, null, null);
        }

        public boolean tieneArchivo() {
            return archivoOriginal != null;
        }

        public boolean puedeReenviar() {
            return estado == EstadoPlanificacion.RECHAZADA;
        }

        public boolean esBorrador() {
            return estado == EstadoPlanificacion.BORRADOR;
        }

        public boolean puedeEditar() {
            return esEditable(estado);
        }
    }

    @Transactional(readOnly = true)
    public List<LineaOpcion> obtenerLineasDelEstudiante(Long idEstudiante) {
        return lineaPracticaRepository.findByInscripcion_Estudiante_IdUsuario(idEstudiante).stream()
                .map(l -> new LineaOpcion(l.getIdLinea(), etiquetaDe(l)))
                .toList();
    }

    /** Primero los borradores (el más reciente arriba) y después las enviadas, por fecha de envío. */
    @Transactional(readOnly = true)
    public List<PlanificacionVista> listarPlanificacionesDelEstudiante(Long idEstudiante) {
        List<Planificacion> todas = planificacionRepository
                .findByLinea_Inscripcion_Estudiante_IdUsuarioOrderByFechaEnvioDesc(idEstudiante);

        Stream<Planificacion> borradores = todas.stream()
                .filter(p -> p.getEstado() == EstadoPlanificacion.BORRADOR)
                .sorted(Comparator.comparing(Planificacion::getIdPlanificacion, Comparator.reverseOrder()));
        Stream<Planificacion> enviadas = todas.stream()
                .filter(p -> p.getEstado() != EstadoPlanificacion.BORRADOR);

        return Stream.concat(borradores, enviadas).map(this::aVista).toList();
    }

    @Transactional(readOnly = true)
    public PlanificacionVista obtenerVistaDelEstudiante(Long idEstudiante, Long idPlanificacion) {
        return aVista(obtenerPlanificacionDelEstudiante(idEstudiante, idPlanificacion));
    }

    /**
     * Envía un borrador al colaborador (BORRADOR -> ENVIADA). Exige que ya tenga su PDF subido
     * (la subida está en PlanificacionArchivoService). No depende del asistente.
     */
    @Transactional
    public void enviarBorrador(Long idEstudiante, Long idPlanificacion) {
        Planificacion planificacion = obtenerPlanificacionDelEstudiante(idEstudiante, idPlanificacion);

        if (planificacion.getEstado() != EstadoPlanificacion.BORRADOR) {
            throw new IllegalStateException("Solo se puede enviar una planificación en borrador");
        }
        exigirArchivo(planificacion);

        planificacion.setEstado(EstadoPlanificacion.ENVIADA);
        planificacion.setFechaEnvio(LocalDateTime.now());

        planificacionRepository.save(planificacion);
        notificarAlColaborador(planificacion, "envió");
    }

    /** Reenvía una planificación RECHAZADA. Exige un PDF (el mismo o uno nuevo que haya subido). */
    @Transactional
    public void reenviarPlanificacion(Long idEstudiante, Long idPlanificacion) {
        Planificacion planificacion = obtenerPlanificacionDelEstudiante(idEstudiante, idPlanificacion);

        if (planificacion.getEstado() != EstadoPlanificacion.RECHAZADA) {
            throw new IllegalStateException("Solo se puede editar y reenviar una planificación RECHAZADA");
        }
        exigirArchivo(planificacion);

        planificacion.setEstado(EstadoPlanificacion.ENVIADA);
        planificacion.setFechaEnvio(LocalDateTime.now());
        planificacion.setRetroalimentacion(null);
        planificacion.setFechaRetro(null);

        planificacionRepository.save(planificacion);
        notificarAlColaborador(planificacion, "reenvió");
    }

    private void exigirArchivo(Planificacion planificacion) {
        if (!planificacion.tieneArchivo()) {
            throw new IllegalArgumentException("Debes subir el PDF de la planificación antes de enviarla");
        }
    }

    /** HU-23: avisa al estudiante de la decisión sin copiar la retroalimentación; corre tras guardar. */
    private void notificarAlEstudiante(Planificacion planificacion, EstadoPlanificacion decision) {
        LineaPractica linea = planificacion.getLinea();
        String resultado = decision == EstadoPlanificacion.APROBADA
                ? "fue aprobada por tu profesor colaborador."
                : "fue rechazada por tu profesor colaborador; revisa la retroalimentación y reenvíala.";
        notificacionService.crear(linea.getInscripcion().getEstudiante().getIdUsuario(),
                NotificacionService.TIPO_PLANIFICACION,
                "Tu planificación de clase (" + etiquetaDe(linea) + ") " + resultado,
                "/estudiante/planificaciones");
    }

    /** HU-22: avisa al colaborador de la línea; corre en la transacción del guardado. */
    private void notificarAlColaborador(Planificacion planificacion, String accion) {
        LineaPractica linea = planificacion.getLinea();
        String mensaje = linea.getInscripcion().getEstudiante().getNombre() + " " + accion
                + " una planificación de clase (" + etiquetaDe(linea) + ") para tu revisión.";
        notificacionService.crear(linea.getColaborador().getIdUsuario(), NotificacionService.TIPO_PLANIFICACION,
                mensaje, "/planificaciones/" + planificacion.getIdPlanificacion() + "/evaluar");
    }

    /** La planificación de un estudiante: 404 si no existe y 403 si es de otro. Lo usa también el chat. */
    public Planificacion obtenerPlanificacionDelEstudiante(Long idEstudiante, Long idPlanificacion) {
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
                p.getFechaRetro(),
                p.getArchivoOriginal(),
                p.getArchivoTamano(),
                p.getArchivoFecha());
    }

    private String etiquetaDe(LineaPractica linea) {
        String asignatura = linea.getInscripcion().getSeccion().getAsignatura().getNombre();
        return asignatura + " (" + linea.getTipo() + ")";
    }
}
