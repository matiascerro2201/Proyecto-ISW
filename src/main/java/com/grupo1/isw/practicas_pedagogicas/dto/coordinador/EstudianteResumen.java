package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

/** Una fila de la lista de estudiantes del coordinador. */
public record EstudianteResumen(Long id, String nombre, String email, String carrera, boolean activo, long cursos) {
    public String enlace() {
        return "/coordinador/estudiantes/" + id;
    }
}
