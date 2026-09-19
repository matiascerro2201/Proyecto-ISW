package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;

@Entity
@Table(name = "profesor_tutor")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class ProfesorTutor extends Usuario {

    public ProfesorTutor() {
        super();
    }
}
