package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pauta_evaluacion")
public class PautaEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pauta_evaluacion")
    private Long idPautaEvaluacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_asignatura", nullable = false)
    private Asignatura asignatura;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_linea", nullable = false)
    private TipoLinea tipoLinea;

    @Column(name = "numero_aplicacion", nullable = false)
    private Integer numeroAplicacion = 1;

    @Column(name = "ponderacion", nullable = false, precision = 5, scale = 2)
    private BigDecimal ponderacion;

    /** HU-38: el coordinador decide si la pauta está vigente. Una pauta deshabilitada no se cuenta como pendiente. */
    @Column(name = "habilitada", nullable = false)
    private boolean habilitada = true;

    @OneToMany(mappedBy = "pautaEvaluacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Dimension> dimensiones = new ArrayList<>();

    @OneToMany(mappedBy = "pautaEvaluacion")
    private List<EvaluacionAplicada> evaluacionesAplicadas = new ArrayList<>();

    public PautaEvaluacion() {
    }

    public PautaEvaluacion(Asignatura asignatura, TipoLinea tipoLinea, Integer numeroAplicacion, BigDecimal ponderacion) {
        this.asignatura = asignatura;
        this.tipoLinea = tipoLinea;
        this.numeroAplicacion = numeroAplicacion;
        this.ponderacion = ponderacion;
    }

    public Long getIdPautaEvaluacion() {
        return idPautaEvaluacion;
    }

    public void setIdPautaEvaluacion(Long idPautaEvaluacion) {
        this.idPautaEvaluacion = idPautaEvaluacion;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public void setAsignatura(Asignatura asignatura) {
        this.asignatura = asignatura;
    }

    public TipoLinea getTipoLinea() {
        return tipoLinea;
    }

    public void setTipoLinea(TipoLinea tipoLinea) {
        this.tipoLinea = tipoLinea;
    }

    public Integer getNumeroAplicacion() {
        return numeroAplicacion;
    }

    public void setNumeroAplicacion(Integer numeroAplicacion) {
        this.numeroAplicacion = numeroAplicacion;
    }

    public BigDecimal getPonderacion() {
        return ponderacion;
    }

    public void setPonderacion(BigDecimal ponderacion) {
        this.ponderacion = ponderacion;
    }

    public boolean isHabilitada() {
        return habilitada;
    }

    public void setHabilitada(boolean habilitada) {
        this.habilitada = habilitada;
    }

    public List<Dimension> getDimensiones() {
        return dimensiones;
    }

    public void setDimensiones(List<Dimension> dimensiones) {
        this.dimensiones = dimensiones;
    }

    public List<EvaluacionAplicada> getEvaluacionesAplicadas() {
        return evaluacionesAplicadas;
    }

    public void setEvaluacionesAplicadas(List<EvaluacionAplicada> evaluacionesAplicadas) {
        this.evaluacionesAplicadas = evaluacionesAplicadas;
    }
}
