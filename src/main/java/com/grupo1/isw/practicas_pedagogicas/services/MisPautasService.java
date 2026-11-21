package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.PautaEvaluacionResponse;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.repositories.InscripcionAsignaturaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Las pautas de evaluación que ve un estudiante: una lista con todas y la vista de cada una. Solo lectura. */
@Service
public class MisPautasService {

    public record CriterioVista(String descripcion, BigDecimal puntajeMax) {
    }

    public record DimensionVista(String nombre, List<CriterioVista> criterios) {
        public int cantidadCriterios() {
            return criterios.size();
        }

        public BigDecimal puntajeMaximo() {
            return criterios.stream().map(CriterioVista::puntajeMax).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /** Una pauta con el período más reciente en que el estudiante cursa su asignatura. */
    public record PautaVista(Long idPauta, Long idAsignatura, String asignatura, int anio, int semestre,
                             String tipoLinea, int numeroAplicacion, BigDecimal ponderacion,
                             List<DimensionVista> dimensiones) {
        public String periodo() {
            return "Semestre " + semestre + " de " + anio;
        }

        public String linea() {
            return switch (tipoLinea) {
                case "UNICA" -> "Línea única";
                case "ESPECIALIDAD" -> "Especialidad";
                case "ORIENTACION" -> "Orientación";
                default -> tipoLinea;
            };
        }

        public int cantidadCriterios() {
            return dimensiones.stream().mapToInt(DimensionVista::cantidadCriterios).sum();
        }

        public BigDecimal puntajeMaximo() {
            return dimensiones.stream().map(DimensionVista::puntajeMaximo).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    private final InscripcionAsignaturaRepository inscripcionRepository;
    private final PautaConsultaService pautaConsultaService;

    public MisPautasService(InscripcionAsignaturaRepository inscripcionRepository,
                            PautaConsultaService pautaConsultaService) {
        this.inscripcionRepository = inscripcionRepository;
        this.pautaConsultaService = pautaConsultaService;
    }

    @Transactional(readOnly = true)
    public List<PautaVista> listar(Long idEstudiante) {
        // Las inscripciones llegan de la más reciente a la más antigua: de cada asignatura se usa la última
        Map<Long, InscripcionAsignatura> porAsignatura = new LinkedHashMap<>();
        for (InscripcionAsignatura inscripcion : inscripcionRepository.findByEstudianteConPautas(idEstudiante)) {
            porAsignatura.putIfAbsent(inscripcion.getSeccion().getAsignatura().getIdAsignatura(), inscripcion);
        }

        List<PautaVista> pautas = new ArrayList<>();
        porAsignatura.forEach((idAsignatura, inscripcion) -> {
            SeccionAsignatura seccion = inscripcion.getSeccion();
            pautaConsultaService.consultarPautasDeAsignatura(idAsignatura, idEstudiante).pautas()
                    .forEach(pauta -> pautas.add(aVista(pauta, idAsignatura, seccion)));
        });

        pautas.sort(Comparator.comparingInt(PautaVista::anio).reversed()
                .thenComparing(Comparator.comparingInt(PautaVista::semestre).reversed())
                .thenComparing(PautaVista::asignatura)
                .thenComparing(PautaVista::tipoLinea)
                .thenComparingInt(PautaVista::numeroAplicacion));
        return pautas;
    }

    /** @throws ResponseStatusException 404 si la pauta no existe o no es de una asignatura del estudiante */
    @Transactional(readOnly = true)
    public PautaVista obtener(Long idEstudiante, Long idPauta) {
        return listar(idEstudiante).stream()
                .filter(pauta -> pauta.idPauta().equals(idPauta))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La pauta no existe o no pertenece a tus asignaturas"));
    }

    private PautaVista aVista(PautaEvaluacionResponse pauta, Long idAsignatura, SeccionAsignatura seccion) {
        List<DimensionVista> dimensiones = pauta.dimensiones().stream()
                .sorted(Comparator.comparingInt(dimension -> dimension.orden()))
                .map(dimension -> new DimensionVista(dimension.nombre(), dimension.criterios().stream()
                        .sorted(Comparator.comparingInt(criterio -> criterio.orden()))
                        .map(criterio -> new CriterioVista(criterio.descripcion(), criterio.puntajeMax()))
                        .toList()))
                .toList();
        return new PautaVista(pauta.idPautaEvaluacion(), idAsignatura, seccion.getAsignatura().getNombre(),
                seccion.getAnio(), seccion.getSemestre(), pauta.tipoLinea(), pauta.numeroAplicacion(),
                pauta.ponderacion(), dimensiones);
    }
}
