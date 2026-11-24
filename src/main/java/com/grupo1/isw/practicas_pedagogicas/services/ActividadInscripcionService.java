package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.ActividadInscripcion;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.ActividadInscripcion.EntregaActividad;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.ActividadInscripcion.EvaluacionActividad;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.ActividadInscripcion.InformeActividad;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.ActividadInscripcion.LineaActividad;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.ActividadInscripcion.PlanificacionActividad;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.Etiquetas;
import com.grupo1.isw.practicas_pedagogicas.models.DocumentoEstudiante;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaDocumento;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.models.TipoEvaluador;
import com.grupo1.isw.practicas_pedagogicas.repositories.CoordinadorConsultaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * HU-37: arma el estado de las inscripciones que ve el coordinador. Trae todo en bloque (una consulta por tipo de
 * dato, no una por estudiante) y compara lo que se espera de cada línea con lo que ya se hizo.
 */
@Service
public class ActividadInscripcionService {
    private final CoordinadorConsultaRepository consulta;
    private final PautaEvaluacionRepository pautaEvaluacionRepository;

    public ActividadInscripcionService(CoordinadorConsultaRepository consulta,
                                       PautaEvaluacionRepository pautaEvaluacionRepository) {
        this.consulta = consulta;
        this.pautaEvaluacionRepository = pautaEvaluacionRepository;
    }

    /** Los datos ya cargados, para armar cada inscripción sin volver a la base. */
    private record Contexto(Map<Long, List<LineaPractica>> lineas,
                            Map<Long, List<EvaluacionAplicada>> evaluaciones,
                            Map<Long, List<Planificacion>> planificaciones,
                            Map<Long, List<DocumentoEstudiante>> documentos,
                            Map<Long, List<PautaEvaluacion>> pautasEvaluacion,
                            Map<Long, List<PautaDocumento>> pautasDocumento) {
    }

    @Transactional(readOnly = true)
    public List<ActividadInscripcion> armar(List<InscripcionAsignatura> inscripciones) {
        if (inscripciones.isEmpty()) {
            return List.of();
        }
        List<Long> idsInscripcion = inscripciones.stream().map(InscripcionAsignatura::getIdInscripcion).toList();

        Map<Long, List<LineaPractica>> lineas = consulta.findLineas(idsInscripcion).stream()
                .collect(Collectors.groupingBy(l -> l.getInscripcion().getIdInscripcion()));
        List<Long> idsLinea = lineas.values().stream().flatMap(List::stream).map(LineaPractica::getIdLinea).toList();

        Map<Long, List<EvaluacionAplicada>> evaluaciones = idsLinea.isEmpty() ? Map.of()
                : consulta.findEvaluaciones(idsLinea).stream()
                        .collect(Collectors.groupingBy(e -> e.getLinea().getIdLinea()));
        Map<Long, List<Planificacion>> planificaciones = idsLinea.isEmpty() ? Map.of()
                : consulta.findPlanificaciones(idsLinea).stream()
                        .collect(Collectors.groupingBy(p -> p.getLinea().getIdLinea()));
        Map<Long, List<DocumentoEstudiante>> documentos = consulta.findDocumentos(idsInscripcion).stream()
                .collect(Collectors.groupingBy(d -> d.getInscripcion().getIdInscripcion()));

        Set<Long> idsAsignatura = inscripciones.stream()
                .map(i -> i.getSeccion().getAsignatura().getIdAsignatura())
                .collect(Collectors.toSet());
        Map<Long, List<PautaEvaluacion>> pautasEvaluacion = cargar(idsAsignatura, pautaEvaluacionRepository::findByAsignatura);
        Map<Long, List<PautaDocumento>> pautasDocumento = cargar(idsAsignatura, consulta::findPautasDocumento);

        Contexto contexto = new Contexto(lineas, evaluaciones, planificaciones, documentos,
                pautasEvaluacion, pautasDocumento);
        return inscripciones.stream().map(i -> armarUna(i, contexto)).toList();
    }

    private static <T> Map<Long, List<T>> cargar(Collection<Long> idsAsignatura, Function<Long, List<T>> buscar) {
        Map<Long, List<T>> porAsignatura = new HashMap<>();
        for (Long id : idsAsignatura) {
            porAsignatura.put(id, buscar.apply(id));
        }
        return porAsignatura;
    }

    private ActividadInscripcion armarUna(InscripcionAsignatura inscripcion, Contexto contexto) {
        Long idInscripcion = inscripcion.getIdInscripcion();
        Long idAsignatura = inscripcion.getSeccion().getAsignatura().getIdAsignatura();
        List<LineaPractica> lineas = contexto.lineas().getOrDefault(idInscripcion, List.of());

        List<LineaActividad> lineasVista = lineas.stream()
                .map(l -> new LineaActividad(Etiquetas.tipoLinea(l.getTipo()),
                        l.getColaborador() == null ? null : l.getColaborador().getNombre(),
                        l.getTutor() == null ? null : l.getTutor().getNombre()))
                .toList();

        List<EvaluacionActividad> evaluaciones = new ArrayList<>();
        List<PlanificacionActividad> planificaciones = new ArrayList<>();
        for (LineaPractica linea : lineas) {
            evaluaciones.addAll(evaluacionesDeLinea(linea, contexto.pautasEvaluacion().getOrDefault(idAsignatura, List.of()),
                    contexto.evaluaciones().getOrDefault(linea.getIdLinea(), List.of())));
            for (Planificacion p : contexto.planificaciones().getOrDefault(linea.getIdLinea(), List.of())) {
                planificaciones.add(new PlanificacionActividad(p.getIdPlanificacion(), Etiquetas.tipoLinea(linea.getTipo()),
                        p.getEstado().name(), Etiquetas.estadoPlanificacion(p.getEstado()),
                        p.getFechaEnvio(), p.getFechaRetro()));
            }
        }

        var estudiante = inscripcion.getEstudiante();
        var seccion = inscripcion.getSeccion();
        return new ActividadInscripcion(
                idInscripcion,
                estudiante.getIdUsuario(),
                estudiante.getNombre(),
                estudiante.getEmail(),
                estudiante.getCarrera(),
                seccion.getAsignatura().getNombre(),
                seccion.getAnio(),
                seccion.getSemestre(),
                Etiquetas.estadoInscripcion(inscripcion.getEstado()),
                inscripcion.getCentro() == null ? "—" : inscripcion.getCentro().getNombre(),
                lineasVista,
                evaluaciones,
                planificaciones,
                entregas(contexto.pautasDocumento().getOrDefault(idAsignatura, List.of()),
                        contexto.documentos().getOrDefault(idInscripcion, List.of())),
                informe(inscripcion.getInformeFinal()));
    }

    /**
     * Lo que se espera de la línea: cada pauta habilitada de su tipo, evaluada por el colaborador y por el tutor,
     * si la línea los tiene (ambos son opcionales). Cada una aparece como realizada (con puntaje) o pendiente. Las evaluaciones ya
     * hechas que no calzan con lo esperado (por ejemplo, de una pauta deshabilitada después) se agregan igual:
     * el coordinador nunca deja de ver algo que existe.
     */
    private static List<EvaluacionActividad> evaluacionesDeLinea(LineaPractica linea, List<PautaEvaluacion> pautas,
                                                                 List<EvaluacionAplicada> hechas) {
        List<EvaluacionActividad> filas = new ArrayList<>();
        Set<Long> usadas = new HashSet<>();
        // Colaborador y tutor son opcionales: solo se espera la pauta de quien participa en la línea.
        List<TipoEvaluador> evaluadores = new ArrayList<>();
        if (linea.getColaborador() != null) {
            evaluadores.add(TipoEvaluador.COLABORADOR);
        }
        if (linea.getTutor() != null) {
            evaluadores.add(TipoEvaluador.TUTOR);
        }

        for (PautaEvaluacion pauta : pautas) {
            if (pauta.getTipoLinea() != linea.getTipo() || !pauta.isHabilitada()) {
                continue;
            }
            for (TipoEvaluador evaluador : evaluadores) {
                Optional<EvaluacionAplicada> hecha = hechas.stream()
                        .filter(e -> pauta.getIdPautaEvaluacion().equals(e.getPautaEvaluacion().getIdPautaEvaluacion())
                                && e.getTipoEvaluador() == evaluador)
                        .findFirst();
                hecha.ifPresent(e -> usadas.add(e.getIdEvaluacion()));
                filas.add(fila(linea, pauta, evaluador, hecha.orElse(null)));
            }
        }
        for (EvaluacionAplicada e : hechas) {
            if (!usadas.contains(e.getIdEvaluacion())) {
                filas.add(fila(linea, e.getPautaEvaluacion(), e.getTipoEvaluador(), e));
            }
        }
        return filas;
    }

    /** Una evaluación cuenta como realizada cuando tiene puntaje total (así lo muestra también la vista del colaborador). */
    private static EvaluacionActividad fila(LineaPractica linea, PautaEvaluacion pauta, TipoEvaluador evaluador,
                                            EvaluacionAplicada evaluacion) {
        boolean realizada = evaluacion != null && evaluacion.getPuntajeTotal() != null;
        return new EvaluacionActividad(
                Etiquetas.tipoLinea(linea.getTipo()),
                "Aplicación " + pauta.getNumeroAplicacion(),
                Etiquetas.tipoEvaluador(evaluador),
                realizada,
                realizada ? evaluacion.getPuntajeTotal() : null,
                realizada ? evaluacion.getFecha() : null,
                evaluacion == null ? null : evaluacion.getFechaLimite());
    }

    /** Una fila por cada entrega habilitada de la asignatura (con la última subida si hay varias), y las deshabilitadas que ya tienen una. */
    private static List<EntregaActividad> entregas(List<PautaDocumento> pautas, List<DocumentoEstudiante> documentos) {
        Map<Long, DocumentoEstudiante> ultimaPorPauta = new HashMap<>();
        for (DocumentoEstudiante d : documentos) {
            ultimaPorPauta.merge(d.getPautaDocumento().getIdPautaDocumento(), d,
                    (a, b) -> a.getFechaSubida().isAfter(b.getFechaSubida()) ? a : b);
        }
        List<EntregaActividad> filas = new ArrayList<>();
        for (PautaDocumento pauta : pautas) {
            DocumentoEstudiante entregado = ultimaPorPauta.get(pauta.getIdPautaDocumento());
            if (!pauta.isHabilitada() && entregado == null) {
                continue;
            }
            filas.add(new EntregaActividad(
                    Etiquetas.tipoDocumento(pauta.getTipo()) + " " + pauta.getNumero(),
                    entregado != null,
                    entregado == null ? null : entregado.getFechaSubida(),
                    entregado == null ? null : entregado.getPuntajeObtenido()));
        }
        return filas;
    }

    private static InformeActividad informe(InformeFinal informe) {
        if (informe == null) {
            return null;
        }
        String estado = informe.getFechaPublicacion() != null ? "Publicado"
                : informe.getFechaValidacion() != null ? "Validado, sin publicar"
                : "Generado, por revisar";
        return new InformeActividad(estado, informe.getCalificacionSugerida(), informe.getCalificacionFinal(),
                informe.getFechaPublicacion());
    }
}
