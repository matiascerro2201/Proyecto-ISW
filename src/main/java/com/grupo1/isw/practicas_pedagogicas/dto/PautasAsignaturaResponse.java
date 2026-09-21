package com.grupo1.isw.practicas_pedagogicas.dto;

import java.util.List;

/** Todas las pautas de una asignatura, con el nombre de la asignatura para mostrarlo en pantalla. */
public record PautasAsignaturaResponse(
        Long idAsignatura,
        String nombreAsignatura,
        List<PautaEvaluacionResponse> pautas
) {}
