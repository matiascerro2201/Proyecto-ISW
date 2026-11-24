package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaSemestreRepository.EntregaFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaSemestreRepository.PautaFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository.SeccionDatos;

import java.math.BigDecimal;
import java.util.List;

/**
 * La estructura de evaluación de un semestre: entregas y pautas con sus ponderaciones.
 * {@code suma} cuenta solo lo habilitado (N-05); lo deshabilitado no entra en la nota.
 */
public record EstructuraVista(SeccionDatos seccion, List<EntregaFila> entregas, List<PautaFila> pautas,
                              BigDecimal suma) {
    private static final BigDecimal CIEN = new BigDecimal("100");

    public boolean suma100() {
        return suma.compareTo(CIEN) == 0;
    }

    /** Cuánto falta para llegar a 100 (negativo si se pasó). */
    public BigDecimal diferencia() {
        return CIEN.subtract(suma);
    }

    /** Mientras el semestre no se inicia, el coordinador puede cambiar la estructura. */
    public boolean editable() {
        return !seccion.iniciada();
    }
}
