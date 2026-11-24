package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

/** N-04: datos básicos de una pauta del semestre. La aplicación es "1" (A) o "2" (B). */
public class PautaSemestreFormulario {
    private String nombre;
    private String evaluador;
    private String tipoLinea;
    private String aplicacion = "1";
    private String ponderacion;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEvaluador() {
        return evaluador;
    }

    public void setEvaluador(String evaluador) {
        this.evaluador = evaluador;
    }

    public String getTipoLinea() {
        return tipoLinea;
    }

    public void setTipoLinea(String tipoLinea) {
        this.tipoLinea = tipoLinea;
    }

    public String getAplicacion() {
        return aplicacion;
    }

    public void setAplicacion(String aplicacion) {
        this.aplicacion = aplicacion;
    }

    public String getPonderacion() {
        return ponderacion;
    }

    public void setPonderacion(String ponderacion) {
        this.ponderacion = ponderacion;
    }
}
