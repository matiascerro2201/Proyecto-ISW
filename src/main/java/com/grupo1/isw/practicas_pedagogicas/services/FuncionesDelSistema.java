package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.inicio.FuncionInicio;

import java.util.List;

/** Funciones de cada rol. La barra y el inicio leen esta lista. */
public final class FuncionesDelSistema {
    private record Entrada(String rol, FuncionInicio funcion) {
    }

    private static Entrada de(String rol, String titulo, String etiqueta, String descripcion, String enlace) {
        return new Entrada(rol, new FuncionInicio(titulo, etiqueta, descripcion, enlace));
    }

    private static final List<Entrada> TODAS = List.of(
            de("ESTUDIANTE", "Pautas de evaluación", "Pautas de evaluación",
                    "Consulta con qué criterios serás evaluado en cada asignatura.", "/mis-pautas"),
            de("ESTUDIANTE", "Mis planificaciones de clase", "Mis planificaciones",
                    "Escribe y envía tus planificaciones, y corrige las que tienen observaciones.",
                    "/estudiante/planificaciones"),
            de("ESTUDIANTE", "Mis entregas", "Mis entregas",
                    "Sube los documentos de tu práctica y revisa su estado.", "/estudiante/documentos"),
            de("ESTUDIANTE", "Mis informes finales", "Mis informes",
                    "Revisa tus informes finales publicados y cómo fuiste evaluado.", "/mis-informes"),

            de("COLABORADOR", "Mis estudiantes", "Mis estudiantes",
                    "Llena las pautas de evaluación de los estudiantes que acompañas.", "/colaborador/lineas"),
            de("COLABORADOR", "Planificaciones pendientes de evaluación", "Planificaciones pendientes",
                    "Aprueba o rechaza las planificaciones que enviaron tus estudiantes.",
                    "/planificaciones/pendientes"),

            de("TUTOR", "Estudiantes que superviso", "Estudiantes que superviso",
                    "Llena las pautas de evaluación de los estudiantes que supervisas.", "/tutor/lineas"),

            de("PROFESOR_ASIGNATURA", "Informes finales de desempeño", "Informes finales",
                    "Revisa, valida y publica los informes finales, y define la fecha límite de revisión.",
                    "/profesor/informes"),
            de("PROFESOR_ASIGNATURA", "Pautas de mi asignatura", "Pautas de mi asignatura",
                    "Crea y edita las pautas de evaluación de tus asignaturas.", "/profesor/pautas"),
            de("PROFESOR_ASIGNATURA", "Informe del asistente de planificaciones", "Informe del asistente",
                    "Mira qué aspectos de las planificaciones les cuestan más a tus estudiantes.",
                    "/profesor/asistente")
    );

    private FuncionesDelSistema() {
    }

    public static List<FuncionInicio> paraRol(String rol) {
        return TODAS.stream()
                .filter(entrada -> entrada.rol().equals(rol))
                .map(Entrada::funcion)
                .toList();
    }
}
