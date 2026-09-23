package com.grupo1.isw.practicas_pedagogicas.dto;

import java.math.BigDecimal;
import java.util.List;

public record FormularioEvaluacion(
        Long idLinea,
        Long idPautaEvaluacion,
        String nombreEstudiante,
        String nombreAsignatura,
        String tipoLinea,
        int numeroAplicacion,
        BigDecimal ponderacion,
        boolean edicion,
        List<DimensionForm> dimensiones
) {
    public record DimensionForm(String nombre, List<CriterioForm> criterios) {}

    public record CriterioForm(
            Long idCriterio,
            String descripcion,
            BigDecimal puntajeMax,
            String puntaje,
            String comentario
    ) {}
}
