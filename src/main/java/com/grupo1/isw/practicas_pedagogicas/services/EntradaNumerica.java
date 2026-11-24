package com.grupo1.isw.practicas_pedagogicas.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Lectura de números y fechas escritos en un formulario (acepta coma o punto decimal). */
public final class EntradaNumerica {
    private static final BigDecimal CIEN = new BigDecimal("100");

    private EntradaNumerica() {
    }

    /** Ponderación en porcentaje: mayor que 0, hasta 100 y con a lo más dos decimales. null si es inválida. */
    public static BigDecimal ponderacion(String texto, List<String> errores) {
        BigDecimal valor = decimal(texto, "La ponderación", errores);
        if (valor == null) {
            return null;
        }
        if (valor.signum() <= 0 || valor.compareTo(CIEN) > 0) {
            errores.add("La ponderación debe ser mayor que 0 y no puede superar 100 %");
            return null;
        }
        return valor;
    }

    /** Número decimal con a lo más dos decimales. null si falta o es inválido (agrega el error). */
    public static BigDecimal decimal(String texto, String nombreCampo, List<String> errores) {
        if (texto == null || texto.isBlank()) {
            errores.add(nombreCampo + " es obligatoria");
            return null;
        }
        BigDecimal valor;
        try {
            valor = new BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            errores.add(nombreCampo + " debe ser un número");
            return null;
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            errores.add(nombreCampo + " admite como máximo dos decimales");
            return null;
        }
        return valor;
    }

    /** Fecha en formato AAAA-MM-DD (el de {@code <input type="date">}). null si viene vacía. */
    public static LocalDate fecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha no es válida");
        }
    }
}
