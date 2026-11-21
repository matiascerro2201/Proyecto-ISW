ALTER TABLE `planificacion`
  MODIFY COLUMN `contenido` text COLLATE utf8mb4_unicode_ci NULL,
  ADD COLUMN `archivo_nombre` varchar(100) COLLATE utf8mb4_unicode_ci NULL,
  ADD COLUMN `archivo_original` varchar(255) COLLATE utf8mb4_unicode_ci NULL,
  ADD COLUMN `archivo_tamano` bigint NULL,
  ADD COLUMN `archivo_fecha` datetime NULL,
  ADD COLUMN `texto_extraido` mediumtext COLLATE utf8mb4_unicode_ci NULL,
  ADD UNIQUE KEY `uq_planificacion_archivo` (`archivo_nombre`),
  ADD CONSTRAINT `ck_planificacion_contenido_o_archivo`
    CHECK (`contenido` IS NOT NULL OR `archivo_nombre` IS NOT NULL);