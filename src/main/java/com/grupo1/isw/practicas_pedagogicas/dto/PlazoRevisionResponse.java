package com.grupo1.isw.practicas_pedagogicas.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * HU-12: fecha límite de revisión de una sección del profesor y cuántos informes le quedan por revisar,
 * para que pueda priorizar su tiempo de corrección.
 */
public record PlazoRevisionResponse(
        Long idSeccion,
        String nombreAsignatura,
        int anio,
        int semestre,
        LocalDate fechaLimite,
        int informesPendientes,
        int totalEstudiantes,
        Estado estado,
        long diasRestantes
) {
    /** Días antes del vencimiento en que el plazo se marca como próximo. */
    public static final int DIAS_AVISO = 3;

    public enum Estado {
        /** El profesor no definió fecha límite. */
        SIN_FECHA,
        /** Ya no quedan informes por revisar en la sección. */
        COMPLETADO,
        /** Vence hoy o dentro de {@link #DIAS_AVISO} días. */
        PROXIMO,
        /** Quedan más de {@link #DIAS_AVISO} días. */
        EN_PLAZO,
        /** La fecha ya pasó y aún quedan informes por revisar. */
        VENCIDO
    }

    public static PlazoRevisionResponse de(Long idSeccion, String nombreAsignatura, int anio, int semestre,
                                           LocalDate fechaLimite, int informesPendientes, int totalEstudiantes,
                                           LocalDate hoy) {
        long dias = fechaLimite == null ? 0 : ChronoUnit.DAYS.between(hoy, fechaLimite);
        Estado estado;
        if (fechaLimite == null) {
            estado = Estado.SIN_FECHA;
        } else if (informesPendientes == 0) {
            estado = Estado.COMPLETADO;
        } else if (dias < 0) {
            estado = Estado.VENCIDO;
        } else if (dias <= DIAS_AVISO) {
            estado = Estado.PROXIMO;
        } else {
            estado = Estado.EN_PLAZO;
        }
        return new PlazoRevisionResponse(idSeccion, nombreAsignatura, anio, semestre, fechaLimite,
                informesPendientes, totalEstudiantes, estado, dias);
    }

    /** Texto del estado para la vista. */
    public String textoEstado() {
        return switch (estado) {
            case SIN_FECHA -> "Sin fecha";
            case COMPLETADO -> "Revisión completa";
            case VENCIDO -> diasRestantes == -1 ? "Venció ayer" : "Venció hace " + (-diasRestantes) + " días";
            case PROXIMO, EN_PLAZO -> diasRestantes == 0 ? "Vence hoy"
                    : diasRestantes == 1 ? "Vence mañana"
                    : "Vence en " + diasRestantes + " días";
        };
    }

    /** Clase de color de base.css para el estado. */
    public String claseEstado() {
        return switch (estado) {
            case SIN_FECHA -> "borrador";
            case COMPLETADO, EN_PLAZO -> "aprobada";
            case PROXIMO -> "pendiente";
            case VENCIDO -> "rechazada";
        };
    }
}
