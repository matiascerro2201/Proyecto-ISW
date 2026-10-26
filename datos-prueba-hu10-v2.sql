-- HU-10: datos de prueba con Camila y Diego completos. Solo desarrollo; ejecutar a mano tras arrancar la app una vez.
-- Idempotente (INSERT IGNORE, ids altos). Esperado: Camila 5,8 y Diego 4,4 (profesor: andres.fuentes@uni.cl / tutor123).

USE practicas_pedagogicas;
SET NAMES utf8mb4;

-- 0. Verificación previa (esperado: 6, 1, 3, 2, 3, 2); si no coincide, falta arrancar la app o cargar data.sql.
SELECT 'usuarios 1 a 6' AS que, COUNT(*) AS filas FROM usuario WHERE id_usuario BETWEEN 1 AND 6
UNION ALL
SELECT 'pauta_evaluacion 901', COUNT(*) FROM pauta_evaluacion WHERE id_pauta_evaluacion = 901
UNION ALL
SELECT 'criterios 901 a 903', COUNT(*) FROM criterio WHERE id_criterio BETWEEN 901 AND 903
UNION ALL
SELECT 'lineas 1 y 3 con tutor 6', COUNT(*) FROM linea_practica WHERE id_linea IN (1, 3) AND id_tutor = 6
UNION ALL
SELECT 'pauta_documento 1 a 3', COUNT(*) FROM pauta_documento WHERE id_pauta_documento BETWEEN 1 AND 3
UNION ALL
SELECT 'documentos 1 y 2 de Camila', COUNT(*) FROM documento_estudiante WHERE id_documento IN (1, 2);

-- 1. Contraseña de prueba del profesor de asignatura (mismo hash de desarrollo que Marcela: tutor123).
UPDATE usuario
    SET password_hash = '$2a$10$jmRiGo4/n0AM.hxk5868xOHzBAunuuw9rJXRANULX7LBXF7mMOaxq'
    WHERE id_usuario = 3 AND password_hash = 'HASH_PENDIENTE_LOGIN';

-- 2. Evaluaciones de la pauta 901 por línea (1 = Camila, 3 = Diego), del colaborador y del tutor.
INSERT IGNORE INTO evaluacion_aplicada
    (id_evaluacion, id_linea, id_pauta_evaluacion, tipo_evaluador, fecha, puntaje_total) VALUES
    (9001, 1, 901, 'COLABORADOR', '2026-10-08 10:00:00', 9.00),
    (9002, 1, 901, 'TUTOR',       '2026-10-09 11:00:00', 8.00),
    (9003, 3, 901, 'COLABORADOR', '2026-10-08 12:00:00', 6.50),
    (9004, 3, 901, 'TUTOR',       '2026-10-09 13:00:00', 6.50);

INSERT IGNORE INTO detalle_criterio
    (id_detalle, id_evaluacion, id_criterio, puntaje_obtenido, transcripcion_comentario) VALUES
    (9001, 9001, 901, 3.50, 'Los objetivos de sus clases son claros y medibles.'),
    (9002, 9001, 902, 2.50, 'Recursos pertinentes, aunque repite el mismo formato.'),
    (9003, 9001, 903, 3.00, 'Excelente clima de aula.'),
    (9004, 9002, 901, 3.00, 'Falta explicitar los indicadores de logro.'),
    (9005, 9002, 902, 2.50, NULL),
    (9006, 9002, 903, 2.50, 'Debe mejorar la participación de los estudiantes más callados.'),
    (9007, 9003, 901, 2.50, 'Los objetivos son correctos pero poco específicos.'),
    (9008, 9003, 902, 2.00, 'Usa pocos recursos distintos.'),
    (9009, 9003, 903, 2.00, 'Le cuesta manejar el ruido en la sala.'),
    (9010, 9004, 901, 2.00, 'Debe alinear mejor los objetivos con las actividades.'),
    (9011, 9004, 902, 2.00, NULL),
    (9012, 9004, 903, 2.50, 'Mejoró el clima de aula durante las últimas clases.');

-- 3. Entregas con puntaje sobre 100 (Camila: informe 1 evaluado y proyecto nuevo; Diego: las tres).
UPDATE documento_estudiante
    SET puntaje_obtenido = 70.00, comentarios_evaluador = 'Falta profundizar en el marco teórico.'
    WHERE id_documento = 1 AND puntaje_obtenido IS NULL;

INSERT IGNORE INTO documento_estudiante
    (id_documento, id_inscripcion, id_pauta_documento, contenido_url, fecha_subida,
     puntaje_obtenido, comentarios_evaluador) VALUES
    (9003, 1, 3, 'demo-proyecto-intervencion.pdf', '2026-10-07 09:00:00',
     90.00, 'Proyecto sólido y bien fundamentado.'),
    (9101, 2, 1, 'demo-diego-informe-1.pdf',       '2026-10-05 10:00:00',
     60.00, 'Cumple lo mínimo; falta análisis de los resultados.'),
    (9102, 2, 2, 'demo-diego-producto-1.pdf',      '2026-10-06 11:00:00',
     65.00, NULL),
    (9103, 2, 3, 'demo-diego-proyecto-intervencion.pdf', '2026-10-07 12:00:00',
     70.00, 'Buena propuesta, pero la evaluación del impacto es débil.');

-- 4. Verificación: 4 evaluaciones de 3 criterios (9,00 / 8,00 / 6,50 / 6,50) y entregas 70/85/90 y 60/65/70.
SELECT l.id_linea, est.nombre AS estudiante, e.tipo_evaluador, e.puntaje_total, COUNT(d.id_detalle) AS criterios
    FROM evaluacion_aplicada e
    JOIN linea_practica l ON l.id_linea = e.id_linea
    JOIN inscripcion_asignatura i ON i.id_inscripcion = l.id_inscripcion
    JOIN usuario est ON est.id_usuario = i.id_estudiante
    LEFT JOIN detalle_criterio d ON d.id_evaluacion = e.id_evaluacion
    WHERE e.id_pauta_evaluacion = 901
    GROUP BY l.id_linea, est.nombre, e.id_evaluacion, e.tipo_evaluador, e.puntaje_total
    ORDER BY l.id_linea, e.tipo_evaluador;

SELECT est.nombre AS estudiante, pd.tipo, pd.numero, pd.ponderacion, de.puntaje_obtenido
    FROM inscripcion_asignatura i
    JOIN usuario est ON est.id_usuario = i.id_estudiante
    JOIN pauta_documento pd ON pd.id_asignatura = 1
    LEFT JOIN documento_estudiante de
           ON de.id_pauta_documento = pd.id_pauta_documento AND de.id_inscripcion = i.id_inscripcion
    WHERE i.id_inscripcion IN (1, 2)
    ORDER BY i.id_inscripcion, pd.tipo, pd.numero;

-- 5. Tras generar los informes: calificacion_sugerida 5.8 y 4.4; nota final, validación y publicación en NULL (HU-11).
SELECT id_informe, id_inscripcion, id_corrector, calificacion_sugerida,
       calificacion_final, fecha_generacion, fecha_validacion, fecha_publicacion
    FROM informe_final
    WHERE id_inscripcion IN (1, 2)
    ORDER BY id_inscripcion;

-- Caso "faltan datos": quitar la evaluación del tutor de Diego (sus detalles se borran en cascada); este script la restaura.
-- DELETE FROM evaluacion_aplicada WHERE id_evaluacion = 9004;
