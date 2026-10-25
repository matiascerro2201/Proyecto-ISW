package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.LineaEvaluacionesResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.LineaEvaluacionesResponse.PautaEstado;
import com.grupo1.isw.practicas_pedagogicas.dto.LineaResumenResponse;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionAplicadaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class LineaConsultaService {
    private final LineaPracticaRepository lineaRepository;
    private final EvaluacionAplicadaRepository evaluacionRepository;
    private final PautaEvaluacionRepository pautaRepository;

    public LineaConsultaService(LineaPracticaRepository lineaRepository,
                                EvaluacionAplicadaRepository evaluacionRepository,
                                PautaEvaluacionRepository pautaRepository) {
        this.lineaRepository = lineaRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.pautaRepository = pautaRepository;
    }

    @Transactional(readOnly = true)
    public List<LineaResumenResponse> listarLineasDelColaborador(Long idColaborador) {
        return lineaRepository.findByColaborador(idColaborador).stream()
                .map(this::aResumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public LineaPractica obtenerLineaDelColaborador(Long idColaborador, Long idLinea) {
        LineaPractica linea = lineaRepository.findByIdConContexto(idLinea)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "La línea de práctica no existe"));

        if (!Objects.equals(linea.getColaborador().getIdUsuario(), idColaborador)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No tienes permiso sobre esta línea de práctica");
        }
        return linea;
    }

    @Transactional(readOnly = true)
    public List<PautaEvaluacion> pautasAplicables(LineaPractica linea) {
        Long idAsignatura = linea.getInscripcion().getSeccion().getAsignatura().getIdAsignatura();
        return pautaRepository.findByAsignatura(idAsignatura).stream()
                .filter(p -> ReglasPauta.aplicaALinea(p, linea))
                .toList();
    }

    @Transactional(readOnly = true)
    public LineaEvaluacionesResponse detalleLinea(Long idColaborador, Long idLinea) {
        LineaPractica linea = obtenerLineaDelColaborador(idColaborador, idLinea);

        Map<Long, EvaluacionAplicada> hechas = evaluacionRepository.findDelColaboradorByLinea(idLinea).stream()
                .collect(Collectors.toMap(
                        e -> e.getPautaEvaluacion().getIdPautaEvaluacion(),
                        e -> e,
                        (primera, otra) -> primera,
                        LinkedHashMap::new));

        List<PautaEstado> pautas = pautasAplicables(linea).stream()
                .map(p -> {
                    EvaluacionAplicada e = hechas.get(p.getIdPautaEvaluacion());
                    return new PautaEstado(
                            p.getIdPautaEvaluacion(),
                            p.getNumeroAplicacion(),
                            p.getPonderacion(),
                            e == null ? null : e.getIdEvaluacion(),
                            e == null ? null : e.getPuntajeTotal(),
                            e == null ? null : e.getFecha());
                })
                .toList();

        LineaResumenResponse resumen = aResumen(linea);
        return new LineaEvaluacionesResponse(
                resumen.idLinea(), resumen.nombreEstudiante(), resumen.nombreAsignatura(),
                resumen.anio(), resumen.semestre(), resumen.tipoLinea(), pautas);
    }

    private LineaResumenResponse aResumen(LineaPractica linea) {
        var inscripcion = linea.getInscripcion();
        var seccion = inscripcion.getSeccion();
        return new LineaResumenResponse(
                linea.getIdLinea(),
                inscripcion.getEstudiante().getNombre(),
                seccion.getAsignatura().getNombre(),
                seccion.getAnio(),
                seccion.getSemestre(),
                String.valueOf(linea.getTipo()));
    }
}
