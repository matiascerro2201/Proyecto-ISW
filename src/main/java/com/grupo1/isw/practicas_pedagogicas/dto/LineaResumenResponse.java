package com.grupo1.isw.practicas_pedagogicas.dto;

public record LineaResumenResponse(
        Long idLinea,
        String nombreEstudiante,
        String nombreAsignatura,
        int anio,
        int semestre,
        String tipoLinea
) {}
