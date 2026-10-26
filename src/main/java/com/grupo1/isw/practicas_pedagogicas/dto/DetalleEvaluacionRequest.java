package com.grupo1.isw.practicas_pedagogicas.dto;

import java.math.BigDecimal;

public record DetalleEvaluacionRequest(
        Long idCriterio,
        BigDecimal puntaje,
        String comentario
) {}
