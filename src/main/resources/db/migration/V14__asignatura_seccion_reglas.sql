ALTER TABLE `asignatura`
  ADD UNIQUE KEY `uq_asignatura_nombre` (`nombre`),
  ADD CONSTRAINT `ck_asignatura_numero_lineas` CHECK (`numero_lineas` BETWEEN 1 AND 2);

ALTER TABLE `seccion_asignatura`
  ADD CONSTRAINT `ck_seccion_semestre` CHECK (`semestre` IN (1, 2));