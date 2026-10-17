package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.AsignaturaInscritaResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.PautaEvaluacionResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.PautaEvaluacionResponse.CriterioResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.PautaEvaluacionResponse.DimensionResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.PautasAsignaturaResponse;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Criterio;
import com.grupo1.isw.practicas_pedagogicas.models.Dimension;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.repositories.CriterioRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.DimensionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.InscripcionAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public List<AsignaturaInscritaResponse> listarAsignaturasConPautas(Long idEstudiante) {
        return inscripcionRepository.findByEstudianteConPautas(idEstudiante)
                .stream()
                .map(AsignaturaInscritaResponse::desde)
                .toList();
    }

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
                String.valueOf(pauta.getTipoLinea()),
                pauta.getNumeroAplicacion(),
                pauta.getPonderacion(),
                dimensiones);
    }
}
