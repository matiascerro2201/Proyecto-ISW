-- HU-26: una planificación puede existir como BORRADOR antes de enviarse al profesor,
-- para que el estudiante la trabaje con el asistente.
--
-- 1) Nuevo estado BORRADOR. Se agrega AL FINAL de la lista del enum: así MySQL no reordena
--    los valores existentes ni reconstruye la tabla. El valor por defecto sigue siendo
--    ENVIADA para no alterar a quien inserte planificaciones sin indicar el estado
--    (por ejemplo, los scripts de datos de prueba); el código pone BORRADOR de forma explícita.
-- 2) Un borrador todavía no se ha enviado, así que no tiene fecha de envío: la columna admite NULL.
-- 3) Para que eso no deje datos inconsistentes, toda planificación que NO sea borrador
--    debe tener fecha de envío (MySQL 8.0.16 o superior hace cumplir esta regla).

ALTER TABLE `planificacion`
  MODIFY COLUMN `estado` enum('ENVIADA','APROBADA','RECHAZADA','BORRADOR') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ENVIADA',
  MODIFY COLUMN `fecha_envio` datetime DEFAULT NULL,
  ADD CONSTRAINT `ck_planificacion_fecha_envio` CHECK (`estado` = 'BORRADOR' OR `fecha_envio` IS NOT NULL);
