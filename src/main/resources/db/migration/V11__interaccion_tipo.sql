-- HU-30: tipo de cada interacción con el asistente.
--
-- Cada mensaje al asistente es una REVISION (el estudiante pidió una revisión general con el botón) o una
-- PREGUNTA (un mensaje normal). Las revisiones tienen un tope por planificación, así que hay que poder
-- contarlas; las preguntas no. Se guardan todas las interacciones, de ambos tipos, porque alimentan el
-- informe del profesor.
--
-- Las interacciones que ya existían quedan como PREGUNTA (el valor por defecto): antes de esta migración no
-- había botón de revisión, así que ninguna planificación parte con revisiones usadas.
--
-- El índice sirve para contar las revisiones de una planificación sin recorrer todas sus interacciones.
ALTER TABLE `interaccion_chatbot`
  ADD COLUMN `tipo` enum('REVISION','PREGUNTA') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PREGUNTA'
    AFTER `respuesta_bot`,
  ADD KEY `idx_interaccion_planificacion_tipo` (`id_planificacion`, `tipo`);
