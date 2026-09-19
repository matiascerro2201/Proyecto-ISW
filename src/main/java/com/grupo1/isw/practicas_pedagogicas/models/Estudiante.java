package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;

@Entity
@Table(name = "estudiante")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Estudiante extends Usuario {

    @Column(name = "carrera", nullable = false, length = 150)
    private String carrera;

    public Estudiante() {
        super();
    }

    public String getCarrera() {
        return carrera;
    }

    public void setCarrera(String carrera) {
        this.carrera = carrera;
    }
}
