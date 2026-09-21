package com.grupo1.isw.practicas_pedagogicas.dto;

/**
 * Una línea de práctica del colaborador (un estudiante en una asignatura).
 * Solo expone el nombre del estudiante: nunca email ni password_hash.
 * anio y semestre son int primitivo para aceptar Byte/Short/Integer de la entidad.
 */
public record LineaResumenResponse(
        Long idLinea,
        String nombreEstudiante,
        String nombreAsignatura,
        int anio,
        int semestre,
        String tipoLinea            // UNICA | ESPECIALIDAD | ORIENTACION
) {}
