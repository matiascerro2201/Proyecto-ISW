-- Fecha para la que el estudiante necesita la revisión de su planificación (la elige al enviarla).
-- Debe ser como mínimo 7 días después del envío (regla en PlanificacionService, no en la base:
-- una restricción CHECK no puede comparar contra la fecha de hoy).
-- Es NULL en borradores y en las planificaciones anteriores a esta versión.
--
-- recordatorio_revision_enviado: el aviso al colaborador (cuando faltan 2 días) se envía una sola vez.
-- Se reinicia en false cada vez que la planificación se envía o reenvía con una fecha nueva.
ALTER TABLE `planificacion`
  ADD COLUMN `fecha_revision_solicitada` date NULL,
  ADD COLUMN `recordatorio_revision_enviado` bit(1) NOT NULL DEFAULT b'0',
  ADD KEY `idx_planificacion_fecha_revision` (`estado`, `fecha_revision_solicitada`);
