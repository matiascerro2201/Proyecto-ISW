package com.grupo1.isw.practicas_pedagogicas.dto.inicio;

import java.util.List;

public record CursoEstudiantes(String nombre, String periodo, List<EstudianteCurso> estudiantes) {
}
