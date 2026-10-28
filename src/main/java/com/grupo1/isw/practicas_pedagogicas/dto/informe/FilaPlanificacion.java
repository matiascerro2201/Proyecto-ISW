package com.grupo1.isw.practicas_pedagogicas.dto.informe;

import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;

/** Una planificación de la sección, con su estudiante y su estado actual. */
public record FilaPlanificacion(Long idPlanificacion, Long idEstudiante, EstadoPlanificacion estado) {
}
