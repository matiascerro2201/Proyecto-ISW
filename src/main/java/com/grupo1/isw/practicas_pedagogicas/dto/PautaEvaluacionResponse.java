package com.grupo1.isw.practicas_pedagogicas.dto;

import java.math.BigDecimal;
import java.util.List;

public record PautaEvaluacionResponse(
        Long idPautaEvaluacion,
        String tipoLinea,
        int numeroAplicacion,
        BigDecimal ponderacion,
        List<DimensionResponse> dimensiones
) {
    public record DimensionResponse(
            Long idDimension,
            String nombre,
            int orden,
            List<CriterioResponse> criterios
    ) {}

    public record CriterioResponse(
            Long idCriterio,
            String descripcion,
            BigDecimal puntajeMax,
            int orden
    ) {}
}
