-- HU-12: fecha límite que define el profesor de la asignatura para revisar los informes de una sección.
-- Es opcional (NULL = sin fecha definida) y es un día sin hora: vence al terminar ese día.
ALTER TABLE `seccion_asignatura` ADD COLUMN `fecha_limite_revision` date NULL;
