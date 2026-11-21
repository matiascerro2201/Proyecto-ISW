package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

    @Column(name = "contenido", columnDefinition = "TEXT")
    private String contenido;

    // ENVIADA por defecto (igual que en la base de datos): el constructor con fecha de envío crea una
    // planificación ya enviada. Los borradores se crean con Planificacion.borrador(...).
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPlanificacion estado = EstadoPlanificacion.ENVIADA;

    @Column(name = "retroalimentacion", columnDefinition = "TEXT")
    private String retroalimentacion;

    // null mientras la planificación es un BORRADOR; la base de datos exige fecha en cualquier otro estado.
    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_retro")
    private LocalDateTime fechaRetro;

    @Column(name = "archivo_nombre", length = 100)
    private String archivoNombre;

    @Column(name = "archivo_original", length = 255)
    private String archivoOriginal;

    @Column(name = "archivo_tamano")
    private Long archivoTamano;

    @Column(name = "archivo_fecha")
    private LocalDateTime archivoFecha;

    @Column(name = "texto_extraido", columnDefinition = "MEDIUMTEXT")
    private String textoExtraido;

    @OneToMany(mappedBy = "planificacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InteraccionChatbot> interacciones = new ArrayList<>();

    // Historial de evaluaciones: es un registro, por eso no lleva orphanRemoval.
    @OneToMany(mappedBy = "planificacion", cascade = CascadeType.ALL)
    @OrderBy("fecha ASC, idEvaluacion ASC")
    private List<EvaluacionPlanificacion> evaluaciones = new ArrayList<>();

    public Planificacion() {
    }

    public Planificacion(LineaPractica linea, String contenido, LocalDateTime fechaEnvio) {
        this.linea = linea;
        this.contenido = contenido;
        this.fechaEnvio = fechaEnvio;
    }

    /** Crea un borrador: todavía no se envía al profesor, así que no tiene fecha de envío. */
    public static Planificacion borrador(LineaPractica linea, String contenido) {
        Planificacion borrador = new Planificacion();
        borrador.linea = linea;
        borrador.contenido = contenido;
        borrador.estado = EstadoPlanificacion.BORRADOR;
        return borrador;
    }

    public void adjuntarArchivo(String nombre, String original, long tamano, String textoExtraido,LocalDateTime fecha) {
        this.archivoNombre = nombre;
        this.archivoOriginal = original;
        this.archivoTamano = tamano;
        this.textoExtraido = textoExtraido;
        this.archivoFecha = fecha;
    }

    public boolean tieneArchivo() {
        return archivoNombre != null;
    }

    public String getArchivoNombre() { return archivoNombre; }
    public String getArchivoOriginal() { return archivoOriginal; }
    public Long getArchivoTamano() { return archivoTamano; }
    public LocalDateTime getArchivoFecha() { return archivoFecha; }
    public String getTextoExtraido() { return textoExtraido; }


    public void registrarEvaluacion(EstadoPlanificacion decision, String retroalimentacion, LocalDateTime fecha) {
        evaluaciones.add(new EvaluacionPlanificacion(this, decision, retroalimentacion, fecha));
    }

    /** Las evaluaciones en orden cronológico; la lista no se puede modificar desde fuera. */
    public List<EvaluacionPlanificacion> getEvaluaciones() {
        return Collections.unmodifiableList(evaluaciones);
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
