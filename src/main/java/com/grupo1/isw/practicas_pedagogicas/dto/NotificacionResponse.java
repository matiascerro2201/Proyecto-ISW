package com.grupo1.isw.practicas_pedagogicas.dto;

import com.grupo1.isw.practicas_pedagogicas.models.Notificacion;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Long idNotificacion,
        String tipo,
        String mensaje,
        String enlace,
        boolean leida,
        LocalDateTime fecha
) {
    public static NotificacionResponse desde(Notificacion n) {
        return new NotificacionResponse(n.getIdNotificacion(), n.getTipo(), n.getMensaje(),
                n.getEnlace(), n.isLeida(), n.getFecha());
    }
}
