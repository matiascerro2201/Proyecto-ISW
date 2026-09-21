package com.grupo1.isw.practicas_pedagogicas.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pauta (plantilla) de evaluación con su árbol completo: pauta -> dimensiones -> criterios.
 * Es de solo lectura: no expone entidades JPA, así que no hay forma de modificarlas desde aquí.
 *
 * orden y numeroAplicacion son int primitivo a propósito: aceptan Byte, Short o Integer de la
 * entidad sin ajustar tipos (las columnas TINYINT son NOT NULL en el esquema).
 */
public record PautaEvaluacionResponse(
        Long idPautaEvaluacion,
        String tipoLinea,               // UNICA | ESPECIALIDAD | ORIENTACION
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
