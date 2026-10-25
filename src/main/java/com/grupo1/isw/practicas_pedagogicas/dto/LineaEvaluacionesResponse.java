package com.grupo1.isw.practicas_pedagogicas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LineaEvaluacionesResponse(
        Long idLinea,
        String nombreEstudiante,
        String nombreAsignatura,
        int anio,
        int semestre,
        String tipoLinea,
        List<PautaEstado> pautas
) {
    public record PautaEstado(
            Long idPautaEvaluacion,
            int numeroAplicacion,
            BigDecimal ponderacion,
            Long idEvaluacion,
            BigDecimal puntajeTotal,
            LocalDateTime fecha
    ) {}
}
