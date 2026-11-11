package com.grupo1.isw.practicas_pedagogicas.dto.inicio;

import java.util.List;

public record EstudianteCurso(Long idInscripcion, String nombre, String estado, String claseEstado,
                              List<AccionEstudiante> acciones) {
}
