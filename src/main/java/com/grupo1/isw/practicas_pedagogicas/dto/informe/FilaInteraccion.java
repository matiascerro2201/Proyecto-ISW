package com.grupo1.isw.practicas_pedagogicas.dto.informe;

import java.time.LocalDateTime;

/** Una consulta de un estudiante al asistente, sobre una de sus planificaciones. */
public record FilaInteraccion(Long idPlanificacion, Long idEstudiante, Long idInteraccion, LocalDateTime fecha) {
}
