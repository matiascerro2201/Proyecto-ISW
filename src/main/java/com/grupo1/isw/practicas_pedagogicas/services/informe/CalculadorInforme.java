package com.grupo1.isw.practicas_pedagogicas.services.informe;

import com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaHallazgo;
import com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaInteraccion;
import com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.CategoriaPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.NivelHallazgo;
import com.grupo1.isw.practicas_pedagogicas.services.informe.ResumenInforme.Aprobacion;
import com.grupo1.isw.practicas_pedagogicas.services.informe.ResumenInforme.Aspecto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Calcula las cifras del informe a partir de las filas de una sección. Es lógica pura, sin base de datos.
 *
 * <p>La unidad es la planificación, no el mensaje: quien consulta diez veces pesa lo mismo que quien consulta
 * una. De cada planificación se toma la primera valoración del asistente (lo que no lograba hacer sola) y la
 * última (lo que seguía débil).
 */
public final class CalculadorInforme {

    static final int MAX_EJEMPLOS_POR_ASPECTO = 3;
    static final int MAX_CARACTERES_EJEMPLO = 200;

    private CalculadorInforme() {
    }

    public static ResumenInforme calcular(int totalEstudiantes,
                                          List<CategoriaPlanificacion> categorias,
                                          List<FilaPlanificacion> planificaciones,
                                          List<FilaInteraccion> interacciones,
                                          List<FilaHallazgo> hallazgos,
                                          List<FilaEvaluacion> evaluaciones) {

        Set<Long> estudiantes = new HashSet<>();
        Map<Long, LocalDateTime> primeraConsulta = new HashMap<>();
        for (FilaInteraccion interaccion : interacciones) {
            estudiantes.add(interaccion.idEstudiante());
            primeraConsulta.merge(interaccion.idPlanificacion(), interaccion.fecha(),
                    (a, b) -> b.isBefore(a) ? b : a);
        }

        Map<Long, FilaEvaluacion> primeraEvaluacion = new HashMap<>();
        for (FilaEvaluacion evaluacion : evaluaciones) {
            primeraEvaluacion.merge(evaluacion.idPlanificacion(), evaluacion,
                    (a, b) -> esAnterior(b, a) ? b : a);
        }

        int enviadas = 0;
        int enviadasConAsistente = 0;
        for (FilaPlanificacion planificacion : planificaciones) {
            if (planificacion.estado() == EstadoPlanificacion.BORRADOR) {
                continue;
            }
            enviadas++;
            if (consultoAntes(planificacion.idPlanificacion(), primeraConsulta, primeraEvaluacion)) {
                enviadasConAsistente++;
            }
        }

        int evaluadasCon = 0;
        int aprobadasCon = 0;
        int evaluadasSin = 0;
        int aprobadasSin = 0;
        for (FilaEvaluacion primera : primeraEvaluacion.values()) {
            boolean consulto = consultoAntes(primera.idPlanificacion(), primeraConsulta, primeraEvaluacion);
            boolean aprobada = primera.decision() == EstadoPlanificacion.APROBADA;
            if (consulto) {
                evaluadasCon++;
                if (aprobada) {
                    aprobadasCon++;
                }
            } else {
                evaluadasSin++;
                if (aprobada) {
                    aprobadasSin++;
                }
            }
        }

        Valoraciones valoraciones = valorar(categorias, hallazgos);

        return new ResumenInforme(totalEstudiantes, estudiantes.size(), primeraConsulta.size(),
                interacciones.size(), enviadas, enviadasConAsistente, valoraciones.planificacionesValoradas(),
                valoraciones.aspectos(), new Aprobacion(evaluadasCon, aprobadasCon, evaluadasSin, aprobadasSin));
    }

    /**
     * ¿Consultó al asistente antes de enviar la planificación? Como el chat solo existe en borrador o rechazada,
     * una consulta anterior a la primera evaluación es una consulta anterior al primer envío. Si todavía no se
     * evaluó, cualquier consulta es anterior al envío.
     */
    private static boolean consultoAntes(Long idPlanificacion, Map<Long, LocalDateTime> primeraConsulta,
                                         Map<Long, FilaEvaluacion> primeraEvaluacion) {
        LocalDateTime consulta = primeraConsulta.get(idPlanificacion);
        if (consulta == null) {
            return false;
        }
        FilaEvaluacion evaluacion = primeraEvaluacion.get(idPlanificacion);
        return evaluacion == null || consulta.isBefore(evaluacion.fecha());
    }

    /** a es anterior a b por fecha; con la misma fecha, por identificador. */
    private static boolean esAnterior(FilaEvaluacion a, FilaEvaluacion b) {
        if (a.fecha().isBefore(b.fecha())) {
            return true;
        }
        return a.fecha().equals(b.fecha()) && a.idEvaluacion() < b.idEvaluacion();
    }

    private record Valoraciones(int planificacionesValoradas, List<Aspecto> aspectos) {
    }

    private static Valoraciones valorar(List<CategoriaPlanificacion> categorias, List<FilaHallazgo> hallazgos) {
        Map<String, String> nombres = new LinkedHashMap<>();
        for (CategoriaPlanificacion categoria : categorias) {
            nombres.put(categoria.getCodigo(), categoria.getNombre());
        }

        Map<Long, List<FilaHallazgo>> porPlanificacion = new TreeMap<>();
        for (FilaHallazgo hallazgo : hallazgos) {
            if (nombres.containsKey(hallazgo.codigoCategoria())) {
                porPlanificacion.computeIfAbsent(hallazgo.idPlanificacion(), id -> new ArrayList<>()).add(hallazgo);
            }
        }

        Map<String, Integer> debilesPrimera = new HashMap<>();
        Map<String, Integer> debilesUltima = new HashMap<>();
        Map<String, Set<String>> ejemplos = new HashMap<>();

        Comparator<FilaHallazgo> cronologico = Comparator.comparing(FilaHallazgo::fecha)
                .thenComparing(FilaHallazgo::idInteraccion);
        for (List<FilaHallazgo> filas : porPlanificacion.values()) {
            Long idPrimera = filas.stream().min(cronologico).orElseThrow().idInteraccion();
            Long idUltima = filas.stream().max(cronologico).orElseThrow().idInteraccion();
            for (FilaHallazgo fila : filas) {
                if (fila.nivel() != NivelHallazgo.DEBIL) {
                    continue;
                }
                if (fila.idInteraccion().equals(idPrimera)) {
                    debilesPrimera.merge(fila.codigoCategoria(), 1, Integer::sum);
                    anotarEjemplo(ejemplos, fila);
                }
                if (fila.idInteraccion().equals(idUltima)) {
                    debilesUltima.merge(fila.codigoCategoria(), 1, Integer::sum);
                }
            }
        }

        List<Aspecto> aspectos = new ArrayList<>();
        for (Map.Entry<String, String> categoria : nombres.entrySet()) {
            String codigo = categoria.getKey();
            aspectos.add(new Aspecto(codigo, categoria.getValue(),
                    debilesPrimera.getOrDefault(codigo, 0), debilesUltima.getOrDefault(codigo, 0),
                    new ArrayList<>(ejemplos.getOrDefault(codigo, Set.of()))));
        }
        // Estable: a igual debilidad se conserva el orden del catálogo.
        aspectos.sort(Comparator.comparingInt(Aspecto::debilesPrimera).reversed());

        return new Valoraciones(porPlanificacion.size(), aspectos);
    }

    private static void anotarEjemplo(Map<String, Set<String>> ejemplos, FilaHallazgo fila) {
        if (fila.observacion() == null || fila.observacion().isBlank()) {
            return;
        }
        Set<String> delAspecto = ejemplos.computeIfAbsent(fila.codigoCategoria(), c -> new LinkedHashSet<>());
        if (delAspecto.size() >= MAX_EJEMPLOS_POR_ASPECTO) {
            return;
        }
        String texto = fila.observacion().strip();
        delAspecto.add(texto.length() <= MAX_CARACTERES_EJEMPLO ? texto : texto.substring(0, MAX_CARACTERES_EJEMPLO));
    }
}
