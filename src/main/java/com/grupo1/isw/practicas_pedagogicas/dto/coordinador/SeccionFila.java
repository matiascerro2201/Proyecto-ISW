package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

/** N-01: una sección en el listado del coordinador. */
public record SeccionFila(Long id, Long idAsignatura, String asignatura, int anio, int semestre,
                          Long idProfesor, String profesor, long inscritos) {

    public String periodo() {
        return "Semestre " + semestre + " de " + anio;
    }

    /** Solo se puede borrar una sección sin inscripciones. */
    public boolean puedeEliminar() {
        return inscritos == 0;
    }

    /** Asignatura, año y semestre solo cambian mientras nadie esté inscrito (el profesor sí, siempre). */
    public boolean puedeCambiarPeriodo() {
        return inscritos == 0;
    }
}
