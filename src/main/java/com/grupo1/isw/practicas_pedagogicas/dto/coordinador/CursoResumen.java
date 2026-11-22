package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;

/** Un curso (sección de una asignatura en un semestre) en la lista de un semestre. */
public record CursoResumen(Long idSeccion, String nombre, Ciclo ciclo, String profesor,
                           long estudiantes, long informesPublicados) {
    public String cicloEtiqueta() {
        return Etiquetas.ciclo(ciclo);
    }

    public String enlace() {
        return "/coordinador/cursos/seccion/" + idSeccion;
    }
}
