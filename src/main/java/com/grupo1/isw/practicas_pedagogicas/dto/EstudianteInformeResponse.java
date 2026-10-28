package com.grupo1.isw.practicas_pedagogicas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** HU-10: fila de la lista del profesor: un estudiante y el estado de su informe (pendientes solo si es PENDIENTE). */
public record EstudianteInformeResponse(
        Long idInscripcion,
        String nombreEstudiante,
        String nombreAsignatura,
        int anio,
        int semestre,
        Estado estado,
        Long idInforme,
        BigDecimal calificacionSugerida,
        BigDecimal calificacionFinal,
        LocalDateTime fechaGeneracion,
        List<String> pendientes
) {
    public enum Estado {
        /** Faltan evaluaciones o entregas: todavía no se puede generar. */
        PENDIENTE,
        /** Tiene todo lo necesario y aún no se generó. */
        LISTO,
        /** Generado; el profesor aún no lo valida ni lo publica. Puede volver a generarse. */
        GENERADO,
        VALIDADO,
        PUBLICADO
    }

    public EstudianteInformeResponse {
        pendientes = List.copyOf(pendientes);
    }

    public boolean puedeGenerar() {
        return estado == Estado.LISTO;
    }

    public boolean tieneInforme() {
        return idInforme != null;
    }
}
