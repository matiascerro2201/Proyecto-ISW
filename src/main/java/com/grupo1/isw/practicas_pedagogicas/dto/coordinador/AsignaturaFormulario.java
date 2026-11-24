package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;

/** N-01: datos que el coordinador escribe al crear o editar una asignatura. */
public class AsignaturaFormulario {
    private String nombre;
    private Ciclo ciclo;
    private Integer numeroLineas = 1;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Ciclo getCiclo() {
        return ciclo;
    }

    public void setCiclo(Ciclo ciclo) {
        this.ciclo = ciclo;
    }

    public Integer getNumeroLineas() {
        return numeroLineas;
    }

    public void setNumeroLineas(Integer numeroLineas) {
        this.numeroLineas = numeroLineas;
    }
}
