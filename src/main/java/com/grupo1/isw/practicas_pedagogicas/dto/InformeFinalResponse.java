package com.grupo1.isw.practicas_pedagogicas.dto;

import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InformeFinalResponse(
        Long idInforme,
        Long idInscripcion,
        String nombreAsignatura,
        int anio,
        int semestre,
        BigDecimal calificacionFinal,
        LocalDateTime fechaGeneracion,
        LocalDateTime fechaValidacion,
        LocalDateTime fechaPublicacion
) {
    public static InformeFinalResponse desde(InformeFinal informe) {
        var inscripcion = informe.getInscripcion();
        var seccion = inscripcion.getSeccion();
        return new InformeFinalResponse(
                informe.getIdInforme(),
                inscripcion.getIdInscripcion(),
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
