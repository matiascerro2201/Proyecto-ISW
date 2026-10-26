package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.models.Criterio;
import com.grupo1.isw.practicas_pedagogicas.models.DetalleCriterio;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.TipoEvaluador;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Component
public class EvaluacionMapper {
    public static boolean esDelColaborador(EvaluacionAplicada evaluacion) {
        return "COLABORADOR".equals(String.valueOf(evaluacion.getTipoEvaluador()));
    }

    public EvaluacionAplicada nuevaEvaluacion(LineaPractica linea, PautaEvaluacion pauta,
                                              BigDecimal puntajeTotal, LocalDateTime fecha) {
        return nuevaEvaluacion(linea, pauta, TipoEvaluador.COLABORADOR, puntajeTotal, fecha);
    }

    public EvaluacionAplicada nuevaEvaluacion(LineaPractica linea, PautaEvaluacion pauta,
                                              TipoEvaluador tipoEvaluador,
                                              BigDecimal puntajeTotal, LocalDateTime fecha) {
        EvaluacionAplicada evaluacion = new EvaluacionAplicada();
        evaluacion.setLinea(linea);
        evaluacion.setPautaEvaluacion(pauta);
        evaluacion.setTipoEvaluador(tipoEvaluador);
        evaluacion.setFecha(fecha);
        evaluacion.setPuntajeTotal(puntajeTotal);
        return evaluacion;
    }

    public void actualizarEvaluacion(EvaluacionAplicada evaluacion, BigDecimal puntajeTotal, LocalDateTime fecha) {
        evaluacion.setPuntajeTotal(puntajeTotal);
        evaluacion.setFecha(fecha);
    }

    public DetalleCriterio nuevoDetalle(EvaluacionAplicada evaluacion, Criterio criterio,
                                        BigDecimal puntaje, String comentario) {
        DetalleCriterio detalle = new DetalleCriterio();
        detalle.setEvaluacion(evaluacion);
        detalle.setCriterio(criterio);
        actualizarDetalle(detalle, puntaje, comentario);
        return detalle;
    }

    public void actualizarDetalle(DetalleCriterio detalle, BigDecimal puntaje, String comentario) {
        detalle.setPuntajeObtenido(puntaje.setScale(2, RoundingMode.HALF_UP));
        detalle.setTranscripcionComentario(limpiarComentario(comentario));
    }

    public String limpiarComentario(String comentario) {
        if (comentario == null || comentario.isBlank()) {
            return null;
        }
        return comentario.strip();
    }
}
