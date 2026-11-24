package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

/** N-01: datos que el coordinador escribe al crear o editar una sección. */
public class SeccionFormulario {
    private Long idAsignatura;
    private Integer anio;
    private Integer semestre;
    private Long idProfesor;

    public Long getIdAsignatura() {
        return idAsignatura;
    }

    public void setIdAsignatura(Long idAsignatura) {
        this.idAsignatura = idAsignatura;
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

    public Long getIdProfesor() {
        return idProfesor;
    }

    public void setIdProfesor(Long idProfesor) {
        this.idProfesor = idProfesor;
    }
}
