package com.grupo1.isw.practicas_pedagogicas.dto;

import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;

public record AsignaturaInscritaResponse(
        Long idAsignatura,
        String nombre,
        int anio,
        int semestre
) {
    public static AsignaturaInscritaResponse desde(InscripcionAsignatura inscripcion) {
        var seccion = inscripcion.getSeccion();
        return new AsignaturaInscritaResponse(
                seccion.getAsignatura().getIdAsignatura(),
                seccion.getAsignatura().getNombre(),
                seccion.getAnio(),
                seccion.getSemestre()
        );
    }
}
