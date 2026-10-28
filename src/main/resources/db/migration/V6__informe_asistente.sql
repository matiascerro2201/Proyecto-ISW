-- HU-26: análisis del asistente que se le muestra al profesor de la asignatura.
--
-- El análisis lo redacta el asistente a partir de las cifras agregadas de una sección (asignatura, año y
-- semestre). Se guarda cada vez que se genera, para no volver a pedirlo al proveedor cada vez que el profesor
-- abre la pantalla (cada consulta gasta parte del cupo diario) y para conservar los análisis anteriores.
-- Con cada uno se guarda en cuántos estudiantes y planificaciones se basó, porque esos números cambian después.

CREATE TABLE `informe_asistente` (
  `id_informe` bigint NOT NULL AUTO_INCREMENT,
  `id_seccion` bigint NOT NULL,
  `fecha_generacion` datetime NOT NULL,
  `contenido` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `num_estudiantes` int NOT NULL,
  `num_planificaciones` int NOT NULL,
  PRIMARY KEY (`id_informe`),
  KEY `idx_informe_asistente_seccion_fecha` (`id_seccion`,`fecha_generacion`),
  CONSTRAINT `fk_informe_asistente_seccion` FOREIGN KEY (`id_seccion`) REFERENCES `seccion_asignatura` (`id_seccion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
