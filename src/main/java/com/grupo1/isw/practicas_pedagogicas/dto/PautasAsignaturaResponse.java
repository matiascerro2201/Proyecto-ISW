package com.grupo1.isw.practicas_pedagogicas.dto;

import java.util.List;

public record PautasAsignaturaResponse(
        Long idAsignatura,
        String nombreAsignatura,
        List<PautaEvaluacionResponse> pautas
) {}
