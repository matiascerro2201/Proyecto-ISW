-- ------------------------------------------------------------
-- 1. Usuarios 
-- ------------------------------------------------------------
INSERT IGNORE INTO usuario (id_usuario, nombre, email, password_hash, rol) VALUES
    (1, 'Carlos Rojas',      'carlos.rojas@liceo.cl',     'HASH_PENDIENTE_LOGIN', 'COLABORADOR'),
    (2, 'Patricia Molina',   'patricia.molina@liceo.cl',  'HASH_PENDIENTE_LOGIN', 'COLABORADOR'),
    (3, 'Andrés Fuentes',    'andres.fuentes@uni.cl',     'HASH_PENDIENTE_LOGIN', 'PROFESOR_ASIGNATURA'),
    (4, 'Camila Soto',       'camila.soto@uni.cl',        'HASH_PENDIENTE_LOGIN', 'ESTUDIANTE'),
    (5, 'Diego Pérez',       'diego.perez@uni.cl',        'HASH_PENDIENTE_LOGIN', 'ESTUDIANTE');

-- ------------------------------------------------------------
-- 2. Centro de práctica
-- ------------------------------------------------------------
INSERT IGNORE INTO centro_practica (id_centro, nombre, comuna, tipo) VALUES
    (1, 'Liceo Los Aromos', 'La Florida', 'MUNICIPAL');

-- ------------------------------------------------------------
-- 3. Subtablas de roles 
-- ------------------------------------------------------------
INSERT IGNORE INTO estudiante (id_usuario, carrera) VALUES
    (4, 'Pedagogía en Matemática'),
    (5, 'Pedagogía en Matemática');

INSERT IGNORE INTO profesor_colaborador (id_usuario, id_centro) VALUES
    (1, 1),
    (2, 1);

INSERT IGNORE INTO profesor_asignatura (id_usuario) VALUES
    (3);

-- ------------------------------------------------------------
-- 4. Asignatura y sección 
-- ------------------------------------------------------------
INSERT IGNORE INTO asignatura (id_asignatura, nombre, ciclo, numero_lineas) VALUES
    (1, 'Práctica Pedagógica', 'FINAL', 2);

INSERT IGNORE INTO seccion_asignatura (id_seccion, id_asignatura, id_profesor, anio, semestre) VALUES
    (1, 1, 3, 2026, 2);

-- ------------------------------------------------------------
-- 5. Inscripciones y líneas de práctica
--    Camila (inscripción 1): 2 líneas, cada una con distinto colaborador
--    Diego  (inscripción 2): 1 línea con el colaborador 1
-- ------------------------------------------------------------
INSERT IGNORE INTO inscripcion_asignatura (id_inscripcion, id_estudiante, id_seccion, id_centro, estado) VALUES
    (1, 4, 1, 1, 'EN_CURSO'),
    (2, 5, 1, 1, 'EN_CURSO');

INSERT IGNORE INTO linea_practica (id_linea, id_inscripcion, id_colaborador, id_tutor, tipo) VALUES
    (1, 1, 1, NULL, 'ESPECIALIDAD'),
    (2, 1, 2, NULL, 'ORIENTACION'),
    (3, 2, 1, NULL, 'ESPECIALIDAD');

-- ------------------------------------------------------------
-- 6. Planificaciones en distintos estados
-- ------------------------------------------------------------
INSERT IGNORE INTO planificacion (id_planificacion, id_linea, contenido, estado, retroalimentacion, fecha_envio, fecha_retro) VALUES
    (1, 1, 'Clase 2: Funciones lineales. Objetivo: que los estudiantes identifiquen pendiente e intercepto a partir de una tabla de valores. Inicio: problema de contexto sobre tarifas de taxi. Desarrollo: trabajo en parejas con tablas y gráficos. Cierre: ticket de salida con dos preguntas.',
        'ENVIADA', NULL, '2026-09-14 10:30:00', NULL),
    (2, 1, 'Clase 3: Ecuación de la recta. Objetivo: escribir la ecuación de una recta dados dos puntos. Inicio: repaso de pendiente. Desarrollo: ejemplos guiados y ejercicios graduados. Cierre: revisión colectiva de errores frecuentes.',
        'ENVIADA', NULL, '2026-09-16 09:00:00', NULL),
    (3, 3, 'Clase 1: Introducción a los números racionales. Objetivo: ubicar fracciones en la recta numérica. Inicio: material concreto con tiras de papel. Desarrollo: actividad de ubicación y comparación. Cierre: síntesis oral.',
        'ENVIADA', NULL, '2026-09-15 11:45:00', NULL),

    (4, 1, 'Clase 1: Repaso de álgebra básica. Objetivo: reforzar lenguaje algebraico. Actividades graduadas con retroalimentación inmediata.',
        'APROBADA', 'Excelente planificación: objetivos claros y actividades bien secuenciadas.', '2026-09-07 08:00:00', '2026-09-08 15:20:00'),
    (5, 3, 'Clase 0: Presentación del curso y diagnóstico inicial.',
        'RECHAZADA', 'Faltan los objetivos de aprendizaje y los criterios de evaluación de la clase.', '2026-09-10 09:30:00', '2026-09-11 10:10:00'),

    (6, 2, 'Clase 1: Orientación vocacional y proyecto de vida. Objetivo: que los estudiantes reflexionen sobre sus intereses. Dinámica grupal y cierre reflexivo.',
        'ENVIADA', NULL, '2026-09-12 14:00:00', NULL);


-- ------------------------------------------------------------
-- 7. HU-03: profesor supervisor (tutor) y una pauta para probarla
--    Login de prueba:  marcela.vidal@uni.cl  /  tutor123   (solo desarrollo)
--    Marcela supervisa las líneas 1 (Camila) y 3 (Diego), ambas ESPECIALIDAD.
--    La línea 2 queda sin tutor para probar el control de acceso (403).
-- ------------------------------------------------------------
INSERT IGNORE INTO usuario (id_usuario, nombre, email, password_hash, rol) VALUES
    (6, 'Marcela Vidal', 'marcela.vidal@uni.cl', '$2a$10$jmRiGo4/n0AM.hxk5868xOHzBAunuuw9rJXRANULX7LBXF7mMOaxq', 'TUTOR');

-- Si el tutor ya existía con el hash pendiente, se le asigna la contraseña de prueba.
UPDATE usuario SET password_hash = '$2a$10$jmRiGo4/n0AM.hxk5868xOHzBAunuuw9rJXRANULX7LBXF7mMOaxq'
    WHERE id_usuario = 6 AND password_hash = 'HASH_PENDIENTE_LOGIN';

INSERT IGNORE INTO profesor_tutor (id_usuario) VALUES
    (6);

UPDATE linea_practica SET id_tutor = 6 WHERE id_linea IN (1, 3) AND id_tutor IS NULL;

-- Pauta mínima para líneas ESPECIALIDAD: 2 dimensiones, 3 criterios (máximo total 10,00).
-- Ids altos (901+) para no chocar con pautas que otros integrantes hayan cargado.
INSERT IGNORE INTO pauta_evaluacion (id_pauta_evaluacion, id_asignatura, tipo_linea, numero_aplicacion, ponderacion) VALUES
    (901, 1, 'ESPECIALIDAD', 1, 50.00);

INSERT IGNORE INTO dimension (id_dimension, id_pauta_evaluacion, nombre, orden) VALUES
    (901, 901, 'Planificación y preparación', 1),
    (902, 901, 'Gestión de aula', 2);

INSERT IGNORE INTO criterio (id_criterio, id_dimension, descripcion, puntaje_max, orden) VALUES
    (901, 901, 'Los objetivos de la clase están claramente definidos', 4.00, 1),
    (902, 901, 'Selecciona recursos pertinentes para el aprendizaje', 3.00, 2),
    (903, 902, 'Mantiene un clima de respeto y participación', 3.00, 1);
