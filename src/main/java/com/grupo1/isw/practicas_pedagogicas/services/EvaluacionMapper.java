package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.models.Criterio;
import com.grupo1.isw.practicas_pedagogicas.models.DetalleCriterio;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.TipoEvaluador;      // <-- AJUSTAR el import si el enum está en otro paquete
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * ÚNICO lugar que crea y modifica las entidades EvaluacionAplicada y DetalleCriterio.
 * Si algún setter de tus entidades se llama distinto, se corrige aquí y en ningún otro archivo.
 *
 * Líneas marcadas con "AJUSTAR": son las que más probablemente difieran de tus entidades reales.
 */
@Component
public class EvaluacionMapper {

    /** ¿La hizo el colaborador (y no el tutor)? Compara el texto, sirve si el campo es enum o String. */
    public static boolean esDelColaborador(EvaluacionAplicada evaluacion) {
        return "COLABORADOR".equals(String.valueOf(evaluacion.getTipoEvaluador()));
    }

    public EvaluacionAplicada nuevaEvaluacion(LineaPractica linea, PautaEvaluacion pauta,
                                              BigDecimal puntajeTotal, LocalDateTime fecha) {
        EvaluacionAplicada evaluacion = new EvaluacionAplicada();
        evaluacion.setLinea(linea);                              // AJUSTAR: nombre del setter de la relación
        evaluacion.setPautaEvaluacion(pauta);                    // AJUSTAR
        evaluacion.setTipoEvaluador(TipoEvaluador.COLABORADOR);  // AJUSTAR: si el campo es String, usa "COLABORADOR"
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
        detalle.setEvaluacion(evaluacion);                       // AJUSTAR
        detalle.setCriterio(criterio);
        actualizarDetalle(detalle, puntaje, comentario);
        return detalle;
    }

    public void actualizarDetalle(DetalleCriterio detalle, BigDecimal puntaje, String comentario) {
        detalle.setPuntajeObtenido(puntaje.setScale(2, RoundingMode.HALF_UP));
        detalle.setTranscripcionComentario(limpiarComentario(comentario));   // AJUSTAR
    }

    /** Comentario en blanco -> null (la columna acepta NULL); si no, sin espacios sobrantes. */
    public String limpiarComentario(String comentario) {
        if (comentario == null || comentario.isBlank()) {
            return null;
        }
        return comentario.strip();
    }
}
