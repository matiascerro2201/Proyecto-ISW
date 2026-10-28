package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Aspecto en que se revisa una planificación (objetivos, evaluación, etc.). */
@Entity
@Table(name = "categoria_planificacion")
public class CategoriaPlanificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long idCategoria;

    /** Clave estable que usa el asistente en sus respuestas (por ejemplo, OBJETIVOS). */
    @Column(name = "codigo", nullable = false, length = 50)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    /** Qué se espera de este aspecto; forma parte de las instrucciones del asistente. */
    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;

    @Column(name = "orden", nullable = false)
    private int orden;

    /** Se desactiva en vez de borrarse, para no perder el historial de hallazgos. */
    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    public CategoriaPlanificacion() {
    }

    public CategoriaPlanificacion(String codigo, String nombre, String descripcion, int orden) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.orden = orden;
    }

    public Long getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Long idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
