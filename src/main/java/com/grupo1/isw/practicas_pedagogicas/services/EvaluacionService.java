package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.DetalleEvaluacionRequest;
import com.grupo1.isw.practicas_pedagogicas.dto.FormularioEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.dto.FormularioEvaluacion.CriterioForm;
import com.grupo1.isw.practicas_pedagogicas.dto.FormularioEvaluacion.DimensionForm;
import com.grupo1.isw.practicas_pedagogicas.models.Criterio;
import com.grupo1.isw.practicas_pedagogicas.models.DetalleCriterio;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.TipoEvaluador;
import com.grupo1.isw.practicas_pedagogicas.repositories.DetalleCriterioRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionAplicadaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EvaluacionService {
    private final LineaConsultaService lineaConsulta;
    private final PautaEvaluacionRepository pautaRepository;
    private final EvaluacionAplicadaRepository evaluacionRepository;
    private final DetalleCriterioRepository detalleRepository;
    private final PautaCargador cargador;
    private final ValidadorEvaluacion validador;
    private final EvaluacionMapper mapper;

    public EvaluacionService(LineaConsultaService lineaConsulta,
                             PautaEvaluacionRepository pautaRepository,
                             EvaluacionAplicadaRepository evaluacionRepository,
                             DetalleCriterioRepository detalleRepository,
                             PautaCargador cargador,
                             ValidadorEvaluacion validador,
                             EvaluacionMapper mapper) {
        this.lineaConsulta = lineaConsulta;
        this.pautaRepository = pautaRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.detalleRepository = detalleRepository;
        this.cargador = cargador;
        this.validador = validador;
        this.mapper = mapper;
    }

    @Transactional
    public void guardar(Long idColaborador, Long idLinea, Long idPauta, List<DetalleEvaluacionRequest> detalles) {
        LineaPractica linea = lineaConsulta.obtenerLineaDelColaborador(idColaborador, idLinea);
        guardarEvaluacion(linea, TipoEvaluador.COLABORADOR, idPauta, detalles);
    }

    /** HU-03: el profesor supervisor (tutor) completa la pauta directamente en el sistema. */
    @Transactional
    public void guardarComoTutor(Long idTutor, Long idLinea, Long idPauta, List<DetalleEvaluacionRequest> detalles) {
        LineaPractica linea = lineaConsulta.obtenerLineaDelTutor(idTutor, idLinea);
        guardarEvaluacion(linea, TipoEvaluador.TUTOR, idPauta, detalles);
    }

    private void guardarEvaluacion(LineaPractica linea, TipoEvaluador evaluador, Long idPauta,
                                   List<DetalleEvaluacionRequest> detalles) {
        PautaEvaluacion pauta = pautaAplicable(linea, idPauta);

        List<Criterio> criterios = cargador.criteriosDePauta(pauta.getIdPautaEvaluacion());
        BigDecimal total = validador.validar(criterios, detalles);

        Map<Long, Criterio> criterioPorId = criterios.stream()
                .collect(Collectors.toMap(Criterio::getIdCriterio, Function.identity()));

        Optional<EvaluacionAplicada> existente = evaluacionExistente(linea.getIdLinea(), idPauta, evaluador);
        if (existente.isPresent()) {
            actualizarExistente(existente.get(), criterioPorId, detalles, total);
        } else {
            crearNueva(linea, pauta, evaluador, criterioPorId, detalles, total);
        }
    }

    private void crearNueva(LineaPractica linea, PautaEvaluacion pauta, TipoEvaluador evaluador,
                            Map<Long, Criterio> criterioPorId,
                            List<DetalleEvaluacionRequest> detalles, BigDecimal total) {
        EvaluacionAplicada evaluacion = evaluacionRepository.save(
                mapper.nuevaEvaluacion(linea, pauta, evaluador, total, LocalDateTime.now()));

        List<DetalleCriterio> nuevos = new ArrayList<>();
        for (DetalleEvaluacionRequest d : detalles) {
            nuevos.add(mapper.nuevoDetalle(evaluacion, criterioPorId.get(d.idCriterio()), d.puntaje(), d.comentario()));
        }
        detalleRepository.saveAll(nuevos);
    }

    private void actualizarExistente(EvaluacionAplicada evaluacion, Map<Long, Criterio> criterioPorId,
                                     List<DetalleEvaluacionRequest> detalles, BigDecimal total) {
        Map<Long, DetalleCriterio> actuales = detalleRepository.findByEvaluaciones(List.of(evaluacion.getIdEvaluacion())).stream()
                .collect(Collectors.toMap(d -> d.getCriterio().getIdCriterio(), Function.identity(), (a, b) -> a));

        List<DetalleCriterio> porGuardar = new ArrayList<>();
        for (DetalleEvaluacionRequest d : detalles) {
            DetalleCriterio detalle = actuales.get(d.idCriterio());
            if (detalle != null) {
                mapper.actualizarDetalle(detalle, d.puntaje(), d.comentario());
                porGuardar.add(detalle);
            } else {
                porGuardar.add(mapper.nuevoDetalle(evaluacion, criterioPorId.get(d.idCriterio()), d.puntaje(), d.comentario()));
            }
        }
        detalleRepository.saveAll(porGuardar);
        mapper.actualizarEvaluacion(evaluacion, total, LocalDateTime.now());
        evaluacionRepository.save(evaluacion);
    }

    @Transactional(readOnly = true)
    public FormularioEvaluacion formulario(Long idColaborador, Long idLinea, Long idPauta,
                                           Map<Long, String> puntajesEnviados,
                                           Map<Long, String> comentariosEnviados) {
        LineaPractica linea = lineaConsulta.obtenerLineaDelColaborador(idColaborador, idLinea);
        return armarFormulario(linea, TipoEvaluador.COLABORADOR, idPauta, puntajesEnviados, comentariosEnviados);
    }

    /** HU-03: formulario de la pauta para el profesor supervisor (tutor). */
    @Transactional(readOnly = true)
    public FormularioEvaluacion formularioComoTutor(Long idTutor, Long idLinea, Long idPauta,
                                                    Map<Long, String> puntajesEnviados,
                                                    Map<Long, String> comentariosEnviados) {
        LineaPractica linea = lineaConsulta.obtenerLineaDelTutor(idTutor, idLinea);
        return armarFormulario(linea, TipoEvaluador.TUTOR, idPauta, puntajesEnviados, comentariosEnviados);
    }

    private FormularioEvaluacion armarFormulario(LineaPractica linea, TipoEvaluador evaluador, Long idPauta,
                                                 Map<Long, String> puntajesEnviados,
                                                 Map<Long, String> comentariosEnviados) {
        PautaEvaluacion pauta = pautaAplicable(linea, idPauta);

        Optional<EvaluacionAplicada> existente = evaluacionExistente(linea.getIdLinea(), idPauta, evaluador);
        Map<Long, DetalleCriterio> guardados = existente
                .map(e -> detalleRepository.findByEvaluaciones(List.of(e.getIdEvaluacion())).stream()
                        .collect(Collectors.toMap(d -> d.getCriterio().getIdCriterio(), Function.identity(), (a, b) -> a)))
                .orElse(Map.of());

        List<DimensionForm> dimensiones = cargador.cargar(List.of(idPauta)).getOrDefault(idPauta, List.of()).stream()
                .map(dc -> new DimensionForm(
                        dc.dimension().getNombre(),
                        dc.criterios().stream()
                                .map(c -> criterioForm(c, guardados.get(c.getIdCriterio()), puntajesEnviados, comentariosEnviados))
                                .toList()))
                .toList();

        var inscripcion = linea.getInscripcion();
        return new FormularioEvaluacion(
                linea.getIdLinea(),
                pauta.getIdPautaEvaluacion(),
                inscripcion.getEstudiante().getNombre(),
                inscripcion.getSeccion().getAsignatura().getNombre(),
                String.valueOf(linea.getTipo()),
                pauta.getNumeroAplicacion(),
                pauta.getPonderacion(),
                existente.isPresent(),
                dimensiones);
    }

    private CriterioForm criterioForm(Criterio criterio, DetalleCriterio guardado,
                                      Map<Long, String> puntajesEnviados, Map<Long, String> comentariosEnviados) {
        Long id = criterio.getIdCriterio();
        String puntaje;
        String comentario;
        if (puntajesEnviados != null) {
            puntaje = puntajesEnviados.getOrDefault(id, "");
            comentario = comentariosEnviados == null ? "" : comentariosEnviados.getOrDefault(id, "");
        } else if (guardado != null) {
            puntaje = guardado.getPuntajeObtenido() == null ? "" : guardado.getPuntajeObtenido().toPlainString();
            comentario = guardado.getTranscripcionComentario() == null ? "" : guardado.getTranscripcionComentario();
        } else {
            puntaje = "";
            comentario = "";
        }
        return new CriterioForm(id, criterio.getDescripcion(), criterio.getPuntajeMax(), puntaje, comentario);
    }

    private PautaEvaluacion pautaAplicable(LineaPractica linea, Long idPauta) {
        return pautaRepository.findById(idPauta)
                .filter(p -> ReglasPauta.aplicaALinea(p, linea))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "La pauta no corresponde a esta línea"));
    }

    private Optional<EvaluacionAplicada> evaluacionExistente(Long idLinea, Long idPauta, TipoEvaluador evaluador) {
        return evaluacionRepository.findByLineaYEvaluador(idLinea, evaluador).stream()
                .filter(e -> Objects.equals(e.getPautaEvaluacion().getIdPautaEvaluacion(), idPauta))
                .findFirst();
    }
}
