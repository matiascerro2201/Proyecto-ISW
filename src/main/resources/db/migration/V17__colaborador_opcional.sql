-- Algunas asignaturas no cuentan con profesor colaborador: la línea de práctica puede quedar sin él.
-- La llave foránea fk_linea_colaborador se mantiene: si hay colaborador, debe existir.
ALTER TABLE linea_practica
    MODIFY id_colaborador bigint NULL;
