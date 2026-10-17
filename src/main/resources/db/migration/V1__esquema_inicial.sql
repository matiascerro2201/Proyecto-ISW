CREATE TABLE `asignatura` (
  `id_asignatura` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ciclo` enum('INICIAL','INTERMEDIO','FINAL') COLLATE utf8mb4_unicode_ci NOT NULL,
  `numero_lineas` tinyint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id_asignatura`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `centro_practica` (
  `id_centro` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `comuna` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tipo` enum('MUNICIPAL','SUBVENCIONADO','PARTICULAR','EDUCACION_ADULTOS') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_centro`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `usuario` (
  `id_usuario` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rol` enum('ESTUDIANTE','COLABORADOR','TUTOR','PROFESOR_ASIGNATURA','COORDINADOR') COLLATE utf8mb4_unicode_ci NOT NULL,
  `creado_en` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `estudiante` (
  `id_usuario` bigint NOT NULL,
  `carrera` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_usuario`),
  CONSTRAINT `fk_estudiante_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `pauta_documento` (
  `id_pauta_documento` bigint NOT NULL AUTO_INCREMENT,
  `id_asignatura` bigint NOT NULL,
  `ponderacion` decimal(5,2) NOT NULL,
  `tipo` enum('INFORME','PRODUCTO','PROYECTO_INTERVENCION') COLLATE utf8mb4_unicode_ci NOT NULL,
  `numero` tinyint NOT NULL,
  PRIMARY KEY (`id_pauta_documento`),
  UNIQUE KEY `uq_pauta_documento` (`id_asignatura`,`tipo`,`numero`),
  CONSTRAINT `fk_pauta_doc_asignatura` FOREIGN KEY (`id_asignatura`) REFERENCES `asignatura` (`id_asignatura`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `pauta_evaluacion` (
  `id_pauta_evaluacion` bigint NOT NULL AUTO_INCREMENT,
  `id_asignatura` bigint NOT NULL,
  `tipo_linea` enum('UNICA','ESPECIALIDAD','ORIENTACION') COLLATE utf8mb4_unicode_ci NOT NULL,
  `numero_aplicacion` tinyint NOT NULL DEFAULT '1',
  `ponderacion` decimal(5,2) NOT NULL,
  PRIMARY KEY (`id_pauta_evaluacion`),
  KEY `fk_pauta_eval_asignatura` (`id_asignatura`),
  CONSTRAINT `fk_pauta_eval_asignatura` FOREIGN KEY (`id_asignatura`) REFERENCES `asignatura` (`id_asignatura`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `profesor_asignatura` (
  `id_usuario` bigint NOT NULL,
  PRIMARY KEY (`id_usuario`),
  CONSTRAINT `fk_profasig_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `profesor_colaborador` (
  `id_usuario` bigint NOT NULL,
  `id_centro` bigint NOT NULL,
  PRIMARY KEY (`id_usuario`),
  KEY `fk_colaborador_centro` (`id_centro`),
  CONSTRAINT `fk_colaborador_centro` FOREIGN KEY (`id_centro`) REFERENCES `centro_practica` (`id_centro`),
  CONSTRAINT `fk_colaborador_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `profesor_tutor` (
  `id_usuario` bigint NOT NULL,
  PRIMARY KEY (`id_usuario`),
  CONSTRAINT `fk_tutor_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `dimension` (
  `id_dimension` bigint NOT NULL AUTO_INCREMENT,
  `id_pauta_evaluacion` bigint NOT NULL,
  `nombre` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `orden` tinyint NOT NULL,
  PRIMARY KEY (`id_dimension`),
  KEY `fk_dimension_pauta` (`id_pauta_evaluacion`),
  CONSTRAINT `fk_dimension_pauta` FOREIGN KEY (`id_pauta_evaluacion`) REFERENCES `pauta_evaluacion` (`id_pauta_evaluacion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `seccion_asignatura` (
  `id_seccion` bigint NOT NULL AUTO_INCREMENT,
  `id_asignatura` bigint NOT NULL,
  `id_profesor` bigint NOT NULL,
  `anio` smallint NOT NULL,
  `semestre` tinyint NOT NULL,
  PRIMARY KEY (`id_seccion`),
  UNIQUE KEY `uq_seccion` (`id_asignatura`,`anio`,`semestre`),
  KEY `fk_seccion_profesor` (`id_profesor`),
  CONSTRAINT `fk_seccion_asignatura` FOREIGN KEY (`id_asignatura`) REFERENCES `asignatura` (`id_asignatura`),
  CONSTRAINT `fk_seccion_profesor` FOREIGN KEY (`id_profesor`) REFERENCES `profesor_asignatura` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `criterio` (
  `id_criterio` bigint NOT NULL AUTO_INCREMENT,
  `id_dimension` bigint NOT NULL,
  `descripcion` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `puntaje_max` decimal(5,2) NOT NULL,
  `orden` tinyint NOT NULL,
  PRIMARY KEY (`id_criterio`),
  KEY `fk_criterio_dimension` (`id_dimension`),
  CONSTRAINT `fk_criterio_dimension` FOREIGN KEY (`id_dimension`) REFERENCES `dimension` (`id_dimension`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `inscripcion_asignatura` (
  `id_inscripcion` bigint NOT NULL AUTO_INCREMENT,
  `id_estudiante` bigint NOT NULL,
  `id_seccion` bigint NOT NULL,
  `id_centro` bigint NOT NULL,
  `estado` enum('EN_CURSO','APROBADO','REPROBADO','RETIRADO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EN_CURSO',
  PRIMARY KEY (`id_inscripcion`),
  UNIQUE KEY `uq_inscripcion` (`id_estudiante`,`id_seccion`),
  KEY `fk_inscripcion_seccion` (`id_seccion`),
  KEY `fk_inscripcion_centro` (`id_centro`),
  CONSTRAINT `fk_inscripcion_centro` FOREIGN KEY (`id_centro`) REFERENCES `centro_practica` (`id_centro`),
  CONSTRAINT `fk_inscripcion_estudiante` FOREIGN KEY (`id_estudiante`) REFERENCES `estudiante` (`id_usuario`),
  CONSTRAINT `fk_inscripcion_seccion` FOREIGN KEY (`id_seccion`) REFERENCES `seccion_asignatura` (`id_seccion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `documento_estudiante` (
  `id_documento` bigint NOT NULL AUTO_INCREMENT,
  `id_inscripcion` bigint NOT NULL,
  `id_pauta_documento` bigint NOT NULL,
  `contenido_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha_subida` datetime NOT NULL,
  `puntaje_obtenido` decimal(6,2) DEFAULT NULL,
  `comentarios_evaluador` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id_documento`),
  KEY `fk_documento_inscripcion` (`id_inscripcion`),
  KEY `fk_documento_pauta` (`id_pauta_documento`),
  CONSTRAINT `fk_documento_inscripcion` FOREIGN KEY (`id_inscripcion`) REFERENCES `inscripcion_asignatura` (`id_inscripcion`) ON DELETE CASCADE,
  CONSTRAINT `fk_documento_pauta` FOREIGN KEY (`id_pauta_documento`) REFERENCES `pauta_documento` (`id_pauta_documento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `informe_final` (
  `id_informe` bigint NOT NULL AUTO_INCREMENT,
  `id_inscripcion` bigint NOT NULL,
  `id_corrector` bigint NOT NULL,
  `calificacion_sugerida` decimal(3,1) DEFAULT NULL,
  `calificacion_final` decimal(3,1) DEFAULT NULL,
  `fecha_generacion` datetime DEFAULT NULL,
  `fecha_validacion` datetime DEFAULT NULL,
  `fecha_publicacion` datetime DEFAULT NULL,
  PRIMARY KEY (`id_informe`),
  UNIQUE KEY `id_inscripcion` (`id_inscripcion`),
  KEY `fk_informe_corrector` (`id_corrector`),
  CONSTRAINT `fk_informe_corrector` FOREIGN KEY (`id_corrector`) REFERENCES `profesor_asignatura` (`id_usuario`),
  CONSTRAINT `fk_informe_inscripcion` FOREIGN KEY (`id_inscripcion`) REFERENCES `inscripcion_asignatura` (`id_inscripcion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `linea_practica` (
  `id_linea` bigint NOT NULL AUTO_INCREMENT,
  `id_inscripcion` bigint NOT NULL,
  `id_colaborador` bigint NOT NULL,
  `id_tutor` bigint DEFAULT NULL,
  `tipo` enum('UNICA','ESPECIALIDAD','ORIENTACION') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_linea`),
  UNIQUE KEY `uq_linea` (`id_inscripcion`,`tipo`),
  KEY `fk_linea_colaborador` (`id_colaborador`),
  KEY `fk_linea_tutor` (`id_tutor`),
  CONSTRAINT `fk_linea_colaborador` FOREIGN KEY (`id_colaborador`) REFERENCES `profesor_colaborador` (`id_usuario`),
  CONSTRAINT `fk_linea_inscripcion` FOREIGN KEY (`id_inscripcion`) REFERENCES `inscripcion_asignatura` (`id_inscripcion`) ON DELETE CASCADE,
  CONSTRAINT `fk_linea_tutor` FOREIGN KEY (`id_tutor`) REFERENCES `profesor_tutor` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `evaluacion_aplicada` (
  `id_evaluacion` bigint NOT NULL AUTO_INCREMENT,
  `id_linea` bigint NOT NULL,
  `id_pauta_evaluacion` bigint NOT NULL,
  `tipo_evaluador` enum('COLABORADOR','TUTOR') COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha` datetime NOT NULL,
  `puntaje_total` decimal(6,2) DEFAULT NULL,
  PRIMARY KEY (`id_evaluacion`),
  KEY `fk_eval_linea` (`id_linea`),
  KEY `fk_eval_pauta` (`id_pauta_evaluacion`),
  CONSTRAINT `fk_eval_linea` FOREIGN KEY (`id_linea`) REFERENCES `linea_practica` (`id_linea`) ON DELETE CASCADE,
  CONSTRAINT `fk_eval_pauta` FOREIGN KEY (`id_pauta_evaluacion`) REFERENCES `pauta_evaluacion` (`id_pauta_evaluacion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `planificacion` (
  `id_planificacion` bigint NOT NULL AUTO_INCREMENT,
  `id_linea` bigint NOT NULL,
  `contenido` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `estado` enum('ENVIADA','APROBADA','RECHAZADA') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ENVIADA',
  `retroalimentacion` text COLLATE utf8mb4_unicode_ci,
  `fecha_envio` datetime NOT NULL,
  `fecha_retro` datetime DEFAULT NULL,
  PRIMARY KEY (`id_planificacion`),
  KEY `fk_planificacion_linea` (`id_linea`),
  CONSTRAINT `fk_planificacion_linea` FOREIGN KEY (`id_linea`) REFERENCES `linea_practica` (`id_linea`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `detalle_criterio` (
  `id_detalle` bigint NOT NULL AUTO_INCREMENT,
  `id_evaluacion` bigint NOT NULL,
  `id_criterio` bigint NOT NULL,
  `puntaje_obtenido` decimal(5,2) NOT NULL,
  `transcripcion_comentario` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id_detalle`),
  UNIQUE KEY `uq_detalle` (`id_evaluacion`,`id_criterio`),
  KEY `fk_detalle_criterio` (`id_criterio`),
  CONSTRAINT `fk_detalle_criterio` FOREIGN KEY (`id_criterio`) REFERENCES `criterio` (`id_criterio`),
  CONSTRAINT `fk_detalle_evaluacion` FOREIGN KEY (`id_evaluacion`) REFERENCES `evaluacion_aplicada` (`id_evaluacion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `interaccion_chatbot` (
  `id_interaccion` bigint NOT NULL AUTO_INCREMENT,
  `id_planificacion` bigint NOT NULL,
  `mensaje_estudiante` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `respuesta_bot` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha` datetime NOT NULL,
  PRIMARY KEY (`id_interaccion`),
  KEY `fk_interaccion_planificacion` (`id_planificacion`),
  CONSTRAINT `fk_interaccion_planificacion` FOREIGN KEY (`id_planificacion`) REFERENCES `planificacion` (`id_planificacion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
