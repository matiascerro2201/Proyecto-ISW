ALTER TABLE evaluacion_aplicada ADD COLUMN fecha_limite DATE;
ALTER TABLE evaluacion_aplicada ADD COLUMN estado VARCHAR(20) DEFAULT 'PENDIENTE';