package com.grupo1.isw.practicas_pedagogicas.dto;

import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** HU-10: el informe como lo ve el profesor; desactualizado = los datos cambiaron después de generarlo. */
public record InformeProfesorResponse(
        Long idInforme,
        Long idInscripcion,
        String nombreEstudiante,
        String nombreAsignatura,
        int anio,
        int semestre,
        BigDecimal calificacionSugerida,
        BigDecimal calificacionFinal,
        LocalDateTime fechaGeneracion,
        LocalDateTime fechaValidacion,
        LocalDateTime fechaPublicacion,
        boolean desactualizado,
        DesgloseInforme desglose
) {
    public static InformeProfesorResponse desde(InformeFinal informe, DesgloseInforme desglose, boolean desactualizado) {
        var inscripcion = informe.getInscripcion();
        var seccion = inscripcion.getSeccion();
        return new InformeProfesorResponse(
                informe.getIdInforme(),
                inscripcion.getIdInscripcion(),
                inscripcion.getEstudiante().getNombre(),
                seccion.getAsignatura().getNombre(),
                seccion.getAnio(),
                seccion.getSemestre(),
                informe.getCalificacionSugerida(),
                informe.getCalificacionFinal(),
                informe.getFechaGeneracion(),
                informe.getFechaValidacion(),
                informe.getFechaPublicacion(),
                desactualizado,
                desglose);
    }

    /** Solo se puede volver a generar mientras el profesor no lo haya validado ni publicado. */
    public boolean puedeRegenerar() {
        return fechaValidacion == null && fechaPublicacion == null;
    }
}
