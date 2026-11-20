package com.grupo1.isw.practicas_pedagogicas.dto.inicio;

import java.time.LocalDate;

public record PendienteInicio(String titulo, String detalle, LocalDate fecha, String enlace,
                              String etiqueta, String claseEstado, boolean vencida) {
    public static PendienteInicio porHacer(String titulo, String detalle, String enlace,
                                           String etiqueta, String claseEstado) {
        return new PendienteInicio(titulo, detalle, null, enlace, etiqueta, claseEstado, false);
    }
}
