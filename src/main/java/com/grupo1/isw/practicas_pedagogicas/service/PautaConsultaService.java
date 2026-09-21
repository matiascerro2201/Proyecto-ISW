package com.grupo1.isw.practicas_pedagogicas.service;

import com.grupo1.isw.practicas_pedagogicas.dto.AsignaturaInscritaResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.PautaEvaluacionResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.PautaEvaluacionResponse.CriterioResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.PautaEvaluacionResponse.DimensionResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.PautasAsignaturaResponse;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Criterio;
import com.grupo1.isw.practicas_pedagogicas.models.Dimension;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.repository.CriterioRepository;
import com.grupo1.isw.practicas_pedagogicas.repository.DimensionRepository;
import com.grupo1.isw.practicas_pedagogicas.repository.InscripcionAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.repository.PautaEvaluacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HU-07: el estudiante consulta las pautas de evaluación de SUS asignaturas.
 * Servicio de solo lectura: ningún método escribe en la base de datos.
 */
@Service
public class PautaConsultaService {

    private final InscripcionAsignaturaRepository inscripcionRepository;
    private final PautaEvaluacionRepository pautaRepository;
    private final DimensionRepository dimensionRepository;
    private final CriterioRepository criterioRepository;

    public PautaConsultaService(InscripcionAsignaturaRepository inscripcionRepository,
                                PautaEvaluacionRepository pautaRepository,
                                DimensionRepository dimensionRepository,
                                CriterioRepository criterioRepository) {
        this.inscripcionRepository = inscripcionRepository;
        this.pautaRepository = pautaRepository;
        this.dimensionRepository = dimensionRepository;
        this.criterioRepository = criterioRepository;
    }

    /** Asignaturas del estudiante que ya tienen al menos una pauta registrada (punto de partida de la vista). */
    @Transactional(readOnly = true)
    public List<AsignaturaInscritaResponse> listarAsignaturasConPautas(Long idEstudiante) {
        return inscripcionRepository.findByEstudianteConPautas(idEstudiante)
                .stream()
                .map(AsignaturaInscritaResponse::desde)
                .toList();
    }

    /**
     * Devuelve las pautas de una asignatura solo si el estudiante está inscrito en ella.
     *
     * @throws ResponseStatusException 403 si no está inscrito (o la asignatura no existe: no se distingue,
     *                                 para no revelar qué asignaturas existen fuera de las suyas)
     */
    @Transactional(readOnly = true)
    public PautasAsignaturaResponse consultarPautasDeAsignatura(Long idAsignatura, Long idEstudiante) {

        Asignatura asignatura = inscripcionRepository.findAsignaturaInscrita(idEstudiante, idAsignatura)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No estás inscrito en esta asignatura"));

        List<PautaEvaluacion> pautas = pautaRepository.findByAsignatura(idAsignatura);
        if (pautas.isEmpty()) {
            return new PautasAsignaturaResponse(
                    asignatura.getIdAsignatura(), asignatura.getNombre(), List.of());
        }

        // 3 consultas en total (pautas, dimensiones, criterios); el árbol se arma en memoria
        List<Long> idsPauta = pautas.stream().map(PautaEvaluacion::getIdPautaEvaluacion).toList();
        List<Dimension> dimensiones = dimensionRepository.findByPautas(idsPauta);

        Map<Long, List<Dimension>> dimensionesPorPauta = dimensiones.stream()
                .collect(Collectors.groupingBy(d -> d.getPautaEvaluacion().getIdPautaEvaluacion()));

        Map<Long, List<Criterio>> criteriosPorDimension = Collections.emptyMap();
        if (!dimensiones.isEmpty()) {
            List<Long> idsDimension = dimensiones.stream().map(Dimension::getIdDimension).toList();
            criteriosPorDimension = criterioRepository.findByDimensiones(idsDimension).stream()
                    .collect(Collectors.groupingBy(c -> c.getDimension().getIdDimension()));
        }

        final Map<Long, List<Criterio>> criterios = criteriosPorDimension;
        List<PautaEvaluacionResponse> respuesta = pautas.stream()
                .map(p -> armarPauta(p, dimensionesPorPauta, criterios))
                .toList();

        return new PautasAsignaturaResponse(
                asignatura.getIdAsignatura(), asignatura.getNombre(), respuesta);
    }

    private PautaEvaluacionResponse armarPauta(PautaEvaluacion pauta,
                                               Map<Long, List<Dimension>> dimensionesPorPauta,
                                               Map<Long, List<Criterio>> criteriosPorDimension) {

        List<DimensionResponse> dimensiones = dimensionesPorPauta
                .getOrDefault(pauta.getIdPautaEvaluacion(), List.of())
                .stream()
                .map(d -> new DimensionResponse(
                        d.getIdDimension(),
                        d.getNombre(),
                        d.getOrden(),
                        criteriosPorDimension.getOrDefault(d.getIdDimension(), List.of())
                                .stream()
                                .map(c -> new CriterioResponse(
                                        c.getIdCriterio(),
                                        c.getDescripcion(),
                                        c.getPuntajeMax(),
                                        c.getOrden()))
                                .toList()))
                .toList();

        return new PautaEvaluacionResponse(
                pauta.getIdPautaEvaluacion(),
                String.valueOf(pauta.getTipoLinea()),   // sirve si tipoLinea es enum o String
                pauta.getNumeroAplicacion(),
                pauta.getPonderacion(),
                dimensiones);
    }
}
