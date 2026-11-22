package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

import java.util.List;

/** Un estudiante con todos los cursos en que está inscrito y el estado de sus actividades en cada uno. */
public record EstudianteDetalle(Long id, String nombre, String email, String carrera, boolean activo,
                                List<ActividadInscripcion> cursos) {
}
