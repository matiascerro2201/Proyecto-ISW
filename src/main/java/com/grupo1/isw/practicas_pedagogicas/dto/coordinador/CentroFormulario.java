package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

import com.grupo1.isw.practicas_pedagogicas.models.TipoCentro;

/** Datos que el coordinador escribe al crear o editar un centro de práctica. */
public class CentroFormulario {
    private String nombre;
    private String comuna;
    private TipoCentro tipo;

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
