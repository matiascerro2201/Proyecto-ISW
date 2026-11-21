-- Rol COORDINADOR (HU-36, HU-37, HU-38, HU-39).
--
-- usuario.activo: desactivar una cuenta (HU-36) en vez de borrarla, para conservar todo su historial
--   (inscripciones, evaluaciones, informes). Una cuenta inactiva no puede iniciar sesión.
--   Las cuentas que ya existen quedan activas.
--
-- pauta_evaluacion.habilitada / pauta_documento.habilitada: el coordinador decide qué pautas están
--   vigentes para cada asignatura (HU-38). Una pauta deshabilitada no se borra (conserva sus evaluaciones
--   ya hechas); solo deja de contarse como pendiente. Las pautas que ya existen quedan habilitadas.
--   La marca es por asignatura: vale para todas las secciones (semestres) de esa asignatura.

ALTER TABLE `usuario`
  ADD COLUMN `activo` bit(1) NOT NULL DEFAULT b'1';

ALTER TABLE `pauta_evaluacion`
  ADD COLUMN `habilitada` bit(1) NOT NULL DEFAULT b'1';

ALTER TABLE `pauta_documento`
  ADD COLUMN `habilitada` bit(1) NOT NULL DEFAULT b'1';
