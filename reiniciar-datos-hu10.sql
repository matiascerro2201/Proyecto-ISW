-- HU-10: deja la base como estaba antes de datos-prueba-hu10-v2.sql (borra informes, evaluaciones y entregas de prueba).
-- Solo desarrollo; ejecutar a mano para repetir la prueba desde cero.

USE practicas_pedagogicas;

DELETE FROM informe_final        WHERE id_inscripcion IN (1, 2);
DELETE FROM detalle_criterio     WHERE id_detalle BETWEEN 9001 AND 9012;
DELETE FROM evaluacion_aplicada  WHERE id_evaluacion BETWEEN 9001 AND 9004;
DELETE FROM documento_estudiante WHERE id_documento IN (9003, 9101, 9102, 9103);
UPDATE documento_estudiante SET puntaje_obtenido = NULL, comentarios_evaluador = NULL WHERE id_documento = 1;
UPDATE usuario SET password_hash = 'HASH_PENDIENTE_LOGIN' WHERE id_usuario = 3;
