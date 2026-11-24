package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

import com.grupo1.isw.practicas_pedagogicas.dto.PautaFormulario.CriterioFormulario;
import com.grupo1.isw.practicas_pedagogicas.dto.PautaFormulario.DimensionFormulario;

import java.util.ArrayList;
import java.util.List;

/**
 * N-04: lo que el coordinador escribe al definir una pauta del semestre: sus datos básicos (la aplicación es
 * "1" (A) o "2" (B)) y su contenido, las dimensiones con sus criterios. Todo viaja como texto para que el servicio
 * pueda explicar cada error. Una lista de dimensiones vacía significa "sin contenido": al actualizar no toca el que
 * la pauta ya tiene.
 */
public class PautaSemestreFormulario {
    private String nombre;
    private String evaluador;
    private String tipoLinea;
    private String aplicacion = "1";
    private String ponderacion;
    private List<DimensionFormulario> dimensiones = new ArrayList<>();

    /** Formulario de una pauta nueva: parte con una dimensión que ya trae un criterio vacío. */
    public static PautaSemestreFormulario vacio() {
        PautaSemestreFormulario formulario = new PautaSemestreFormulario();
        DimensionFormulario dimension = new DimensionFormulario();
        dimension.getCriterios().add(new CriterioFormulario());
        formulario.dimensiones.add(dimension);
        return formulario;
    }

    public List<DimensionFormulario> getDimensiones() {
        return dimensiones;
    }

    public void setDimensiones(List<DimensionFormulario> dimensiones) {
        this.dimensiones = dimensiones;
    }

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
