package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "linea_practica",
       uniqueConstraints = @UniqueConstraint(name = "uq_linea", columnNames = {"id_inscripcion", "tipo"}))
public class LineaPractica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_linea")
    private Long idLinea;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_inscripcion", nullable = false)
    private InscripcionAsignatura inscripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_colaborador", nullable = false)
    private ProfesorColaborador colaborador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tutor")
    private ProfesorTutor tutor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoLinea tipo;

    @OneToMany(mappedBy = "linea", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluacionAplicada> evaluaciones = new ArrayList<>();

    @OneToMany(mappedBy = "linea", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Planificacion> planificaciones = new ArrayList<>();

    public LineaPractica() {
    }

    public LineaPractica(InscripcionAsignatura inscripcion, ProfesorColaborador colaborador, ProfesorTutor tutor, TipoLinea tipo) {
        this.inscripcion = inscripcion;
        this.colaborador = colaborador;
        this.tutor = tutor;
        this.tipo = tipo;
    }

    public Long getIdLinea() {
        return idLinea;
    }

    public void setIdLinea(Long idLinea) {
        this.idLinea = idLinea;
    }

    public InscripcionAsignatura getInscripcion() {
        return inscripcion;
    }

    public void setInscripcion(InscripcionAsignatura inscripcion) {
        this.inscripcion = inscripcion;
    }

    public ProfesorColaborador getColaborador() {
        return colaborador;
    }

    public void setColaborador(ProfesorColaborador colaborador) {
        this.colaborador = colaborador;
    }

    public ProfesorTutor getTutor() {
        return tutor;
    }

    public void setTutor(ProfesorTutor tutor) {
        this.tutor = tutor;
    }

    public TipoLinea getTipo() {
        return tipo;
    }

    public void setTipo(TipoLinea tipo) {
        this.tipo = tipo;
    }

    public List<EvaluacionAplicada> getEvaluaciones() {
        return evaluaciones;
    }

    public void setEvaluaciones(List<EvaluacionAplicada> evaluaciones) {
        this.evaluaciones = evaluaciones;
    }

    public List<Planificacion> getPlanificaciones() {
        return planificaciones;
    }

    public void setPlanificaciones(List<Planificacion> planificaciones) {
        this.planificaciones = planificaciones;
    }
}
