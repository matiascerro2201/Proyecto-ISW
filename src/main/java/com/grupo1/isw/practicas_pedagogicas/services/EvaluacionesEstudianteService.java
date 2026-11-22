package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository.CriterioEvaluado;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository.EvaluacionEsperada;
import com.grupo1.isw.practicas_pedagogicas.repositories.NotaFinalRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.NotaFinalRepository.NotaFinal;
import com.grupo1.isw.practicas_pedagogicas.services.EntregaEstudianteService.EntregaVista;
import com.grupo1.isw.practicas_pedagogicas.services.EntregaEstudianteService.GrupoEntregas;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * N-20 a N-23: la tabla de evaluaciones del estudiante (entregas y pautas de colaborador y tutor, con su
 * ponderación, estado y nota), el detalle de cada nota cuando el profesor la publicó y la nota final del informe de
 * desempeño en la misma tabla. Solo aparece lo habilitado de semestres iniciados, y las notas solo cuando están
 * publicadas.
 */
@Service
public class EvaluacionesEstudianteService {
    public static final String ENTREGA = "ENTREGA";
    public static final String PAUTA = "PAUTA";

    /** Estados de una fila: ENTREGADA, PENDIENTE, NO_ENTREGADA (entregas) y APLICADA, PENDIENTE (pautas). */
    public static final String APLICADA = "APLICADA";

    public record FilaEvaluacion(String tipo, String nombre, String detalle, BigDecimal ponderacion,
                                 LocalDate fechaLimite, String estado, BigDecimal nota, boolean puedeEntregar,
                                 boolean tieneArchivo, boolean conDetalle, Long idInscripcion, Long idEntrega,
                                 Long idEvaluacion) {
        public boolean esEntrega() {
            return ENTREGA.equals(tipo);
        }
    }

    public record GrupoEvaluaciones(Long idInscripcion, String asignatura, String periodo,
                                    List<FilaEvaluacion> filas, BigDecimal notaFinal) {
        public boolean tieneNotaFinal() {
            return notaFinal != null;
        }
    }

    public record DetalleEntrega(String asignatura, String periodo, String nombre, BigDecimal ponderacion,
                                 LocalDate fechaLimite, LocalDateTime entregadaEl, BigDecimal nota,
                                 String comentario, boolean automatica) {
    }

    public record DimensionDetalle(String nombre, BigDecimal obtenido, BigDecimal maximo,
                                   List<CriterioEvaluado> criterios) {
    }

    public record DetallePauta(String asignatura, String periodo, String nombre, String evaluador,
                               String nombreEvaluador, BigDecimal ponderacion, BigDecimal nota, BigDecimal puntaje,
                               BigDecimal puntajeMaximo, LocalDateTime fecha, List<DimensionDetalle> dimensiones) {
    }

    private final EntregaEstudianteService entregas;
    private final EvaluacionesEsperadasRepository pautas;
    private final NotaFinalRepository notasFinales;
    private final CalculadoraNota calculadora;

    public EvaluacionesEstudianteService(EntregaEstudianteService entregas, EvaluacionesEsperadasRepository pautas,
                                         NotaFinalRepository notasFinales, CalculadoraNota calculadora) {
        this.entregas = entregas;
        this.pautas = pautas;
        this.notasFinales = notasFinales;
        this.calculadora = calculadora;
    }

    /** N-20, N-21 y N-23: una tabla por asignatura y semestre. */
    @Transactional(readOnly = true)
    public List<GrupoEvaluaciones> tabla(long idEstudiante) {
        Map<Long, String[]> cabeceras = new LinkedHashMap<>();
        Map<Long, List<FilaEvaluacion>> filas = new LinkedHashMap<>();

        for (GrupoEntregas grupo : entregas.listar(idEstudiante)) {
            cabeceras.putIfAbsent(grupo.idInscripcion(), new String[]{grupo.asignatura(), grupo.periodo()});
            List<FilaEvaluacion> lista = filas.computeIfAbsent(grupo.idInscripcion(), k -> new ArrayList<>());
            for (EntregaVista e : grupo.entregas()) {
                lista.add(filaDeEntrega(e));
            }
        }
        for (EvaluacionEsperada p : pautas.delEstudiante(idEstudiante)) {
            cabeceras.putIfAbsent(p.idInscripcion(), new String[]{p.asignatura(), p.periodo()});
            filas.computeIfAbsent(p.idInscripcion(), k -> new ArrayList<>()).add(filaDePauta(p));
        }

        Map<Long, NotaFinal> finales = notasFinales.publicadas(idEstudiante);
        List<GrupoEvaluaciones> grupos = new ArrayList<>();
        cabeceras.forEach((id, cabecera) -> {
            NotaFinal nota = finales.get(id);
            grupos.add(new GrupoEvaluaciones(id, cabecera[0], cabecera[1], List.copyOf(filas.get(id)),
                    nota == null ? null : nota.nota()));
        });
        return grupos;
    }

    /** N-22: comentario del profesor y nota de una entrega, solo si ya fue publicada. */
    @Transactional(readOnly = true)
    public DetalleEntrega detalleEntrega(long idEstudiante, long idInscripcion, long idEntrega) {
        for (GrupoEntregas grupo : entregas.listar(idEstudiante)) {
            if (!grupo.idInscripcion().equals(idInscripcion)) {
                continue;
            }
            for (EntregaVista e : grupo.entregas()) {
                if (e.datos().idEntrega().equals(idEntrega) && e.notaVisible()) {
                    var d = e.datos();
                    return new DetalleEntrega(grupo.asignatura(), grupo.periodo(), d.nombre(), d.ponderacion(),
                            d.fechaLimite(), d.fechaSubida(), d.nota(), d.comentario(), d.automatica());
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta nota todavía no está disponible");
    }

    /** N-22: la nota de una pauta con el detalle criterio por criterio, solo si ya fue publicada. */
    @Transactional(readOnly = true)
    public DetallePauta detallePauta(long idEstudiante, long idEvaluacion) {
        EvaluacionEsperada pauta = pautas.delEstudiante(idEstudiante).stream()
                .filter(p -> p.aplicada() && p.publicada() && idEvaluacion == p.idEvaluacion())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Esta evaluación todavía no está disponible"));

        Map<String, List<CriterioEvaluado>> porDimension = new LinkedHashMap<>();
        for (CriterioEvaluado c : pautas.criterios(idEvaluacion)) {
            porDimension.computeIfAbsent(c.dimension(), k -> new ArrayList<>()).add(c);
        }
        List<DimensionDetalle> dimensiones = new ArrayList<>();
        porDimension.forEach((nombre, criterios) -> {
            BigDecimal obtenido = criterios.stream().map(CriterioEvaluado::obtenido)
                    .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal maximo = criterios.stream().map(CriterioEvaluado::maximo)
                    .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            dimensiones.add(new DimensionDetalle(nombre, obtenido, maximo, List.copyOf(criterios)));
        });
        return new DetallePauta(pauta.asignatura(), pauta.periodo(), pauta.pauta(), pauta.etiquetaEvaluador(),
                pauta.nombreEvaluador(), pauta.ponderacion(), NotasDePauta.redondeada(calculadora, pauta),
                pauta.puntaje(), pauta.puntajeMaximo(), pauta.fechaEvaluacion(), List.copyOf(dimensiones));
    }

    private static FilaEvaluacion filaDeEntrega(EntregaVista e) {
        var d = e.datos();
        return new FilaEvaluacion(ENTREGA, d.nombre(), "Entrega de documento", d.ponderacion(), d.fechaLimite(),
                e.estado(), e.notaVisible() ? d.nota() : null, e.puedeSubir(), d.tieneArchivo(), e.notaVisible(),
                d.idInscripcion(), d.idEntrega(), null);
    }

    private FilaEvaluacion filaDePauta(EvaluacionEsperada p) {
        boolean visible = p.aplicada() && p.publicada();
        String detalle = p.etiquetaEvaluador() + " · " + p.etiquetaLinea();
        return new FilaEvaluacion(PAUTA, p.pauta() + " (" + p.etiquetaAplicacion() + ")", detalle, p.ponderacion(),
                p.fechaLimite(), p.aplicada() ? APLICADA : EntregaEstudianteService.PENDIENTE,
                visible ? NotasDePauta.redondeada(calculadora, p) : null, false, false, visible, p.idInscripcion(),
                null, visible ? p.idEvaluacion() : null);
    }
}
