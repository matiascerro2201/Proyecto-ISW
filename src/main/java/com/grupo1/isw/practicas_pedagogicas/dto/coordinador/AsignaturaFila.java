package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;

/** N-01: una asignatura en el listado del coordinador, con lo necesario para saber qué puede hacerse con ella. */
public record AsignaturaFila(Long id, String nombre, Ciclo ciclo, int numeroLineas, long secciones,
                             long inscripciones) {

    public String cicloEtiqueta() {
        return Etiquetas.ciclo(ciclo);
    }

    /** Solo se puede borrar una asignatura sin secciones. */
    public boolean puedeEliminar() {
        return secciones == 0;
    }

    /** El número de líneas solo cambia mientras nadie esté inscrito. */
    public boolean puedeCambiarLineas() {
        return inscripciones == 0;
    }
}
