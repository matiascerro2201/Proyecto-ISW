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

@Component
public class PautaCargador {
    public record DimensionConCriterios(Dimension dimension, List<Criterio> criterios) {}

    private final DimensionRepository dimensionRepository;
    private final CriterioRepository criterioRepository;

    public PautaCargador(DimensionRepository dimensionRepository, CriterioRepository criterioRepository) {
        this.dimensionRepository = dimensionRepository;
        this.criterioRepository = criterioRepository;
    }

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

    public List<Criterio> criteriosDePauta(Long idPauta) {
        return cargar(List.of(idPauta)).getOrDefault(idPauta, List.of()).stream()
                .flatMap(dc -> dc.criterios().stream())
                .toList();
    }
}
