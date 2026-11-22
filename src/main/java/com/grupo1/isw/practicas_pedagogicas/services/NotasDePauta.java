package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository.EvaluacionEsperada;

import java.math.BigDecimal;

/** La nota (1,0 a 7,0) de una pauta aplicada, con la misma escala y exigencia del resto del sistema. */
public final class NotasDePauta {
    private NotasDePauta() {
    }

    /** Nota sin redondear, o null si la pauta no se aplicó o no tiene puntaje máximo. */
    public static BigDecimal exacta(CalculadoraNota calculadora, EvaluacionEsperada evaluacion) {
        if (!evaluacion.aplicada() || evaluacion.puntajeMaximo() == null || evaluacion.puntajeMaximo().signum() <= 0) {
            return null;
        }
        return calculadora.notaDesdeLogro(calculadora.logro(evaluacion.puntaje(), evaluacion.puntajeMaximo()));
    }

    /** Nota a un decimal para mostrar, o null. */
    public static BigDecimal redondeada(CalculadoraNota calculadora, EvaluacionEsperada evaluacion) {
        BigDecimal exacta = exacta(calculadora, evaluacion);
        return exacta == null ? null : calculadora.redondear(exacta);
    }
}
