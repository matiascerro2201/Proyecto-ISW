package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Una decisión del colaborador sobre una planificación. Es un registro histórico: la planificación solo
 * guarda su estado actual y borra la retroalimentación al reenviarse, y esto conserva cada evaluación.
 * La decisión es APROBADA o RECHAZADA (los únicos valores que admite la tabla).
 */
@Entity
@Table(name = "evaluacion_planificacion")
public class EvaluacionPlanificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    private Long idEvaluacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_planificacion", nullable = false)
    private Planificacion planificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false)
    private EstadoPlanificacion decision;

    @Column(name = "retroalimentacion", columnDefinition = "TEXT")
    private String retroalimentacion;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public EvaluacionPlanificacion() {
    }

    public EvaluacionPlanificacion(Planificacion planificacion, EstadoPlanificacion decision,
                                   String retroalimentacion, LocalDateTime fecha) {
        this.planificacion = planificacion;
        this.decision = decision;
        this.retroalimentacion = retroalimentacion;
        this.fecha = fecha;
    }

    public Long getIdEvaluacion() {
        return idEvaluacion;
    }

    public Planificacion getPlanificacion() {
        return planificacion;
    }

    public EstadoPlanificacion getDecision() {
        return decision;
    }

    public String getRetroalimentacion() {
        return retroalimentacion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
