package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;

@Entity
@Table(name = "seccion_asignatura",
       uniqueConstraints = @UniqueConstraint(name = "uq_seccion", columnNames = {"id_asignatura", "anio", "semestre"}))
public class SeccionAsignatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seccion")
    private Long idSeccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_asignatura", nullable = false)
    private Asignatura asignatura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_profesor", nullable = false)
    private ProfesorAsignatura profesor;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "semestre", nullable = false)
    private Integer semestre;

    public SeccionAsignatura() {
    }

    public SeccionAsignatura(Asignatura asignatura, ProfesorAsignatura profesor, Integer anio, Integer semestre) {
        this.asignatura = asignatura;
        this.profesor = profesor;
        this.anio = anio;
        this.semestre = semestre;
    }

    public Long getIdSeccion() {
        return idSeccion;
    }

    public void setIdSeccion(Long idSeccion) {
        this.idSeccion = idSeccion;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public void setAsignatura(Asignatura asignatura) {
        this.asignatura = asignatura;
    }

    public ProfesorAsignatura getProfesor() {
        return profesor;
    }

    public void setProfesor(ProfesorAsignatura profesor) {
        this.profesor = profesor;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getSemestre() {
        return semestre;
    }

    public void setSemestre(Integer semestre) {
        this.semestre = semestre;
    }
}
