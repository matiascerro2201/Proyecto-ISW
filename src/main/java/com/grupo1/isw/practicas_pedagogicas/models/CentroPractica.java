package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;
@Entity
@Table(name = "centro_practica")
public class CentroPractica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_centro")
    private Long idCentro;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "comuna", length = 100)
    private String comuna;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoCentro tipo;

    public CentroPractica() {
    }

    public CentroPractica(String nombre, String comuna, TipoCentro tipo) {
        this.nombre = nombre;
        this.comuna = comuna;
        this.tipo = tipo;
    }

    public Long getIdCentro() {
        return idCentro;
    }

    public void setIdCentro(Long idCentro) {
        this.idCentro = idCentro;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getComuna() {
        return comuna;
    }

    public void setComuna(String comuna) {
        this.comuna = comuna;
    }

    public TipoCentro getTipo() {
        return tipo;
    }

    public void setTipo(TipoCentro tipo) {
        this.tipo = tipo;
    }
}
