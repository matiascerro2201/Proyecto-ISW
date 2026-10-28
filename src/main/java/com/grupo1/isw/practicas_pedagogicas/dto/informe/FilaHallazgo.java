package com.grupo1.isw.practicas_pedagogicas.dto.informe;

import com.grupo1.isw.practicas_pedagogicas.models.NivelHallazgo;

import java.time.LocalDateTime;

/** Lo que el asistente valoró de un aspecto de una planificación, en una consulta concreta. */
public record FilaHallazgo(Long idPlanificacion, Long idInteraccion, LocalDateTime fecha,
                           String codigoCategoria, NivelHallazgo nivel, String observacion) {
}
