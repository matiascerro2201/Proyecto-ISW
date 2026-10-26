-- ============================================================
-- HU-01: datos de prueba para la definición de pautas.
--
-- Se ejecuta A MANO en MySQL Workbench (no lo carga la aplicación), para no meter
-- usuarios de prueba en la base de todo el equipo. No modifica data.sql.
-- Es idempotente (INSERT IGNORE con ids fijos 9001+), se puede ejecutar más de una vez.
-- Para quitarlo todo: scripts/hu01-limpiar-datos-prueba.sql
--
-- Requiere la base del equipo con: profesor1 (id 4), colaborador1 (id 3), Estudiante Tres (id 5)
-- y la inscripción 1 (de donde se toma el centro de práctica).
--
-- Qué crea y para qué sirve:
--   * Asignatura 9001 "Prueba HU-01 (profesor 1)", del profesor1:
--       - pauta 9001 (UNICA, aplicación 1): SIN evaluaciones -> se puede editar y eliminar.
--       - pauta 9002 (UNICA, aplicación 2): CON una evaluación -> solo lectura.
--   * Asignatura 9002 "Prueba HU-01 (profesor 2)", de otro profesor (profesor2@test.cl):
--       - pauta 9003: profesor1 NO debe poder verla (403 en /profesor/pautas/9003/editar).
--   * Una inscripción y una línea de práctica (Estudiante Tres con colaborador1) que dan
--     contexto a la evaluación de la pauta 9002, sin tocar las líneas ni los informes reales.
--   * profesor2@test.cl usa la misma clave que profesor1.
--
-- Nota: los ids fijos adelantan los contadores AUTO_INCREMENT de estas tablas; es solo estético.
-- ============================================================
USE practicas_pedagogicas;

-- Otro profesor de asignatura (copia la clave de profesor1)
INSERT IGNORE INTO usuario (id_usuario, nombre, email, password_hash, rol)
SELECT 9001, 'Profesor Asignatura Dos', 'profesor2@test.cl', password_hash, 'PROFESOR_ASIGNATURA'
FROM usuario WHERE id_usuario = 4;

INSERT IGNORE INTO profesor_asignatura (id_usuario) VALUES (9001);

-- Asignaturas y secciones de prueba
INSERT IGNORE INTO asignatura (id_asignatura, nombre, ciclo, numero_lineas) VALUES
    (9001, 'Prueba HU-01 (profesor 1)', 'FINAL', 1),
    (9002, 'Prueba HU-01 (profesor 2)', 'FINAL', 1);

INSERT IGNORE INTO seccion_asignatura (id_seccion, id_asignatura, id_profesor, anio, semestre) VALUES
    (9001, 9001, 4,    2026, 2),
    (9002, 9002, 9001, 2026, 2);

-- Una inscripción y una línea de práctica solo para poder registrar una evaluación
INSERT IGNORE INTO estudiante (id_usuario, carrera) VALUES (5, 'Pedagogía en Matemática');

INSERT IGNORE INTO inscripcion_asignatura (id_inscripcion, id_estudiante, id_seccion, id_centro, estado)
SELECT 9001, 5, 9001, id_centro, 'EN_CURSO'
FROM inscripcion_asignatura WHERE id_inscripcion = 1;

INSERT IGNORE INTO linea_practica (id_linea, id_inscripcion, id_colaborador, id_tutor, tipo) VALUES
    (9001, 9001, 3, NULL, 'UNICA');

-- Pautas: 9001 editable, 9002 con evaluaciones, 9003 del otro profesor
INSERT IGNORE INTO pauta_evaluacion (id_pauta_evaluacion, id_asignatura, tipo_linea, numero_aplicacion, ponderacion) VALUES
    (9001, 9001, 'UNICA', 1,  50.00),
    (9002, 9001, 'UNICA', 2,  50.00),
    (9003, 9002, 'UNICA', 1, 100.00);

INSERT IGNORE INTO dimension (id_dimension, id_pauta_evaluacion, nombre, orden) VALUES
    (9001, 9001, 'Dimensión de prueba', 1),
    (9002, 9002, 'Dimensión de prueba', 1),
    (9003, 9003, 'Dimensión de prueba', 1);

INSERT IGNORE INTO criterio (id_criterio, id_dimension, descripcion, puntaje_max, orden) VALUES
    (9001, 9001, 'Criterio de prueba', 5.00, 1),
    (9002, 9002, 'Criterio de prueba', 5.00, 1),
    (9003, 9003, 'Criterio de prueba', 5.00, 1);

-- Evaluación aplicada sobre la pauta 9002: la deja "en uso" (solo lectura)
INSERT IGNORE INTO evaluacion_aplicada (id_evaluacion, id_linea, id_pauta_evaluacion, tipo_evaluador, fecha, puntaje_total) VALUES
    (9001, 9001, 9002, 'COLABORADOR', '2026-10-20 10:00:00', 4.00);

INSERT IGNORE INTO detalle_criterio (id_detalle, id_evaluacion, id_criterio, puntaje_obtenido, transcripcion_comentario) VALUES
    (9001, 9001, 9002, 4.00, NULL);
