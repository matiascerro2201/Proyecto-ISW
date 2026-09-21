package com.grupo1.isw.practicas_pedagogicas.dto;

import java.math.BigDecimal;

/**
 * Lo que el colaborador escribió para UN criterio en el formulario. El comentario es solo texto.
 * El puntaje puede llegar nulo (campo vacío o ilegible): el validador lo informa por criterio.
 */
public record DetalleEvaluacionRequest(
        Long idCriterio,
        BigDecimal puntaje,
        String comentario
) {}
