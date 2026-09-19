package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "criterio")
public class Criterio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_criterio")
    private Long idCriterio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_dimension", nullable = false)
    private Dimension dimension;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;

    @Column(name = "puntaje_max", nullable = false, precision = 5, scale = 2)
    private BigDecimal puntajeMax;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    public Criterio() {
    }

    public Criterio(Dimension dimension, String descripcion, BigDecimal puntajeMax, Integer orden) {
        this.dimension = dimension;
        this.descripcion = descripcion;
        this.puntajeMax = puntajeMax;
        this.orden = orden;
    }

    public Long getIdCriterio() {
        return idCriterio;
    }

    public void setIdCriterio(Long idCriterio) {
        this.idCriterio = idCriterio;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPuntajeMax() {
        return puntajeMax;
    }

    public void setPuntajeMax(BigDecimal puntajeMax) {
        this.puntajeMax = puntajeMax;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }
}
