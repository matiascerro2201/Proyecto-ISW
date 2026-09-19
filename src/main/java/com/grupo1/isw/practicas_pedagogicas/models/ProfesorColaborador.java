package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;

@Entity
@Table(name = "profesor_colaborador")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class ProfesorColaborador extends Usuario {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_centro", nullable = false)
    private CentroPractica centro;

    public ProfesorColaborador() {
        super();
    }

    public CentroPractica getCentro() {
        return centro;
    }

    public void setCentro(CentroPractica centro) {
        this.centro = centro;
    }
}
