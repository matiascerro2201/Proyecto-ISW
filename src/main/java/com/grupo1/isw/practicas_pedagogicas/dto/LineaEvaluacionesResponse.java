package com.grupo1.isw.practicas_pedagogicas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Una línea con sus pautas aplicables y, para cada una, si el colaborador ya la evaluó. */
public record LineaEvaluacionesResponse(
        Long idLinea,
        String nombreEstudiante,
        String nombreAsignatura,
        int anio,
        int semestre,
        String tipoLinea,
        List<PautaEstado> pautas
) {

    /** idEvaluacion, puntajeTotal y fecha son nulos si la pauta aún no fue evaluada. */
    public record PautaEstado(
            Long idPautaEvaluacion,
            int numeroAplicacion,
            BigDecimal ponderacion,
            Long idEvaluacion,
            BigDecimal puntajeTotal,
            LocalDateTime fecha
    ) {}
}
