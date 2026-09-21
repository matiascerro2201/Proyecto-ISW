package com.grupo1.isw.practicas_pedagogicas.service;

import com.grupo1.isw.practicas_pedagogicas.dto.DetalleEvaluacionRequest;
import com.grupo1.isw.practicas_pedagogicas.dto.FormularioEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.dto.FormularioEvaluacion.CriterioForm;
import com.grupo1.isw.practicas_pedagogicas.dto.FormularioEvaluacion.DimensionForm;
import com.grupo1.isw.practicas_pedagogicas.models.Criterio;
import com.grupo1.isw.practicas_pedagogicas.models.DetalleCriterio;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.repository.DetalleCriterioRepository;
import com.grupo1.isw.practicas_pedagogicas.repository.EvaluacionAplicadaRepository;
import com.grupo1.isw.practicas_pedagogicas.repository.PautaEvaluacionRepository;
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

/**
 * HU-02: el colaborador llena la pauta de evaluación (evaluacion_aplicada + detalle_criterio).
 *
 * Solo hay un formulario por (línea, pauta): guardar() crea la evaluación la primera vez y las
 * siguientes actualiza sus puntajes. Así el esquema nunca queda con dos evaluaciones del colaborador
 * para la misma pauta y línea (la base de datos no lo impide por sí sola).
 *
 * Todo se identifica por la URL (línea y pauta) y el colaborador sale de la sesión; nunca llega
 * un id de evaluación desde el navegador.
 *
 * Orden de las validaciones:
 *   existe la línea (404) -> es del colaborador (403) -> la pauta corresponde a la línea (404)
 *   -> los puntajes son válidos (400, con todos los errores juntos).
 */
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

    /**
     * Guarda la evaluación del colaborador para esa línea y pauta.
     * No escribe nada si algo falla: la validación ocurre antes de cualquier escritura.
     *
     * @throws EvaluacionInvalidaException 400 con la lista de errores de puntajes
     * @throws ResponseStatusException     404 línea o pauta inexistente para esa URL; 403 línea de otro colaborador
     */
    @Transactional
    public void guardar(Long idColaborador, Long idLinea, Long idPauta, List<DetalleEvaluacionRequest> detalles) {
        LineaPractica linea = lineaConsulta.obtenerLineaDelColaborador(idColaborador, idLinea);
        PautaEvaluacion pauta = pautaAplicable(linea, idPauta);

        List<Criterio> criterios = cargador.criteriosDePauta(pauta.getIdPautaEvaluacion());
        BigDecimal total = validador.validar(criterios, detalles);

        Map<Long, Criterio> criterioPorId = criterios.stream()
                .collect(Collectors.toMap(Criterio::getIdCriterio, Function.identity()));

        Optional<EvaluacionAplicada> existente = evaluacionExistente(idLinea, idPauta);
        if (existente.isPresent()) {
            actualizarExistente(existente.get(), criterioPorId, detalles, total);
        } else {
            crearNueva(linea, pauta, criterioPorId, detalles, total);
        }
    }

    private void crearNueva(LineaPractica linea, PautaEvaluacion pauta, Map<Long, Criterio> criterioPorId,
                            List<DetalleEvaluacionRequest> detalles, BigDecimal total) {
        EvaluacionAplicada evaluacion = evaluacionRepository.save(
                mapper.nuevaEvaluacion(linea, pauta, total, LocalDateTime.now()));

        List<DetalleCriterio> nuevos = new ArrayList<>();
        for (DetalleEvaluacionRequest d : detalles) {
            nuevos.add(mapper.nuevoDetalle(evaluacion, criterioPorId.get(d.idCriterio()), d.puntaje(), d.comentario()));
        }
        detalleRepository.saveAll(nuevos);
    }

    /**
     * Actualiza los detalles EN EL LUGAR (por criterio). Borrar y volver a insertar chocaría con
     * uq_detalle (id_evaluacion, id_criterio), porque Hibernate ejecuta los INSERT antes que los DELETE.
     */
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

    /**
     * Arma el formulario. Si se pasan los textos enviados (tras un error de validación) se repintan tal cual;
     * si no, se precarga con lo ya guardado (edición) o se deja en blanco (evaluación nueva).
     *
     * @throws ResponseStatusException 404 si la pauta no corresponde a esa línea; 403 si la línea no es tuya
     */
    @Transactional(readOnly = true)
    public FormularioEvaluacion formulario(Long idColaborador, Long idLinea, Long idPauta,
                                           Map<Long, String> puntajesEnviados,
                                           Map<Long, String> comentariosEnviados) {
        LineaPractica linea = lineaConsulta.obtenerLineaDelColaborador(idColaborador, idLinea);
        PautaEvaluacion pauta = pautaAplicable(linea, idPauta);

        Optional<EvaluacionAplicada> existente = evaluacionExistente(idLinea, idPauta);
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

    // ---------------------------------------------------------------- internos

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

    /** La pauta debe existir y corresponder a la asignatura y al tipo de la línea; si no, 404 (URL inválida). */
    private PautaEvaluacion pautaAplicable(LineaPractica linea, Long idPauta) {
        return pautaRepository.findById(idPauta)
                .filter(p -> ReglasPauta.aplicaALinea(p, linea))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "La pauta no corresponde a esta línea"));
    }

    private Optional<EvaluacionAplicada> evaluacionExistente(Long idLinea, Long idPauta) {
        return evaluacionRepository.findDelColaboradorByLinea(idLinea).stream()
                .filter(e -> Objects.equals(e.getPautaEvaluacion().getIdPautaEvaluacion(), idPauta))
                .findFirst();
    }
}
