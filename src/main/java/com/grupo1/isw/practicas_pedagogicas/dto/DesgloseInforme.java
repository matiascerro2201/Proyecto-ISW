package com.grupo1.isw.practicas_pedagogicas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** HU-10: cómo fue evaluado el estudiante y por qué; la calificación calculada es null mientras haya pendientes. */
public record DesgloseInforme(
        List<PautaDesglose> evaluaciones,
        List<DocumentoDesglose> documentos,
        BigDecimal calificacionCalculada,
        BigDecimal sumaPonderaciones,
        BigDecimal exigencia,
        List<String> pendientes,
        LocalDateTime ultimaModificacion
) {
    public DesgloseInforme {
        evaluaciones = List.copyOf(evaluaciones);
        documentos = List.copyOf(documentos);
        pendientes = List.copyOf(pendientes);
    }

    /** True si no falta nada y por lo tanto hay calificación calculada. */
    public boolean completo() {
        return pendientes.isEmpty();
    }

    /** Las ponderaciones suelen sumar 100; si no, la nota igual se calcula normalizando por la suma real. */
    public boolean ponderacionesSuman100() {
        return sumaPonderaciones.compareTo(new BigDecimal("100")) == 0;
    }

    /** Una pauta de desempeño (p. ej. "Especialidad · aplicación 1") y cómo salió. */
    public record PautaDesglose(
            Long idPauta,
            String titulo,
            BigDecimal ponderacion,
            boolean evaluada,
            BigDecimal puntajeObtenido,
            BigDecimal puntajeMaximo,
            BigDecimal porcentaje,
            BigDecimal nota,
            LocalDateTime fecha,
            List<DimensionDesglose> dimensiones
    ) {
        public PautaDesglose {
            dimensiones = List.copyOf(dimensiones);
        }
    }

    public record DimensionDesglose(
            String nombre,
            BigDecimal puntajeObtenido,
            BigDecimal puntajeMaximo,
            List<CriterioDesglose> criterios
    ) {
        public DimensionDesglose {
            criterios = List.copyOf(criterios);
        }
    }

    /** Un criterio con el puntaje obtenido y el comentario del evaluador que lo justifica. */
    public record CriterioDesglose(
            String descripcion,
            BigDecimal puntajeObtenido,
            BigDecimal puntajeMaximo,
            String comentario
    ) {
    }

    /** Una entrega (informe, producto o proyecto de intervención) y su evaluación. */
    public record DocumentoDesglose(
            String etiqueta,
            BigDecimal ponderacion,
            boolean entregado,
            boolean evaluado,
            BigDecimal puntaje,
            BigDecimal puntajeMaximo,
            BigDecimal porcentaje,
            BigDecimal nota,
            String comentarios
    ) {
    }
}
