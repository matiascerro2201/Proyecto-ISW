package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.PlazoRevisionResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.inicio.CursoInicio;
import com.grupo1.isw.practicas_pedagogicas.dto.inicio.InicioResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.inicio.PendienteInicio;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoInscripcion;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaDocumento;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.repositories.DocumentoEstudianteRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionAplicadaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.InscripcionAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaDocumentoRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PlanificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Cursos y pendientes del inicio según el rol. */
@Service
public class InicioService {
    private final InscripcionAsignaturaRepository inscripcionRepository;
    private final LineaPracticaRepository lineaRepository;
    private final PlanificacionRepository planificacionRepository;
    private final PautaDocumentoRepository pautaDocumentoRepository;
    private final DocumentoEstudianteRepository documentoRepository;
    private final PautaEvaluacionRepository pautaEvaluacionRepository;
    private final EvaluacionAplicadaRepository evaluacionRepository;
    private final PlazoRevisionService plazoRevisionService;

    public InicioService(InscripcionAsignaturaRepository inscripcionRepository,
                         LineaPracticaRepository lineaRepository,
                         PlanificacionRepository planificacionRepository,
                         PautaDocumentoRepository pautaDocumentoRepository,
                         DocumentoEstudianteRepository documentoRepository,
                         PautaEvaluacionRepository pautaEvaluacionRepository,
                         EvaluacionAplicadaRepository evaluacionRepository,
                         PlazoRevisionService plazoRevisionService) {
        this.inscripcionRepository = inscripcionRepository;
        this.lineaRepository = lineaRepository;
        this.planificacionRepository = planificacionRepository;
        this.pautaDocumentoRepository = pautaDocumentoRepository;
        this.documentoRepository = documentoRepository;
        this.pautaEvaluacionRepository = pautaEvaluacionRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.plazoRevisionService = plazoRevisionService;
    }

    @Transactional(readOnly = true)
    public InicioResponse armar(Long idUsuario, String rol) {
        InicioResponse respuesta = switch (rol) {
            case "ESTUDIANTE" -> deEstudiante(idUsuario);
            case "COLABORADOR" -> deProfesorDeLinea(idUsuario, true);
            case "TUTOR" -> deProfesorDeLinea(idUsuario, false);
            case "PROFESOR_ASIGNATURA" -> deProfesorDeAsignatura(idUsuario);
            default -> InicioResponse.vacio();
        };
        return new InicioResponse(respuesta.cursos(), ordenar(respuesta.pendientes()));
    }

    private InicioResponse deEstudiante(Long idEstudiante) {
        List<InscripcionAsignatura> inscripciones = inscripcionRepository.findByEstudianteConAsignatura(idEstudiante)
                .stream()
                .filter(ins -> ins.getEstado() != EstadoInscripcion.RETIRADO)
                .toList();

        Set<Long> conPautas = inscripcionRepository.findByEstudianteConPautas(idEstudiante).stream()
                .map(ins -> ins.getSeccion().getAsignatura().getIdAsignatura())
                .collect(Collectors.toSet());

        List<CursoInicio> cursos = inscripciones.stream()
                .map(ins -> {
                    Asignatura asignatura = ins.getSeccion().getAsignatura();
                    String enlace = conPautas.contains(asignatura.getIdAsignatura())
                            ? "/mis-pautas/asignatura/" + asignatura.getIdAsignatura()
                            : null;
                    return new CursoInicio(asignatura.getNombre(), sigla(asignatura.getNombre()),
                            periodo(ins.getSeccion()), null, enlace, false);
                })
                .toList();

        List<PendienteInicio> pendientes = new ArrayList<>();

        for (Planificacion planificacion
                : planificacionRepository.findByLinea_Inscripcion_Estudiante_IdUsuarioOrderByFechaEnvioDesc(idEstudiante)) {
            String asignatura = nombreAsignatura(planificacion.getLinea());
            if (planificacion.getEstado() == EstadoPlanificacion.BORRADOR) {
                pendientes.add(PendienteInicio.porHacer("Terminar y enviar tu planificación", asignatura,
                        "/estudiante/planificaciones", "Borrador", "borrador"));
            } else if (planificacion.getEstado() == EstadoPlanificacion.RECHAZADA) {
                pendientes.add(PendienteInicio.porHacer("Corregir y reenviar tu planificación", asignatura,
                        "/estudiante/planificaciones", "Con observaciones", "rechazada"));
            }
        }

        for (InscripcionAsignatura inscripcion : inscripciones) {
            Asignatura asignatura = inscripcion.getSeccion().getAsignatura();
            Set<Long> entregadas = documentoRepository.findByInscripcion_IdInscripcion(inscripcion.getIdInscripcion())
                    .stream()
                    .map(documento -> documento.getPautaDocumento().getIdPautaDocumento())
                    .collect(Collectors.toSet());
            for (PautaDocumento pauta
                    : pautaDocumentoRepository.findByAsignatura_IdAsignaturaOrderByTipoAscNumeroAsc(asignatura.getIdAsignatura())) {
                if (!entregadas.contains(pauta.getIdPautaDocumento())) {
                    pendientes.add(PendienteInicio.porHacer("Entregar " + nombreDocumento(pauta), asignatura.getNombre(),
                            "/estudiante/documentos", "Por entregar", "pendiente"));
                }
            }
        }

        return new InicioResponse(cursos, pendientes);
    }

    private InicioResponse deProfesorDeLinea(Long idProfesor, boolean esColaborador) {
        List<LineaPractica> lineas = (esColaborador
                ? lineaRepository.findByColaborador(idProfesor)
                : lineaRepository.findByTutor(idProfesor))
                .stream()
                .filter(linea -> linea.getInscripcion().getEstado() != EstadoInscripcion.RETIRADO)
                .toList();
        String enlaceLineas = esColaborador ? "/colaborador/lineas" : "/tutor/lineas";

        Map<Long, List<LineaPractica>> porSeccion = lineas.stream().collect(Collectors.groupingBy(
                linea -> linea.getInscripcion().getSeccion().getIdSeccion(), LinkedHashMap::new, Collectors.toList()));
        List<CursoInicio> cursos = porSeccion.values().stream()
                .map(grupo -> {
                    SeccionAsignatura seccion = grupo.get(0).getInscripcion().getSeccion();
                    long estudiantes = grupo.stream()
                            .map(linea -> linea.getInscripcion().getIdInscripcion())
                            .distinct()
                            .count();
                    String nombre = seccion.getAsignatura().getNombre();
                    return new CursoInicio(nombre, sigla(nombre), periodo(seccion),
                            estudiantes == 1 ? "1 estudiante" : estudiantes + " estudiantes",
                            enlaceCurso(seccion.getIdSeccion()), true);
                })
                .toList();

        List<PendienteInicio> pendientes = new ArrayList<>();

        if (esColaborador) {
            for (Planificacion planificacion
                    : planificacionRepository.findByEstadoAndLinea_Colaborador_IdUsuarioOrderByFechaEnvioAsc(
                            EstadoPlanificacion.ENVIADA, idProfesor)) {
                String estudiante = planificacion.getLinea().getInscripcion().getEstudiante().getNombre();
                pendientes.add(PendienteInicio.porHacer("Evaluar la planificación de " + estudiante,
                        nombreAsignatura(planificacion.getLinea()),
                        "/planificaciones/" + planificacion.getIdPlanificacion() + "/evaluar",
                        "Por evaluar", "pendiente"));
            }
        }

        Map<Long, List<PautaEvaluacion>> pautasPorAsignatura = new HashMap<>();
        for (LineaPractica linea : lineas) {
            Long idAsignatura = linea.getInscripcion().getSeccion().getAsignatura().getIdAsignatura();
            List<PautaEvaluacion> pautas = pautasPorAsignatura.computeIfAbsent(
                    idAsignatura, pautaEvaluacionRepository::findByAsignatura);
            List<EvaluacionAplicada> hechas = esColaborador
                    ? evaluacionRepository.findDelColaboradorByLinea(linea.getIdLinea())
                    : evaluacionRepository.findDelTutorByLinea(linea.getIdLinea());
            Set<Long> evaluadas = hechas.stream()
                    .map(evaluacion -> evaluacion.getPautaEvaluacion().getIdPautaEvaluacion())
                    .collect(Collectors.toSet());

            String estudiante = linea.getInscripcion().getEstudiante().getNombre();
            for (PautaEvaluacion pauta : pautas) {
                if (pauta.getTipoLinea() == linea.getTipo() && !evaluadas.contains(pauta.getIdPautaEvaluacion())) {
                    pendientes.add(PendienteInicio.porHacer(
                            "Evaluar la pauta " + pauta.getNumeroAplicacion() + " de " + estudiante,
                            nombreAsignatura(linea), enlaceLineas + "/" + linea.getIdLinea(),
                            "Por evaluar", "pendiente"));
                }
            }
        }

        return new InicioResponse(cursos, pendientes);
    }

    private InicioResponse deProfesorDeAsignatura(Long idProfesor) {
        // el plazo de revisión es la única fecha límite que existe hoy
        List<PlazoRevisionResponse> plazos = plazoRevisionService.listarPlazos(idProfesor);

        List<CursoInicio> cursos = plazos.stream()
                .map(plazo -> new CursoInicio(plazo.nombreAsignatura(), sigla(plazo.nombreAsignatura()),
                        "Semestre " + plazo.semestre() + " de " + plazo.anio(),
                        plazo.totalEstudiantes() == 1 ? "1 estudiante" : plazo.totalEstudiantes() + " estudiantes",
                        enlaceCurso(plazo.idSeccion()), true))
                .toList();

        List<PendienteInicio> pendientes = plazos.stream()
                .filter(plazo -> plazo.informesPendientes() > 0)
                .map(plazo -> new PendienteInicio(
                        "Revisar informes finales de " + plazo.nombreAsignatura(),
                        plazo.informesPendientes() + " de " + plazo.totalEstudiantes() + " por revisar",
                        plazo.fechaLimite(), "/profesor/informes",
                        plazo.textoEstado(), plazo.claseEstado(),
                        plazo.estado() == PlazoRevisionResponse.Estado.VENCIDO))
                .toList();

        return new InicioResponse(cursos, pendientes);
    }

    private static List<PendienteInicio> ordenar(List<PendienteInicio> pendientes) {
        List<PendienteInicio> ordenadas = new ArrayList<>(pendientes);
        ordenadas.sort(Comparator.comparing(PendienteInicio::fecha,
                Comparator.nullsLast(Comparator.<LocalDate>naturalOrder())));
        return ordenadas;
    }

    private static String enlaceCurso(Long idSeccion) {
        return "/cursos/" + idSeccion + "/estudiantes";
    }

    public static String etiquetaRol(String rol) {
        return switch (rol) {
            case "ESTUDIANTE" -> "Estudiante";
            case "COLABORADOR" -> "Profesor colaborador";
            case "TUTOR" -> "Profesor supervisor";
            case "PROFESOR_ASIGNATURA" -> "Profesor de asignatura";
            default -> capitalizar(rol);
        };
    }

    public static String sigla(String nombre) {
        StringBuilder sigla = new StringBuilder();
        for (String palabra : nombre.trim().split("\\s+")) {
            if (palabra.length() > 2 && sigla.length() < 3) {
                sigla.append(Character.toUpperCase(palabra.charAt(0)));
            }
        }
        if (sigla.length() == 0) {
            return nombre.isBlank() ? "?" : nombre.trim().substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return sigla.toString();
    }

    private static String periodo(SeccionAsignatura seccion) {
        return "Semestre " + seccion.getSemestre() + " de " + seccion.getAnio();
    }

    private static String nombreAsignatura(LineaPractica linea) {
        return linea.getInscripcion().getSeccion().getAsignatura().getNombre();
    }

    private static String nombreDocumento(PautaDocumento pauta) {
        return capitalizar(pauta.getTipo().name()) + " " + pauta.getNumero();
    }

    private static String capitalizar(String constante) {
        String texto = constante.replace('_', ' ').toLowerCase(Locale.ROOT);
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
