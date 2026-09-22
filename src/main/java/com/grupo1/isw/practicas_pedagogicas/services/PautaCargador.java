package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.models.Criterio;
import com.grupo1.isw.practicas_pedagogicas.models.Dimension;
import com.grupo1.isw.practicas_pedagogicas.repositories.CriterioRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.DimensionRepository;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Carga el árbol pauta -> dimensiones -> criterios con 2 consultas (reutiliza los métodos de
 * DimensionRepository y CriterioRepository de HU-07) y lo entrega ya agrupado y ordenado.
 */
@Component
public class PautaCargador {

    public record DimensionConCriterios(Dimension dimension, List<Criterio> criterios) {}

    private final DimensionRepository dimensionRepository;
    private final CriterioRepository criterioRepository;

    public PautaCargador(DimensionRepository dimensionRepository, CriterioRepository criterioRepository) {
        this.dimensionRepository = dimensionRepository;
        this.criterioRepository = criterioRepository;
    }

    /** id de pauta -> sus dimensiones (en orden), cada una con sus criterios (en orden). */
    public Map<Long, List<DimensionConCriterios>> cargar(Collection<Long> idsPauta) {
        if (idsPauta.isEmpty()) {
            return Map.of();
        }
        List<Dimension> dimensiones = dimensionRepository.findByPautas(idsPauta);

        Map<Long, List<Criterio>> criteriosPorDimension = Map.of();
        if (!dimensiones.isEmpty()) {
            List<Long> idsDimension = dimensiones.stream().map(Dimension::getIdDimension).toList();
            criteriosPorDimension = criterioRepository.findByDimensiones(idsDimension).stream()
                    .collect(Collectors.groupingBy(c -> c.getDimension().getIdDimension()));
        }

        final Map<Long, List<Criterio>> criterios = criteriosPorDimension;
        Map<Long, List<DimensionConCriterios>> resultado = new LinkedHashMap<>();
        for (Dimension d : dimensiones) {
            resultado
                    .computeIfAbsent(d.getPautaEvaluacion().getIdPautaEvaluacion(), k -> new ArrayList<>())
                    .add(new DimensionConCriterios(d, criterios.getOrDefault(d.getIdDimension(), List.of())));
        }
        return resultado;
    }

    /** Todos los criterios de UNA pauta, en orden. */
    public List<Criterio> criteriosDePauta(Long idPauta) {
        return cargar(List.of(idPauta)).getOrDefault(idPauta, List.of()).stream()
                .flatMap(dc -> dc.criterios().stream())
                .toList();
    }
}
