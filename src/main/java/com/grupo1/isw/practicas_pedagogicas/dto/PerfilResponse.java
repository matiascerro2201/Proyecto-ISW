package com.grupo1.isw.practicas_pedagogicas.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Datos de la página "Mi perfil": la cuenta del usuario y sus cursos actuales y pasados. */
public record PerfilResponse(
        String nombre,
        String email,
        String rol,
        /** Solo estudiantes; null en los demás roles. */
        String carrera,
        /** Solo profesores colaboradores; null en los demás roles. */
        String centro,
        LocalDateTime creadoEn,
        boolean activo,
        /** El coordinador no tiene cursos asociados: la vista omite esas secciones. */
        boolean tieneCursos,
        /** Título de la columna "detalle" de las tablas de cursos, según el rol. */
        String etiquetaDetalle,
        List<Curso> cursosActuales,
        List<Curso> cursosPasados
) {
    /**
     * @param detalle según el rol: centro de práctica (estudiante), estudiante a cargo (colaborador y tutor)
     *                o cantidad de estudiantes de la sección (profesor de asignatura)
     */
    public record Curso(String asignatura, int anio, int semestre, String estado, String detalle) {
        public String periodo() {
            return anio + "-" + semestre;
        }
    }
}
