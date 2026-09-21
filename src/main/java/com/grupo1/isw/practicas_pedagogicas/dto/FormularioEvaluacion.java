package com.grupo1.isw.practicas_pedagogicas.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Todo lo que necesita la vista del formulario, ya armado: así la plantilla solo recorre
 * listas y no consulta nada. puntaje y comentario van como texto (vacío si no hay dato)
 * para poder repintar exactamente lo que el usuario escribió cuando hay errores.
 */
public record FormularioEvaluacion(
        Long idLinea,
        Long idPautaEvaluacion,
        String nombreEstudiante,
        String nombreAsignatura,
        String tipoLinea,
        int numeroAplicacion,
        BigDecimal ponderacion,
        boolean edicion,                       // true si ya existe una evaluación guardada
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
