package com.grupo1.isw.practicas_pedagogicas.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * HU-01: lo que el profesor escribe al definir una pauta completa (datos generales, dimensiones y criterios).
 *
 * <p>Todos los campos son texto a propósito: así el servicio puede validar y explicar cada error en vez de
 * fallar al convertir el número. Las filas se agregan y se quitan con botones que reenvían el formulario
 * ({@link #aplicarAccion(String)}), sin JavaScript.
 */
public class PautaFormulario {
    private String tipoLinea = "";
    private String numeroAplicacion = "1";
    private String ponderacion = "";
    private List<DimensionFormulario> dimensiones = new ArrayList<>();

    /** Formulario de una pauta nueva: parte con una dimensión que ya trae un criterio vacío. */
    public static PautaFormulario vacio() {
        PautaFormulario formulario = new PautaFormulario();
        formulario.agregarDimension();
        return formulario;
    }

    public String getTipoLinea() {
        return tipoLinea;
    }

    public void setTipoLinea(String tipoLinea) {
        this.tipoLinea = tipoLinea;
    }

    public String getNumeroAplicacion() {
        return numeroAplicacion;
    }

    public void setNumeroAplicacion(String numeroAplicacion) {
        this.numeroAplicacion = numeroAplicacion;
    }

    public String getPonderacion() {
        return ponderacion;
    }

    public void setPonderacion(String ponderacion) {
        this.ponderacion = ponderacion;
    }

    public List<DimensionFormulario> getDimensiones() {
        return dimensiones;
    }

    public void setDimensiones(List<DimensionFormulario> dimensiones) {
        this.dimensiones = dimensiones;
    }

    /**
     * Aplica la acción del botón que se presionó.
     *
     * @param accion {@code guardar}, {@code agregarDimension}, {@code quitarDimension-i},
     *               {@code agregarCriterio-i} o {@code quitarCriterio-i-j} (índices desde 0)
     * @return true si hay que guardar la pauta; false si solo se cambiaron filas y hay que volver a mostrar
     *         el formulario. Una acción desconocida o con índices fuera de rango no cambia nada.
     */
    public boolean aplicarAccion(String accion) {
        if (accion == null || accion.equals("guardar")) {
            return true;
        }
        String[] partes = accion.split("-");
        try {
            switch (partes[0]) {
                case "agregarDimension" -> agregarDimension();
                case "quitarDimension" -> quitarDimension(Integer.parseInt(partes[1]));
                case "agregarCriterio" -> agregarCriterio(Integer.parseInt(partes[1]));
                case "quitarCriterio" -> quitarCriterio(Integer.parseInt(partes[1]), Integer.parseInt(partes[2]));
                default -> { /* acción desconocida: no se cambia nada */ }
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            // acción mal formada: se ignora y se vuelve a mostrar el formulario
        }
        return false;
    }

    public void agregarDimension() {
        DimensionFormulario dimension = new DimensionFormulario();
        dimension.getCriterios().add(new CriterioFormulario());
        dimensiones.add(dimension);
    }

    public void quitarDimension(int indice) {
        if (indice >= 0 && indice < dimensiones.size()) {
            dimensiones.remove(indice);
        }
    }

    public void agregarCriterio(int indiceDimension) {
        if (indiceDimension >= 0 && indiceDimension < dimensiones.size()) {
            dimensiones.get(indiceDimension).getCriterios().add(new CriterioFormulario());
        }
    }

    public void quitarCriterio(int indiceDimension, int indiceCriterio) {
        if (indiceDimension < 0 || indiceDimension >= dimensiones.size()) {
            return;
        }
        List<CriterioFormulario> criterios = dimensiones.get(indiceDimension).getCriterios();
        if (indiceCriterio >= 0 && indiceCriterio < criterios.size()) {
            criterios.remove(indiceCriterio);
        }
    }

    public static class DimensionFormulario {
        private String nombre = "";
        private List<CriterioFormulario> criterios = new ArrayList<>();

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public List<CriterioFormulario> getCriterios() {
            return criterios;
        }

        public void setCriterios(List<CriterioFormulario> criterios) {
            this.criterios = criterios;
        }
    }

    public static class CriterioFormulario {
        private String descripcion = "";
        private String puntajeMax = "";

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getPuntajeMax() {
            return puntajeMax;
        }

        public void setPuntajeMax(String puntajeMax) {
            this.puntajeMax = puntajeMax;
        }
    }
}
