package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

/** N-03: datos que el coordinador escribe para una entrega del semestre. */
public class EntregaFormulario {
    private String nombre;
    private String ponderacion;

    public EntregaFormulario() {
    }

    public EntregaFormulario(String nombre, String ponderacion) {
        this.nombre = nombre;
        this.ponderacion = ponderacion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPonderacion() {
        return ponderacion;
    }

    public void setPonderacion(String ponderacion) {
        this.ponderacion = ponderacion;
    }
}
