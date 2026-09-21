package com.grupo1.isw.practicas_pedagogicas.dto;

import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vista del informe final que se expone al estudiante.
 * Intencionalmente NO incluye calificacion_sugerida (interna hasta que el profesor
 * de asignatura valida) ni ninguna referencia a Usuario/Estudiante (evita filtrar password_hash).
 *
 * anio y semestre son int primitivo a propósito: aceptan Short, Integer o Byte de la entidad
 * sin tener que ajustar el tipo aquí (ambos son NOT NULL en el esquema).
 */
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
