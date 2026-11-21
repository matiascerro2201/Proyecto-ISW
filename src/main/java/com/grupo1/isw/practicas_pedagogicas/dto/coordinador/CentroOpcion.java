package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

/** Un centro de práctica para elegir en el formulario de un profesor colaborador. */
public record CentroOpcion(Long id, String nombre, String comuna) {
    public String etiqueta() {
        return comuna == null || comuna.isBlank() ? nombre : nombre + " (" + comuna + ")";
    }
}
