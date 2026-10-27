-- ------------------------------------------------------------
-- HU-08 / HU-09: datos de prueba de entregas de documentos
-- Requiere data.sql (asignatura 1 e inscripciones 1 y 2), por eso
-- debe cargarse DESPUES de data.sql.
-- Es idempotente (INSERT IGNORE con ids fijos), igual que data.sql.
-- ------------------------------------------------------------

-- 1. Entregas que exige la asignatura 1 (HU-09 es PROYECTO_INTERVENCION)
INSERT IGNORE INTO pauta_documento (id_pauta_documento, id_asignatura, ponderacion, tipo, numero) VALUES
    (1, 1, 30.00, 'INFORME',               1),
    (2, 1, 30.00, 'PRODUCTO',              1),
    (3, 1, 40.00, 'PROYECTO_INTERVENCION', 1);

-- 2. Documentos ya subidos por Camila (inscripcion 1).
--    contenido_url guarda SOLO el nombre generado (UUID), no la ruta:
--    la carpeta se define con app.uploads.dir.
--    Los PDF de ejemplo con estos nombres deben copiarse a esa carpeta.
--      id 1: INFORME 1, sin evaluar   -> se puede reemplazar
--      id 2: PRODUCTO 1, ya evaluado  -> NO se puede reemplazar
--    Diego (inscripcion 2) no tiene entregas: todo aparece pendiente.
INSERT IGNORE INTO documento_estudiante
    (id_documento, id_inscripcion, id_pauta_documento, contenido_url, fecha_subida, puntaje_obtenido, comentarios_evaluador) VALUES
    (1, 1, 1, '3eaf1b72-0dcd-49e5-8377-7f9d5c8ae1d2.pdf', '2026-10-05 10:15:00', NULL,  NULL),
    (2, 1, 2, '1af814e2-a159-4278-b425-2b02bc35914f.pdf', '2026-10-06 16:40:00', 85.00, 'Buen producto: objetivos claros y evidencia suficiente.');
