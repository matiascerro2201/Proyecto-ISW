-- ============================================================
-- HU-01: elimina los datos creados por scripts/hu01-datos-prueba.sql
-- y cualquier pauta o evaluación que se haya agregado a esas asignaturas de prueba.
-- Se ejecuta a mano en MySQL Workbench. No toca ningún dato real del equipo.
-- Si Workbench responde con el error 1175 (safe update mode), desactiva
-- "Safe Updates" en Edit > Preferences > SQL Editor y vuelve a conectarte.
-- ============================================================
USE practicas_pedagogicas;

DELETE FROM detalle_criterio
WHERE id_evaluacion IN (SELECT id_evaluacion FROM evaluacion_aplicada WHERE id_linea = 9001);

DELETE FROM evaluacion_aplicada WHERE id_linea = 9001;
DELETE FROM linea_practica WHERE id_linea = 9001;
DELETE FROM inscripcion_asignatura WHERE id_inscripcion = 9001;

-- Las dimensiones y criterios se borran en cascada junto con su pauta
DELETE FROM pauta_evaluacion WHERE id_asignatura IN (9001, 9002);

DELETE FROM seccion_asignatura WHERE id_seccion IN (9001, 9002);
DELETE FROM asignatura WHERE id_asignatura IN (9001, 9002);

DELETE FROM profesor_asignatura WHERE id_usuario = 9001;
DELETE FROM usuario WHERE id_usuario = 9001;
