package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Un análisis redactado por el asistente para el profesor de una sección, con cuántos estudiantes y
 * planificaciones tenía en cuenta cuando se generó.
 */
@Entity
@Table(name = "informe_asistente")
public class InformeAsistente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_informe")
    private Long idInforme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_seccion", nullable = false)
    private SeccionAsignatura seccion;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "contenido", nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "num_estudiantes", nullable = false)
    private int numEstudiantes;

    @Column(name = "num_planificaciones", nullable = false)
    private int numPlanificaciones;

    public InformeAsistente() {
    }

    public InformeAsistente(SeccionAsignatura seccion, LocalDateTime fechaGeneracion, String contenido,
                            int numEstudiantes, int numPlanificaciones) {
        this.seccion = seccion;
        this.fechaGeneracion = fechaGeneracion;
        this.contenido = contenido;
        this.numEstudiantes = numEstudiantes;
        this.numPlanificaciones = numPlanificaciones;
    }

    public Long getIdInforme() {
        return idInforme;
    }

    public SeccionAsignatura getSeccion() {
        return seccion;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public String getContenido() {
        return contenido;
    }

    public int getNumEstudiantes() {
        return numEstudiantes;
    }

    public int getNumPlanificaciones() {
        return numPlanificaciones;
    }
}
