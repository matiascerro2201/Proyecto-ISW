package com.grupo1.isw.practicas_pedagogicas.dto;

import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * HU-11: lo que ve el profesor al revisar un informe final antes de publicarlo.
 * Incluye la calificación sugerida, que el estudiante nunca recibe (ver {@link InformeFinalResponse}).
 */
public record InformeRevisionResponse(
        Long idInforme,
        String nombreEstudiante,
        String nombreAsignatura,
        int anio,
        int semestre,
        BigDecimal calificacionSugerida,
        BigDecimal calificacionFinal,
        LocalDateTime fechaGeneracion,
        LocalDateTime fechaValidacion,
        LocalDateTime fechaPublicacion
) {
    /** PENDIENTE: nadie lo revisó. VALIDADO: el profesor fijó la nota. PUBLICADO: ya lo ve el estudiante. */
    public enum Estado {
        PENDIENTE, VALIDADO, PUBLICADO
    }

    public static InformeRevisionResponse desde(InformeFinal informe) {
        var inscripcion = informe.getInscripcion();
        var seccion = inscripcion.getSeccion();
        return new InformeRevisionResponse(
                informe.getIdInforme(),
                inscripcion.getEstudiante().getNombre(),
                seccion.getAsignatura().getNombre(),
                seccion.getAnio(),
                seccion.getSemestre(),
                informe.getCalificacionSugerida(),
                informe.getCalificacionFinal(),
                informe.getFechaGeneracion(),
                informe.getFechaValidacion(),
                informe.getFechaPublicacion()
        );
    }

    public Estado estado() {
        if (fechaPublicacion != null) {
            return Estado.PUBLICADO;
        }
        if (fechaValidacion != null) {
            return Estado.VALIDADO;
        }
        return Estado.PENDIENTE;
    }

    public boolean publicado() {
        return fechaPublicacion != null;
    }

    /** Mientras no se publique, el profesor puede seguir ajustando la nota. */
    public boolean puedeAjustar() {
        return fechaPublicacion == null;
    }

    /** Solo se publica una nota que el profesor ya fijó explícitamente. */
    public boolean puedePublicar() {
        return fechaPublicacion == null && fechaValidacion != null && calificacionFinal != null;
    }

    /** true si el profesor cambió la nota respecto de la que sugirió el sistema. */
    public boolean ajustada() {
        return calificacionFinal != null
                && calificacionSugerida != null
                && calificacionFinal.compareTo(calificacionSugerida) != 0;
    }
}
