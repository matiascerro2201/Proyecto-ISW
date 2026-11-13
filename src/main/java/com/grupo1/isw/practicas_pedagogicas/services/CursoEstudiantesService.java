package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.inicio.AccionEstudiante;
import com.grupo1.isw.practicas_pedagogicas.dto.inicio.CursoEstudiantes;
import com.grupo1.isw.practicas_pedagogicas.dto.inicio.EstudianteCurso;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoInscripcion;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeFinalRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PlanificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.ProfesorAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionAsignaturaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Estudiantes de un curso y las acciones de cada rol sobre ellos. */
@Service
public class CursoEstudiantesService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SeccionAsignaturaRepository seccionRepository;
    private final ProfesorAsignaturaRepository profesorRepository;
    private final LineaPracticaRepository lineaRepository;
    private final PlanificacionRepository planificacionRepository;
    private final InformeFinalRepository informeRepository;

    public CursoEstudiantesService(SeccionAsignaturaRepository seccionRepository,
                                   ProfesorAsignaturaRepository profesorRepository,
                                   LineaPracticaRepository lineaRepository,
                                   PlanificacionRepository planificacionRepository,
                                   InformeFinalRepository informeRepository) {
        this.seccionRepository = seccionRepository;
        this.profesorRepository = profesorRepository;
        this.lineaRepository = lineaRepository;
        this.planificacionRepository = planificacionRepository;
        this.informeRepository = informeRepository;
    }

    /** El curso al que pertenece un informe final, para volver a él. 404 si no existe y 403 si no es del profesor. */
    @Transactional(readOnly = true)
    public Long idSeccionDelInforme(Long idProfesor, Long idInforme) {
        InformeFinal informe = informeRepository.findByIdParaProfesor(idInforme)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El informe final no existe"));
        SeccionAsignatura seccion = informe.getInscripcion().getSeccion();
        if (seccion.getProfesor() == null || !Objects.equals(seccion.getProfesor().getIdUsuario(), idProfesor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No dictas este curso");
        }
        return seccion.getIdSeccion();
    }

    @Transactional(readOnly = true)
    public CursoEstudiantes listar(Long idUsuario, String rol, Long idSeccion) {
        SeccionAsignatura seccion = seccionRepository.findById(idSeccion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El curso no existe"));

        List<EstudianteCurso> estudiantes = switch (rol) {
            case "PROFESOR_ASIGNATURA" -> delProfesorDeAsignatura(idUsuario, seccion);
            case "COLABORADOR" -> delProfesorDeLinea(idUsuario, seccion, true);
            case "TUTOR" -> delProfesorDeLinea(idUsuario, seccion, false);
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Tu rol no consulta los estudiantes de un curso");
        };

        return new CursoEstudiantes(seccion.getAsignatura().getNombre(),
                "Semestre " + seccion.getSemestre() + " de " + seccion.getAnio(), estudiantes);
    }

    private List<EstudianteCurso> delProfesorDeAsignatura(Long idProfesor, SeccionAsignatura seccion) {
        if (seccion.getProfesor() == null || !Objects.equals(seccion.getProfesor().getIdUsuario(), idProfesor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No dictas este curso");
        }
        return profesorRepository.findInscripcionesDelProfesor(idProfesor).stream()
                .filter(ins -> Objects.equals(ins.getSeccion().getIdSeccion(), seccion.getIdSeccion()))
                .filter(ins -> ins.getEstado() != EstadoInscripcion.RETIRADO)
                .map(this::conInforme)
                .toList();
    }

    private EstudianteCurso conInforme(InscripcionAsignatura inscripcion) {
        Long id = inscripcion.getIdInscripcion();
        String nombre = inscripcion.getEstudiante().getNombre();
        InformeFinal informe = inscripcion.getInformeFinal();

        if (informe == null) {
            return new EstudianteCurso(id, nombre, "Sin informe", "pendiente",
                    List.of(new AccionEstudiante("Ir a generar el informe final",
                            "/profesor/informes#curso-" + inscripcion.getSeccion().getIdSeccion())));
        }

        String base = "/profesor/informes/" + informe.getIdInforme();
        if (informe.getFechaPublicacion() != null) {
            return new EstudianteCurso(id, nombre, "Publicado", "aprobada", List.of(
                    new AccionEstudiante("Ver el informe final", base),
                    new AccionEstudiante("Ver la revisión del informe", base + "#revisar")));
        }
        boolean validado = informe.getFechaValidacion() != null;
        return new EstudianteCurso(id, nombre, validado ? "Validado" : "Informe generado",
                validado ? "aprobada" : "borrador", List.of(
                        new AccionEstudiante("Ver el informe final", base),
                        new AccionEstudiante("Revisar y publicar", base + "#revisar")));
    }

    private List<EstudianteCurso> delProfesorDeLinea(Long idProfesor, SeccionAsignatura seccion,
                                                     boolean esColaborador) {
        List<LineaPractica> delCurso = (esColaborador
                ? lineaRepository.findByColaborador(idProfesor)
                : lineaRepository.findByTutor(idProfesor))
                .stream()
                .filter(linea -> Objects.equals(linea.getInscripcion().getSeccion().getIdSeccion(),
                        seccion.getIdSeccion()))
                .toList();
        if (delCurso.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes estudiantes en este curso");
        }

        // una fila por estudiante aunque tenga dos líneas
        Map<Long, List<LineaPractica>> porInscripcion = delCurso.stream()
                .filter(linea -> linea.getInscripcion().getEstado() != EstadoInscripcion.RETIRADO)
                .collect(Collectors.groupingBy(linea -> linea.getInscripcion().getIdInscripcion(),
                        LinkedHashMap::new, Collectors.toList()));

        Map<Long, List<Planificacion>> porEvaluar = new HashMap<>();
        if (esColaborador) {
            for (Planificacion planificacion
                    : planificacionRepository.findByEstadoAndLinea_Colaborador_IdUsuarioOrderByFechaEnvioAsc(
                            EstadoPlanificacion.ENVIADA, idProfesor)) {
                porEvaluar.computeIfAbsent(planificacion.getLinea().getInscripcion().getIdInscripcion(),
                        id -> new ArrayList<>()).add(planificacion);
            }
        }

        String base = esColaborador ? "/colaborador/lineas/" : "/tutor/lineas/";
        List<EstudianteCurso> estudiantes = new ArrayList<>();
        for (List<LineaPractica> lineas : porInscripcion.values()) {
            InscripcionAsignatura inscripcion = lineas.get(0).getInscripcion();
            List<Planificacion> pendientes = porEvaluar.getOrDefault(inscripcion.getIdInscripcion(), List.of());

            List<AccionEstudiante> acciones = new ArrayList<>();
            for (Planificacion planificacion : pendientes) {
                acciones.add(new AccionEstudiante(
                        planificacion.getFechaEnvio() == null
                                ? "Evaluar la planificación"
                                : "Evaluar planificación enviada el " + planificacion.getFechaEnvio().format(FECHA),
                        "/planificaciones/" + planificacion.getIdPlanificacion() + "/evaluar"));
            }
            for (LineaPractica linea : lineas) {
                acciones.add(new AccionEstudiante("Pautas de evaluación (" + tipoDeLinea(linea) + ")",
                        base + linea.getIdLinea()));
            }

            estudiantes.add(new EstudianteCurso(inscripcion.getIdInscripcion(),
                    inscripcion.getEstudiante().getNombre(),
                    pendientes.isEmpty() ? null : pendientes.size() + " por evaluar",
                    pendientes.isEmpty() ? null : "pendiente",
                    acciones));
        }
        return estudiantes;
    }

    private static String tipoDeLinea(LineaPractica linea) {
        String tipo = linea.getTipo().name();
        return switch (tipo) {
            case "UNICA" -> "Línea única";
            case "ESPECIALIDAD" -> "Especialidad";
            case "ORIENTACION" -> "Orientación";
            default -> {
                String texto = tipo.replace('_', ' ').toLowerCase(Locale.ROOT);
                yield Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
            }
        };
    }
}
