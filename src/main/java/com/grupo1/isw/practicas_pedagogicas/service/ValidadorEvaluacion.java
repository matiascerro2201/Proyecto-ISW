package com.grupo1.isw.practicas_pedagogicas.service;

import com.grupo1.isw.practicas_pedagogicas.dto.DetalleEvaluacionRequest;
import com.grupo1.isw.practicas_pedagogicas.models.Criterio;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reglas de los puntajes de una evaluación. Junta TODOS los errores antes de lanzar,
 * para que el colaborador corrija todo de una vez.
 *
 * Reglas: solo criterios de la pauta, sin repetidos, todos presentes, puntaje entre 0 y
 * puntaje_max, con máximo 2 decimales (la columna es DECIMAL(5,2)).
 * Los puntajes se comparan con compareTo: para BigDecimal, 4.0 y 4.00 no son equals.
 */
@Component
public class ValidadorEvaluacion {

    private static final int DECIMALES_MAX = 2;

    /** @return el puntaje total (suma de los puntajes, con 2 decimales) si todo es válido */
    public BigDecimal validar(List<Criterio> criteriosDeLaPauta, List<DetalleEvaluacionRequest> detalles) {
        if (criteriosDeLaPauta.isEmpty()) {
            throw new EvaluacionInvalidaException(List.of("La pauta no tiene criterios definidos"));
        }
        if (detalles == null || detalles.isEmpty()) {
            throw new EvaluacionInvalidaException(List.of("Debes evaluar todos los criterios de la pauta"));
        }

        Map<Long, Criterio> porId = new LinkedHashMap<>();
        for (Criterio c : criteriosDeLaPauta) {
            porId.put(c.getIdCriterio(), c);
        }

        List<String> errores = new ArrayList<>();
        Set<Long> vistos = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;

        for (DetalleEvaluacionRequest detalle : detalles) {
            if (detalle == null || detalle.idCriterio() == null) {
                errores.add("Hay un criterio sin identificador");
                continue;
            }
            Criterio criterio = porId.get(detalle.idCriterio());
            if (criterio == null) {
                errores.add("El criterio " + detalle.idCriterio() + " no pertenece a esta pauta");
                continue;
            }
            if (!vistos.add(detalle.idCriterio())) {
                errores.add("El criterio " + etiqueta(criterio) + " viene repetido");
                continue;
            }

            BigDecimal puntaje = detalle.puntaje();
            if (puntaje == null) {
                errores.add("Falta el puntaje (o no es válido) de " + etiqueta(criterio));
            } else if (puntaje.signum() < 0) {
                errores.add("El puntaje de " + etiqueta(criterio) + " no puede ser negativo");
            } else if (puntaje.stripTrailingZeros().scale() > DECIMALES_MAX) {
                errores.add("El puntaje de " + etiqueta(criterio) + " admite como máximo 2 decimales");
            } else if (puntaje.compareTo(criterio.getPuntajeMax()) > 0) {
                errores.add("El puntaje de " + etiqueta(criterio) + " (" + puntaje.toPlainString()
                        + ") supera el máximo de " + criterio.getPuntajeMax().toPlainString());
            } else {
                total = total.add(puntaje);
            }
        }

        List<String> faltantes = new ArrayList<>();
        for (Map.Entry<Long, Criterio> entrada : porId.entrySet()) {
            if (!vistos.contains(entrada.getKey())) {
                faltantes.add(etiqueta(entrada.getValue()));
            }
        }
        if (!faltantes.isEmpty()) {
            errores.add("Faltan criterios por evaluar: " + String.join(", ", faltantes));
        }

        if (!errores.isEmpty()) {
            throw new EvaluacionInvalidaException(errores);
        }
        return total.setScale(DECIMALES_MAX, RoundingMode.HALF_UP);
    }

    private static String etiqueta(Criterio criterio) {
        String texto = criterio.getDescripcion() == null ? "" : criterio.getDescripcion().strip();
        if (texto.length() > 70) {
            texto = texto.substring(0, 67) + "...";
        }
        return "«" + texto + "»";
    }
}
