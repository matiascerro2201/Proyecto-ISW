package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "planificacion")
public class Planificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_planificacion")
    private Long idPlanificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_linea", nullable = false)
    private LineaPractica linea;

    @Lob
    @Column(name = "contenido", nullable = false)
    private String contenido;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPlanificacion estado = EstadoPlanificacion.ENVIADA;

    @Lob
    @Column(name = "retroalimentacion")
    private String retroalimentacion;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_retro")
    private LocalDateTime fechaRetro;

    @OneToMany(mappedBy = "planificacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InteraccionChatbot> interacciones = new ArrayList<>();

    public Planificacion() {
    }

    public Planificacion(LineaPractica linea, String contenido, LocalDateTime fechaEnvio) {
        this.linea = linea;
        this.contenido = contenido;
        this.fechaEnvio = fechaEnvio;
    }

    public Long getIdPlanificacion() {
        return idPlanificacion;
    }

    public void setIdPlanificacion(Long idPlanificacion) {
        this.idPlanificacion = idPlanificacion;
    }

    public LineaPractica getLinea() {
        return linea;
    }

    public void setLinea(LineaPractica linea) {
        this.linea = linea;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public EstadoPlanificacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoPlanificacion estado) {
        this.estado = estado;
    }

    public String getRetroalimentacion() {
        return retroalimentacion;
    }

    public void setRetroalimentacion(String retroalimentacion) {
        this.retroalimentacion = retroalimentacion;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public LocalDateTime getFechaRetro() {
        return fechaRetro;
    }

    public void setFechaRetro(LocalDateTime fechaRetro) {
        this.fechaRetro = fechaRetro;
    }

    public List<InteraccionChatbot> getInteracciones() {
        return interacciones;
    }

    public void setInteracciones(List<InteraccionChatbot> interacciones) {
        this.interacciones = interacciones;
    }
}
