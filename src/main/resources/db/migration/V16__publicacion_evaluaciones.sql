-- N-18: el profesor de la asignatura publica cada pauta aplicada (nota y detalle) al estudiante.
ALTER TABLE evaluacion_aplicada
    ADD COLUMN publicada bit(1) NOT NULL DEFAULT b'0',
    ADD COLUMN fecha_publicacion datetime NULL;
