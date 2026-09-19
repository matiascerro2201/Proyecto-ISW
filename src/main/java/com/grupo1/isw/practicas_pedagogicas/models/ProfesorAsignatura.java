package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;

@Entity
@Table(name = "profesor_asignatura")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class ProfesorAsignatura extends Usuario {

    public ProfesorAsignatura() {
        super();
    }
}
