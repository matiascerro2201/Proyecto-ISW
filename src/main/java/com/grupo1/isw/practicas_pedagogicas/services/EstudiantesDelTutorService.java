package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.models.EstadoInscripcion;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionAplicadaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Los estudiantes que supervisa un tutor, con el avance de sus pautas de evaluación. */
@Service
public class EstudiantesDelTutorService {

    public record PautaDeLinea(Long idPauta, int numeroAplicacion, BigDecimal ponderacion,
                               boolean evaluada, BigDecimal puntaje, LocalDateTime fecha) {
    }

    public record LineaDelEstudiante(Long idLinea, String tipo, List<PautaDeLinea> pautas) {
        public long evaluadas() {
            return pautas.stream().filter(PautaDeLinea::evaluada).count();
        }

        public long pendientes() {
            return pautas.size() - evaluadas();
        }
    }

    /** Un estudiante con todas sus líneas del curso. Su id es el de su primera línea. */
    public record EstudianteDelTutor(Long id, String nombre, String asignatura, int anio, int semestre,
                                     List<LineaDelEstudiante> lineas) {
        public String periodo() {
            return "Semestre " + semestre + " de " + anio;
        }

        /** Los ids de todas sus líneas, separados por espacio: sirve para volver a este estudiante desde una línea. */
        public String idsDeLineas() {
            return lineas.stream().map(linea -> String.valueOf(linea.idLinea())).collect(Collectors.joining(" "));
        }

        public String tipos() {
            return lineas.stream().map(LineaDelEstudiante::tipo).collect(Collectors.joining(" y "));
        }

        public long totalPautas() {
            return lineas.stream().mapToLong(linea -> linea.pautas().size()).sum();
        }

        public long evaluadas() {
            return lineas.stream().mapToLong(LineaDelEstudiante::evaluadas).sum();
        }

        public long pendientes() {
            return totalPautas() - evaluadas();
        }

        /** SIN_PAUTAS, PENDIENTE (falta evaluar algo) o AL_DIA. */
        public String estado() {
            if (totalPautas() == 0) {
                return "SIN_PAUTAS";
            }
            return pendientes() > 0 ? "PENDIENTE" : "AL_DIA";
        }
    }

    private final LineaPracticaRepository lineaRepository;
    private final PautaEvaluacionRepository pautaRepository;
    private final EvaluacionAplicadaRepository evaluacionRepository;

    public EstudiantesDelTutorService(LineaPracticaRepository lineaRepository,
                                      PautaEvaluacionRepository pautaRepository,
                                      EvaluacionAplicadaRepository evaluacionRepository) {
        this.lineaRepository = lineaRepository;
        this.pautaRepository = pautaRepository;
        this.evaluacionRepository = evaluacionRepository;
    }

    @Transactional(readOnly = true)
    public List<EstudianteDelTutor> listar(Long idTutor) {
        Map<Long, List<LineaPractica>> porInscripcion = lineaRepository.findByTutor(idTutor).stream()
                .filter(linea -> linea.getInscripcion().getEstado() != EstadoInscripcion.RETIRADO)
                .collect(Collectors.groupingBy(linea -> linea.getInscripcion().getIdInscripcion(),
                        LinkedHashMap::new, Collectors.toList()));

        Map<Long, List<PautaEvaluacion>> pautasPorAsignatura = new HashMap<>();
        List<EstudianteDelTutor> estudiantes = new ArrayList<>();
        for (List<LineaPractica> lineas : porInscripcion.values()) {
            InscripcionAsignatura inscripcion = lineas.get(0).getInscripcion();
            SeccionAsignatura seccion = inscripcion.getSeccion();
            List<PautaEvaluacion> pautas = pautasPorAsignatura.computeIfAbsent(
                    seccion.getAsignatura().getIdAsignatura(), pautaRepository::findByAsignatura);

            estudiantes.add(new EstudianteDelTutor(
                    lineas.get(0).getIdLinea(),
                    inscripcion.getEstudiante().getNombre(),
                    seccion.getAsignatura().getNombre(),
                    seccion.getAnio(),
                    seccion.getSemestre(),
                    lineas.stream().map(linea -> deLinea(linea, pautas)).toList()));
        }
        return estudiantes;
    }

    private LineaDelEstudiante deLinea(LineaPractica linea, List<PautaEvaluacion> pautas) {
        Map<Long, EvaluacionAplicada> hechas = new HashMap<>();
        for (EvaluacionAplicada evaluacion : evaluacionRepository.findDelTutorByLinea(linea.getIdLinea())) {
            hechas.put(evaluacion.getPautaEvaluacion().getIdPautaEvaluacion(), evaluacion);
        }

        List<PautaDeLinea> delTipo = pautas.stream()
                .filter(pauta -> pauta.getTipoLinea() == linea.getTipo())
                .sorted(Comparator.comparing(PautaEvaluacion::getNumeroAplicacion))
                .map(pauta -> {
                    EvaluacionAplicada evaluacion = hechas.get(pauta.getIdPautaEvaluacion());
                    return new PautaDeLinea(pauta.getIdPautaEvaluacion(), pauta.getNumeroAplicacion(),
                            pauta.getPonderacion(), evaluacion != null,
                            evaluacion != null ? evaluacion.getPuntajeTotal() : null,
                            evaluacion != null ? evaluacion.getFecha() : null);
                })
                .toList();
        return new LineaDelEstudiante(linea.getIdLinea(), CursoEstudiantesService.tipoDeLinea(linea), delTipo);
    }
}
