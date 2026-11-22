package com.grupo1.isw.practicas_pedagogicas.dto;

import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.services.AlertaRevisionPlanificacionService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Una planificación enviada que el colaborador aún no evalúa: cuánto lleva esperando (antigüedad) y qué tan
 * cerca está de la fecha para la que el estudiante pidió la revisión (urgencia).
 */
public record PlanificacionPendienteResponse(
        Long idPlanificacion,
        String nombreEstudiante,
        String asignatura,
        String linea,
        LocalDateTime fechaEnvio,
        LocalDate fechaRevision,
        long diasEnEspera,
        long diasRestantes,
        Urgencia urgencia
) {
    public enum Urgencia {
        /** Planificaciones anteriores a la fecha de revisión: no tienen fecha pedida. */
        SIN_FECHA,
        /** Faltan más de {@link AlertaRevisionPlanificacionService#DIAS_DE_ANTICIPACION} días. */
        EN_PLAZO,
        /** Faltan 2 días o menos (el mismo umbral del aviso por notificación). */
        PROXIMA,
        /** La fecha pedida ya pasó. */
        VENCIDA
    }

    public static PlanificacionPendienteResponse de(Planificacion p, LocalDate hoy) {
        LineaPractica linea = p.getLinea();
        LocalDate revision = p.getFechaRevisionSolicitada();
        long enEspera = p.getFechaEnvio() == null ? 0
                : Math.max(0, ChronoUnit.DAYS.between(p.getFechaEnvio().toLocalDate(), hoy));
        long restantes = revision == null ? 0 : ChronoUnit.DAYS.between(hoy, revision);

        Urgencia urgencia;
        if (revision == null) {
            urgencia = Urgencia.SIN_FECHA;
        } else if (restantes < 0) {
            urgencia = Urgencia.VENCIDA;
        } else if (restantes <= AlertaRevisionPlanificacionService.DIAS_DE_ANTICIPACION) {
            urgencia = Urgencia.PROXIMA;
        } else {
            urgencia = Urgencia.EN_PLAZO;
        }

        return new PlanificacionPendienteResponse(
                p.getIdPlanificacion(),
                linea.getInscripcion().getEstudiante().getNombre(),
                linea.getInscripcion().getSeccion().getAsignatura().getNombre(),
                nombreLinea(linea),
                p.getFechaEnvio(), revision, enEspera, restantes, urgencia);
    }

    private static String nombreLinea(LineaPractica linea) {
        String tipo = String.valueOf(linea.getTipo());
        return switch (tipo) {
            case "UNICA" -> "Línea única";
            case "ESPECIALIDAD" -> "Especialidad";
            case "ORIENTACION" -> "Orientación";
            default -> tipo;
        };
    }

    /** "Hoy", "Hace 1 día" o "Hace N días" desde el envío. */
    public String textoAntiguedad() {
        return diasEnEspera == 0 ? "Enviada hoy"
                : diasEnEspera == 1 ? "Enviada hace 1 día"
                : "Enviada hace " + diasEnEspera + " días";
    }

    public String textoEstado() {
        return switch (urgencia) {
            case SIN_FECHA -> "Sin fecha";
            case VENCIDA -> diasRestantes == -1 ? "Venció ayer" : "Venció hace " + (-diasRestantes) + " días";
            case PROXIMA, EN_PLAZO -> diasRestantes == 0 ? "Vence hoy"
                    : diasRestantes == 1 ? "Vence mañana"
                    : "Vence en " + diasRestantes + " días";
        };
    }

    /** Clase de color de base.css, la misma convención que los plazos de revisión de informes. */
    public String claseEstado() {
        return switch (urgencia) {
            case SIN_FECHA -> "borrador";
            case EN_PLAZO -> "aprobada";
            case PROXIMA -> "pendiente";
            case VENCIDA -> "rechazada";
        };
    }
}
