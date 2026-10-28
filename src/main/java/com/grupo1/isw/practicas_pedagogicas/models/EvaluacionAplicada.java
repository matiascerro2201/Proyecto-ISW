package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluacion_aplicada")
public class EvaluacionAplicada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    private Long idEvaluacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_linea", nullable = false)
    private LineaPractica linea;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pauta_evaluacion", nullable = false)
    private PautaEvaluacion pautaEvaluacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evaluador", nullable = false)
    private TipoEvaluador tipoEvaluador;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "puntaje_total", precision = 6, scale = 2)
    private BigDecimal puntajeTotal;

    @OneToMany(mappedBy = "evaluacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleCriterio> detalles = new ArrayList<>();

    // --- NUEVOS CAMPOS HU-06 ---
    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(name = "estado", length = 20)
    private String estado = "PENDIENTE";
    // ---------------------------

    public EvaluacionAplicada() {
    }

    public EvaluacionAplicada(LineaPractica linea, PautaEvaluacion pautaEvaluacion, TipoEvaluador tipoEvaluador, LocalDateTime fecha) {
        this.linea = linea;
        this.pautaEvaluacion = pautaEvaluacion;
        this.tipoEvaluador = tipoEvaluador;
        this.fecha = fecha;
    }

    public Long getIdEvaluacion() {
        return idEvaluacion;
    }

    public void setIdEvaluacion(Long idEvaluacion) {
        this.idEvaluacion = idEvaluacion;
    }

    public LineaPractica getLinea() {
        return linea;
    }

    public void setLinea(LineaPractica linea) {
        this.linea = linea;
    }

    public PautaEvaluacion getPautaEvaluacion() {
        return pautaEvaluacion;
    }

    public void setPautaEvaluacion(PautaEvaluacion pautaEvaluacion) {
        this.pautaEvaluacion = pautaEvaluacion;
    }

    public TipoEvaluador getTipoEvaluador() {
        return tipoEvaluador;
    }

    public void setTipoEvaluador(TipoEvaluador tipoEvaluador) {
        this.tipoEvaluador = tipoEvaluador;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getPuntajeTotal() {
        return puntajeTotal;
    }

    public void setPuntajeTotal(BigDecimal puntajeTotal) {
        this.puntajeTotal = puntajeTotal;
    }

    public List<DetalleCriterio> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCriterio> detalles) {
        this.detalles = detalles;
    }

    // --- NUEVOS GETTERS Y SETTERS HU-06 ---
    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
