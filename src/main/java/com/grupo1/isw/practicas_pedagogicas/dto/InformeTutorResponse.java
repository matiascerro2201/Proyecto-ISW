package com.grupo1.isw.practicas_pedagogicas.dto;

import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * HU-14: el informe final de un estudiante tal como lo ve su profesor supervisor (tutor).
 * Solo trae la nota final y las fechas: la calificación sugerida por el sistema es un borrador del
 * profesor de la asignatura y, igual que el estudiante, el tutor no la recibe.
 */
public record InformeTutorResponse(
        Long idInforme,
        String nombreEstudiante,
        String nombreAsignatura,
        int anio,
        int semestre,
        BigDecimal calificacionFinal,
        LocalDateTime fechaGeneracion,
        LocalDateTime fechaValidacion,
        LocalDateTime fechaPublicacion
) {
    public static InformeTutorResponse desde(InformeFinal informe) {
        var inscripcion = informe.getInscripcion();
        var seccion = inscripcion.getSeccion();
        return new InformeTutorResponse(
                informe.getIdInforme(),
                inscripcion.getEstudiante().getNombre(),
                seccion.getAsignatura().getNombre(),
                seccion.getAnio(),
                seccion.getSemestre(),
                informe.getCalificacionFinal(),
                informe.getFechaGeneracion(),
                informe.getFechaValidacion(),
                informe.getFechaPublicacion()
        );
    }
}
