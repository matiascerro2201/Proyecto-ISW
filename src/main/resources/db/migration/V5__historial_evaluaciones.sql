-- HU-26: historial de las evaluaciones de cada planificación.
--
-- Al reenviar una planificación rechazada, el sistema sobrescribe su estado y borra la retroalimentación,
-- así que sin un historial no queda registro de que hubo un rechazo. Esta tabla guarda cada decisión del
-- colaborador (APROBADA o RECHAZADA) con su fecha y su retroalimentación. Permite medir, por ejemplo, cuántas
-- planificaciones se aprueban en la primera evaluación. Es un registro: no se modifica ni se borra, salvo
-- que se borre la planificación completa.
--
-- Las planificaciones que ya estaban evaluadas al aplicar esta migración se copian con su última evaluación
-- conocida. Si alguna ya había sido rechazada y reenviada antes, esas evaluaciones anteriores no se pueden
-- reconstruir, de modo que para ellas la primera evaluación del historial puede no ser la primera real.

CREATE TABLE `evaluacion_planificacion` (
  `id_evaluacion` bigint NOT NULL AUTO_INCREMENT,
  `id_planificacion` bigint NOT NULL,
  `decision` enum('APROBADA','RECHAZADA') COLLATE utf8mb4_unicode_ci NOT NULL,
  `retroalimentacion` text COLLATE utf8mb4_unicode_ci,
  `fecha` datetime NOT NULL,
  PRIMARY KEY (`id_evaluacion`),
  KEY `idx_evaluacion_planificacion_fecha` (`id_planificacion`,`fecha`),
  CONSTRAINT `fk_evaluacion_planificacion` FOREIGN KEY (`id_planificacion`) REFERENCES `planificacion` (`id_planificacion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `evaluacion_planificacion` (`id_planificacion`, `decision`, `retroalimentacion`, `fecha`)
SELECT `id_planificacion`, `estado`, `retroalimentacion`, COALESCE(`fecha_retro`, `fecha_envio`)
FROM `planificacion`
WHERE `estado` IN ('APROBADA', 'RECHAZADA');
