package com.grupo1.isw.practicas_pedagogicas.dto;

import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;

/** Asignatura en la que el estudiante está inscrito (para el listado "Mis pautas"). */
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
