package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dimension")
public class Dimension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dimension")
    private Long idDimension;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pauta_evaluacion", nullable = false)
    private PautaEvaluacion pautaEvaluacion;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @OneToMany(mappedBy = "dimension", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Criterio> criterios = new ArrayList<>();

    public Dimension() {
    }

    public Dimension(PautaEvaluacion pautaEvaluacion, String nombre, Integer orden) {
        this.pautaEvaluacion = pautaEvaluacion;
        this.nombre = nombre;
        this.orden = orden;
    }

    public Long getIdDimension() {
        return idDimension;
    }

    public void setIdDimension(Long idDimension) {
        this.idDimension = idDimension;
    }

    public PautaEvaluacion getPautaEvaluacion() {
        return pautaEvaluacion;
    }

    public void setPautaEvaluacion(PautaEvaluacion pautaEvaluacion) {
        this.pautaEvaluacion = pautaEvaluacion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public List<Criterio> getCriterios() {
        return criterios;
    }

    public void setCriterios(List<Criterio> criterios) {
        this.criterios = criterios;
    }
}
