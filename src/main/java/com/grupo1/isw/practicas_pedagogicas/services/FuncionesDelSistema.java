package com.grupo1.isw.practicas_pedagogicas.services;

import java.util.List;

import com.grupo1.isw.practicas_pedagogicas.dto.inicio.FuncionInicio;

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
        de("ESTUDIANTE", "Mis evaluaciones", "Mis evaluaciones",
                    "Revisa tus entregas, pautas y notas, y sube tus documentos desde la misma tabla.",
                    "/estudiante/evaluaciones"),
        de("ESTUDIANTE", "Mis informes finales", "Mis informes",
                    "Revisa tus informes finales publicados y cómo fuiste evaluado.", "/mis-informes"),

        de("COLABORADOR", "Mis estudiantes", "Mis estudiantes",
                    "Llena las pautas de evaluación de los estudiantes que acompañas.", "/colaborador/lineas"),

        de("COLABORADOR", "Mis pautas por aplicar", "Mis pautas",
                    "Mira las pautas que te tocan por estudiante, con su fecha límite y estado.",
                    "/evaluador/pautas"),

        de("TUTOR", "Mis estudiantes", "Mis estudiantes",
                    "Llena las pautas de evaluación de los estudiantes que supervisas.", "/tutor/lineas"),

        de("TUTOR", "Mis pautas por aplicar", "Mis pautas",
                    "Mira las pautas que te tocan por estudiante, con su fecha límite y estado.",
                    "/evaluador/pautas"),

        de("PROFESOR_ASIGNATURA", "Informes finales de desempeño", "Informes finales",
                    "Revisa, valida y publica los informes finales, y define la fecha límite de revisión.",
                    "/profesor/informes"),
        de("PROFESOR_ASIGNATURA", "Pautas de mi asignatura", "Pautas de mi asignatura",
                    "Crea y edita las pautas de evaluación de tus asignaturas.", "/profesor/pautas"),

        de("PROFESOR_ASIGNATURA", "Planificaciones pendientes de evaluación", "Planificaciones pendientes",
                    "Aprueba o rechaza las planificaciones que enviaron los estudiantes de tus secciones.",
                    "/planificaciones/pendientes"),

        de("PROFESOR_ASIGNATURA", "Estructura del semestre", "Estructura",
                    "Consulta las entregas y pautas del semestre y define sus fechas límite.",
                    "/profesor/estructura"),
        de("PROFESOR_ASIGNATURA", "Entregas del semestre", "Entregas",
                    "Revisa quién entregó, descarga los PDF, califica y publica las notas.",
                    "/profesor/entregas"),
        de("PROFESOR_ASIGNATURA", "Pautas de colaboradores y tutores", "Pautas del semestre",
                    "Revisa las pautas aplicadas y publícalas a cada estudiante.",
                    "/profesor/pautas-semestre"),
        de("PROFESOR_ASIGNATURA", "Informes de desempeño", "Desempeño",
                    "Genera el informe con la nota sugerida, fija la nota final y publícala.",
                    "/profesor/desempeno"),
                    
        de("PROFESOR_ASIGNATURA", "Informe del asistente de planificaciones", "Informe del asistente",
                    "Mira qué aspectos de las planificaciones les cuestan más a tus estudiantes.",
                    "/profesor/asistente"),

            // Coordinador de prácticas (HU-36 a HU-39). El orden es el de la barra superior.
        de("COORDINADOR", "Cursos", "Cursos",
                    "Recorre los cursos de cada semestre: sus pautas, sus estudiantes y cómo va cada uno.",
                    "/coordinador/cursos"),
        de("COORDINADOR", "Estudiantes", "Estudiantes",
                    "Busca a cualquier estudiante y revisa en qué cursos está y el estado de sus actividades.",
                    "/coordinador/estudiantes"),
        de("COORDINADOR", "Usuarios y roles", "Usuarios",
                    "Crea, edita y desactiva cuentas, y asigna el rol de cada usuario.",
                    "/coordinador/usuarios"),
        de("COORDINADOR", "Asignaturas", "Asignaturas",
                    "Crea y edita las asignaturas del programa de prácticas.",
                    "/coordinador/asignaturas"),
        de("COORDINADOR", "Centros de práctica", "Centros",
                    "Registra y edita los centros donde los estudiantes hacen su práctica.",
                    "/coordinador/centros"),
        de("COORDINADOR", "Secciones", "Secciones",
                    "Abre las secciones de cada semestre y asigna su profesor.",
                    "/coordinador/secciones")
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
