-- HU-26: lo que el asistente detecta en cada conversación, para que los profesores de la asignatura
-- puedan ver los aspectos más débiles de sus estudiantes sin leer cada chat.
--
-- categoria_planificacion: aspectos en que se revisa una planificación. Es una tabla (y no un enum)
--   para que el profesor pueda ajustarla. Se desactiva en vez de borrarse, para conservar el historial.
--   LAS CATEGORIAS DE ESTA MIGRACION SON PROVISIONALES: se reemplazarán con la pauta del profesor
--   mediante una migración nueva (no se edita este archivo una vez aplicado).
-- hallazgo_chatbot: cada hallazgo que el asistente reporta junto con su respuesta: una categoría,
--   un nivel (DEBIL o ADECUADO) y una observación breve, sin datos personales. Como máximo uno por
--   categoría en cada interacción.

CREATE TABLE `categoria_planificacion` (
  `id_categoria` bigint NOT NULL AUTO_INCREMENT,
  `codigo` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `orden` int NOT NULL DEFAULT 0,
  `activa` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id_categoria`),
  UNIQUE KEY `uq_categoria_codigo` (`codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `hallazgo_chatbot` (
  `id_hallazgo` bigint NOT NULL AUTO_INCREMENT,
  `id_interaccion` bigint NOT NULL,
  `id_categoria` bigint NOT NULL,
  `nivel` enum('DEBIL','ADECUADO') COLLATE utf8mb4_unicode_ci NOT NULL,
  `observacion` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_hallazgo`),
  UNIQUE KEY `uq_hallazgo_interaccion_categoria` (`id_interaccion`,`id_categoria`),
  KEY `fk_hallazgo_categoria` (`id_categoria`),
  CONSTRAINT `fk_hallazgo_interaccion` FOREIGN KEY (`id_interaccion`) REFERENCES `interaccion_chatbot` (`id_interaccion`) ON DELETE CASCADE,
  CONSTRAINT `fk_hallazgo_categoria` FOREIGN KEY (`id_categoria`) REFERENCES `categoria_planificacion` (`id_categoria`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `categoria_planificacion` (`codigo`, `nombre`, `descripcion`, `orden`) VALUES
  ('OBJETIVOS', 'Objetivos de aprendizaje',
   'Los objetivos son claros, están formulados en términos de lo que el estudiante logrará, se pueden comprobar al final de la clase y se relacionan con el currículo.', 1),
  ('INICIO', 'Inicio de la clase',
   'Activa los conocimientos previos, motiva al curso y presenta el propósito de la clase.', 2),
  ('DESARROLLO', 'Desarrollo y actividades',
   'Las actividades siguen una secuencia lógica, son pertinentes a los objetivos, tienen un nivel de dificultad adecuado y hacen participar activamente a los estudiantes.', 3),
  ('CIERRE', 'Cierre de la clase',
   'Sintetiza lo aprendido, vuelve a los objetivos y da espacio para que los estudiantes reflexionen sobre su aprendizaje.', 4),
  ('EVALUACION', 'Evaluación del aprendizaje',
   'Define cómo se comprobará que los estudiantes lograron los objetivos (instrumentos, preguntas o criterios) y es coherente con ellos.', 5),
  ('CONTENIDO_MATEMATICO', 'Contenido matemático',
   'El contenido es correcto y riguroso, usa lenguaje matemático adecuado, incluye ejemplos pertinentes y anticipa errores o dificultades frecuentes.', 6),
  ('GESTION_TIEMPO', 'Gestión del tiempo',
   'Los tiempos asignados a cada momento de la clase son realistas y suficientes para las actividades planteadas.', 7),
  ('ATENCION_DIVERSIDAD', 'Atención a la diversidad',
   'Considera distintos ritmos y niveles de aprendizaje, y prevé apoyos o extensiones para quienes los necesiten.', 8);
