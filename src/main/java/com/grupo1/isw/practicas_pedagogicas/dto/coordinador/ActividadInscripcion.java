package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * El estado de un estudiante en un curso: sus líneas de práctica, evaluaciones, planificaciones, entregas e
 * informe final (HU-37). Solo lectura: son estados y fechas, nunca el contenido de una planificación.
 */
public record ActividadInscripcion(
        Long idInscripcion,
        Long idEstudiante,
        String estudiante,
        String email,
        String carrera,
        String asignatura,
        int anio,
        int semestre,
        String estadoInscripcion,
        String centro,
        List<LineaActividad> lineas,
        List<EvaluacionActividad> evaluaciones,
        List<PlanificacionActividad> planificaciones,
        List<EntregaActividad> entregas,
        InformeActividad informe) {

    public String periodo() {
        return "Semestre " + semestre + " de " + anio;
    }

    public long evaluacionesRealizadas() {
        return evaluaciones.stream().filter(EvaluacionActividad::realizada).count();
    }

    public int evaluacionesTotales() {
        return evaluaciones.size();
    }

    public long planificacionesPorRevisar() {
        return planificaciones.stream().filter(p -> "ENVIADA".equals(p.estadoClave())).count();
    }

    public long entregasRealizadas() {
        return entregas.stream().filter(EntregaActividad::entregado).count();
    }

    public int entregasTotales() {
        return entregas.size();
    }

    public String informeEstado() {
        return informe == null ? "Sin informe" : informe.estado();
    }

    public record LineaActividad(String tipo, String colaborador, String tutor) {
    }

    /** Una pauta que se espera de un evaluador: realizada (con puntaje y fecha) o pendiente. */
    public record EvaluacionActividad(String linea, String pauta, String evaluador, boolean realizada,
                                      BigDecimal puntaje, LocalDateTime fecha, LocalDate fechaLimite) {
    }

    public record PlanificacionActividad(Long id, String linea, String estadoClave, String estado,
                                         LocalDateTime fechaEnvio, LocalDateTime fechaRetro) {
    }

    public record EntregaActividad(String documento, boolean entregado, LocalDateTime fechaSubida,
                                   BigDecimal puntaje) {
    }

    public record InformeActividad(String estado, BigDecimal calificacionSugerida, BigDecimal calificacionFinal,
                                   LocalDateTime fechaPublicacion) {
    }
}
