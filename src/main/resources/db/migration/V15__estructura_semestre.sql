-- Estructura de evaluación por semestre (HU N-02 a N-09 y siguientes).
--
-- La estructura (entregas y pautas con sus ponderaciones) pasa a definirse por SECCIÓN, es decir por
-- semestre, y la define el coordinador.
--
-- seccion_asignatura.iniciada / fecha_inicio: el coordinador "inicia" el semestre y desde ese momento la
--   estructura queda congelada (N-06).
--
-- pauta_evaluacion: las pautas del semestre son filas normales de esta tabla (así la evaluación del
--   colaborador y del tutor sigue funcionando igual), pero con id_seccion, evaluador (quién la aplica),
--   nombre y fecha_limite. Las pautas anteriores quedan con id_seccion NULL (estructura "antigua").
--
-- entrega_seccion: las entregas del semestre (nombre y ponderación). Reemplaza a pauta_documento en el
--   modelo nuevo, porque cada semestre puede tener las entregas que quiera y con el nombre que quiera.
--
-- entrega_estudiante: lo que el estudiante entregó en cada entrega (PDF) y la nota que recibió.
--   contenido_url puede ser NULL porque la nota 1,0 automática (N-11) existe aunque no haya archivo.

ALTER TABLE `seccion_asignatura`
  ADD COLUMN `iniciada` bit(1) NOT NULL DEFAULT b'0',
  ADD COLUMN `fecha_inicio` datetime DEFAULT NULL;

ALTER TABLE `pauta_evaluacion`
  ADD COLUMN `id_seccion` bigint DEFAULT NULL,
  ADD COLUMN `evaluador` enum('COLABORADOR','TUTOR') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `nombre` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `fecha_limite` date DEFAULT NULL,
  ADD KEY `fk_pauta_eval_seccion` (`id_seccion`),
  ADD CONSTRAINT `fk_pauta_eval_seccion` FOREIGN KEY (`id_seccion`) REFERENCES `seccion_asignatura` (`id_seccion`) ON DELETE CASCADE,
  ADD UNIQUE KEY `uq_pauta_seccion` (`id_seccion`,`evaluador`,`tipo_linea`,`numero_aplicacion`);

CREATE TABLE `entrega_seccion` (
  `id_entrega` bigint NOT NULL AUTO_INCREMENT,
  `id_seccion` bigint NOT NULL,
  `nombre` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ponderacion` decimal(5,2) NOT NULL,
  `habilitada` bit(1) NOT NULL DEFAULT b'1',
  `fecha_limite` date DEFAULT NULL,
  `orden` int NOT NULL DEFAULT 1,
  PRIMARY KEY (`id_entrega`),
  UNIQUE KEY `uq_entrega_nombre` (`id_seccion`,`nombre`),
  CONSTRAINT `fk_entrega_seccion` FOREIGN KEY (`id_seccion`) REFERENCES `seccion_asignatura` (`id_seccion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `entrega_estudiante` (
  `id_entrega_estudiante` bigint NOT NULL AUTO_INCREMENT,
  `id_inscripcion` bigint NOT NULL,
  `id_entrega` bigint NOT NULL,
  `contenido_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_subida` datetime DEFAULT NULL,
  `nota` decimal(3,1) DEFAULT NULL,
  `comentario` text COLLATE utf8mb4_unicode_ci,
  `nota_automatica` bit(1) NOT NULL DEFAULT b'0',
  `publicada` bit(1) NOT NULL DEFAULT b'0',
  `fecha_nota` datetime DEFAULT NULL,
  PRIMARY KEY (`id_entrega_estudiante`),
  UNIQUE KEY `uq_entrega_estudiante` (`id_inscripcion`,`id_entrega`),
  KEY `fk_entrega_est_entrega` (`id_entrega`),
  CONSTRAINT `fk_entrega_est_inscripcion` FOREIGN KEY (`id_inscripcion`) REFERENCES `inscripcion_asignatura` (`id_inscripcion`) ON DELETE CASCADE,
  CONSTRAINT `fk_entrega_est_entrega` FOREIGN KEY (`id_entrega`) REFERENCES `entrega_seccion` (`id_entrega`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
