package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

import com.grupo1.isw.practicas_pedagogicas.models.TipoCentro;

/** Un centro de práctica en el listado del coordinador, con lo necesario para saber si puede borrarse. */
public record CentroFila(Long id, String nombre, String comuna, TipoCentro tipo, long colaboradores,
                         long inscripciones) {

    public String tipoEtiqueta() {
        return Etiquetas.tipoCentro(tipo);
    }

    public String comunaEtiqueta() {
        return comuna == null || comuna.isBlank() ? "—" : comuna;
    }

    /** Solo se puede borrar un centro que nadie usa: ni colaboradores ni inscripciones. */
    public boolean puedeEliminar() {
        return colaboradores == 0 && inscripciones == 0;
    }
}
