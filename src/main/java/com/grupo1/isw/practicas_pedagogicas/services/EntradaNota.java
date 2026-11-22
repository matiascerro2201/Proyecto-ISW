package com.grupo1.isw.practicas_pedagogicas.services;

import java.math.BigDecimal;
import java.util.List;

/** Lectura de una nota escrita en un formulario: de 1,0 a 7,0 con un decimal (acepta coma o punto). */
public final class EntradaNota {
    public static final BigDecimal MINIMA = new BigDecimal("1.0");
    public static final BigDecimal MAXIMA = new BigDecimal("7.0");

    private EntradaNota() {
    }

    /** null si la nota falta o es inválida (en ese caso agrega el motivo a {@code errores}). */
    public static BigDecimal leer(String texto, List<String> errores) {
        if (texto == null || texto.isBlank()) {
            errores.add("La nota es obligatoria");
            return null;
        }
        BigDecimal valor;
        try {
            valor = new BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            errores.add("La nota debe ser un número");
            return null;
        }
        if (valor.stripTrailingZeros().scale() > 1) {
            errores.add("La nota admite un solo decimal");
            return null;
        }
        if (valor.compareTo(MINIMA) < 0 || valor.compareTo(MAXIMA) > 0) {
            errores.add("La nota debe estar entre 1,0 y 7,0");
            return null;
        }
        return valor.setScale(1);
    }
}
