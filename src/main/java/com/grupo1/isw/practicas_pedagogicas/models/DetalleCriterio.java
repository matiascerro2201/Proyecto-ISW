package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_criterio",
       uniqueConstraints = @UniqueConstraint(name = "uq_detalle", columnNames = {"id_evaluacion", "id_criterio"}))
public class DetalleCriterio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long idDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evaluacion", nullable = false)
    private EvaluacionAplicada evaluacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_criterio", nullable = false)
    private Criterio criterio;

    @Column(name = "puntaje_obtenido", nullable = false, precision = 5, scale = 2)
    private BigDecimal puntajeObtenido;

    @Lob
    @Column(name = "transcripcion_comentario")
    private String transcripcionComentario;

    public DetalleCriterio() {
    }

    public DetalleCriterio(EvaluacionAplicada evaluacion, Criterio criterio, BigDecimal puntajeObtenido) {
        this.evaluacion = evaluacion;
        this.criterio = criterio;
        this.puntajeObtenido = puntajeObtenido;
    }

    public Long getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(Long idDetalle) {
        this.idDetalle = idDetalle;
    }

    public EvaluacionAplicada getEvaluacion() {
        return evaluacion;
    }

    public void setEvaluacion(EvaluacionAplicada evaluacion) {
        this.evaluacion = evaluacion;
    }

    public Criterio getCriterio() {
        return criterio;
    }

    public void setCriterio(Criterio criterio) {
        this.criterio = criterio;
    }

    public BigDecimal getPuntajeObtenido() {
        return puntajeObtenido;
    }

    public void setPuntajeObtenido(BigDecimal puntajeObtenido) {
        this.puntajeObtenido = puntajeObtenido;
    }

    public String getTranscripcionComentario() {
        return transcripcionComentario;
    }

    public void setTranscripcionComentario(String transcripcionComentario) {
        this.transcripcionComentario = transcripcionComentario;
    }
}
