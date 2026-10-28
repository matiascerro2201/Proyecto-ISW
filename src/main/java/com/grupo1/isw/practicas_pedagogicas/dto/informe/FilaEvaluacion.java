package com.grupo1.isw.practicas_pedagogicas.dto.informe;

import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;

import java.time.LocalDateTime;

/** Una decisión del colaborador (APROBADA o RECHAZADA) sobre una planificación. */
public record FilaEvaluacion(Long idEvaluacion, Long idPlanificacion, EstadoPlanificacion decision,
                             LocalDateTime fecha) {
}
