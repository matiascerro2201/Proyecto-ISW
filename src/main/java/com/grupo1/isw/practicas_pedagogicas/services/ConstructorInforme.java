package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.DesgloseInforme;
import com.grupo1.isw.practicas_pedagogicas.dto.DesgloseInforme.CriterioDesglose;
import com.grupo1.isw.practicas_pedagogicas.dto.DesgloseInforme.DimensionDesglose;
import com.grupo1.isw.practicas_pedagogicas.dto.DesgloseInforme.DocumentoDesglose;
import com.grupo1.isw.practicas_pedagogicas.dto.DesgloseInforme.PautaDesglose;
import com.grupo1.isw.practicas_pedagogicas.models.Criterio;
import com.grupo1.isw.practicas_pedagogicas.models.DetalleCriterio;
import com.grupo1.isw.practicas_pedagogicas.models.DocumentoEstudiante;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaDocumento;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.TipoDocumento;
import com.grupo1.isw.practicas_pedagogicas.models.TipoEvaluador;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.repositories.DetalleCriterioRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.DocumentoEstudianteRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionAplicadaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaDocumentoRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** HU-10: arma el desglose y la calificación sugerida; si falta una evaluación (colaborador, y tutor si la línea lo tiene)
 *  o una entrega no hay nota, solo pendientes. Llamar dentro de una transacción (recorre asociaciones lazy). */
@Component
public class ConstructorInforme {
    private static final BigDecimal CIEN = new BigDecimal("100");

    /** Resultado de procesar una pauta: lo que se muestra y la nota exacta (null si está pendiente). */
    private record PautaProcesada(PautaDesglose desglose, BigDecimal notaExacta) {
    }

    private final PautaEvaluacionRepository pautaRepository;
    private final PautaCargador cargador;
    private final EvaluacionAplicadaRepository evaluacionRepository;
    private final DetalleCriterioRepository detalleRepository;
    private final PautaDocumentoRepository pautaDocumentoRepository;
    private final DocumentoEstudianteRepository documentoRepository;
    private final CalculadoraNota calculadora;
    private final BigDecimal puntajeMaximoDocumento;

    public ConstructorInforme(PautaEvaluacionRepository pautaRepository,
                              PautaCargador cargador,
                              EvaluacionAplicadaRepository evaluacionRepository,
                              DetalleCriterioRepository detalleRepository,
                              PautaDocumentoRepository pautaDocumentoRepository,
                              DocumentoEstudianteRepository documentoRepository,
                              CalculadoraNota calculadora,
                              @Value("${app.informe.puntaje-maximo-documento:100}") BigDecimal puntajeMaximoDocumento) {
        if (puntajeMaximoDocumento == null || puntajeMaximoDocumento.signum() <= 0) {
            throw new IllegalArgumentException("El puntaje máximo de los documentos debe ser mayor que cero");
        }
        this.pautaRepository = pautaRepository;
        this.cargador = cargador;
        this.evaluacionRepository = evaluacionRepository;
        this.detalleRepository = detalleRepository;
        this.pautaDocumentoRepository = pautaDocumentoRepository;
        this.documentoRepository = documentoRepository;
        this.calculadora = calculadora;
        this.puntajeMaximoDocumento = puntajeMaximoDocumento;
    }

    public DesgloseInforme construir(InscripcionAsignatura inscripcion) {
        Long idAsignatura = inscripcion.getSeccion().getAsignatura().getIdAsignatura();
        Long idInscripcion = inscripcion.getIdInscripcion();

        List<String> pendientes = new ArrayList<>();
        List<CalculadoraNota.ItemNota> items = new ArrayList<>();
        BigDecimal sumaPonderaciones = BigDecimal.ZERO;
        LocalDateTime ultimaModificacion = null;

        // ---- Evaluaciones de desempeño (colaborador / tutor)
        Map<TipoLinea, LineaPractica> lineas = new EnumMap<>(TipoLinea.class);
        for (LineaPractica linea : inscripcion.getLineas()) {
            lineas.put(linea.getTipo(), linea);
        }
        List<PautaEvaluacion> pautas = pautaRepository.findByAsignatura(idAsignatura);
        List<EvaluacionAplicada> evaluaciones = evaluacionRepository.findByInscripcion(idInscripcion);

        Map<Long, List<EvaluacionAplicada>> evaluacionesPorPauta = new HashMap<>();
        for (EvaluacionAplicada evaluacion : evaluaciones) {
            evaluacionesPorPauta
                    .computeIfAbsent(evaluacion.getPautaEvaluacion().getIdPautaEvaluacion(), k -> new ArrayList<>())
                    .add(evaluacion);
            ultimaModificacion = masReciente(ultimaModificacion, evaluacion.getFecha());
        }

        Map<Long, List<PautaCargador.DimensionConCriterios>> estructura =
                cargador.cargar(pautas.stream().map(PautaEvaluacion::getIdPautaEvaluacion).toList());
        Map<Long, List<DetalleCriterio>> detallesPorEvaluacion = detallesPorEvaluacion(evaluaciones);

        List<PautaDesglose> desglosePautas = new ArrayList<>();
        for (PautaEvaluacion pauta : pautas) {
            Long idPauta = pauta.getIdPautaEvaluacion();
            PautaProcesada procesada = procesarPauta(
                    pauta,
                    lineas.get(pauta.getTipoLinea()),
                    evaluacionesPorPauta.getOrDefault(idPauta, List.of()),
                    estructura.getOrDefault(idPauta, List.of()),
                    detallesPorEvaluacion,
                    pendientes);
            desglosePautas.add(procesada.desglose());
            sumaPonderaciones = sumaPonderaciones.add(pauta.getPonderacion());
            if (procesada.notaExacta() != null) {
                items.add(new CalculadoraNota.ItemNota(procesada.notaExacta(), pauta.getPonderacion()));
            }
        }

        // ---- Documentos (informes, productos, proyecto de intervención)
        List<PautaDocumento> pautasDocumento =
                pautaDocumentoRepository.findByAsignatura_IdAsignaturaOrderByTipoAscNumeroAsc(idAsignatura);
        Map<Long, DocumentoEstudiante> entregados = documentoRepository
                .findByInscripcion_IdInscripcion(idInscripcion).stream()
                .collect(Collectors.toMap(
                        d -> d.getPautaDocumento().getIdPautaDocumento(),
                        Function.identity(),
                        (a, b) -> a.getFechaSubida().isAfter(b.getFechaSubida()) ? a : b));

        List<DocumentoDesglose> desgloseDocumentos = new ArrayList<>();
        for (PautaDocumento pautaDocumento : pautasDocumento) {
            DocumentoEstudiante documento = entregados.get(pautaDocumento.getIdPautaDocumento());
            String etiqueta = etiquetaDocumento(pautaDocumento);
            sumaPonderaciones = sumaPonderaciones.add(pautaDocumento.getPonderacion());

            if (documento == null) {
                pendientes.add("El estudiante aún no entrega: " + etiqueta);
                desgloseDocumentos.add(new DocumentoDesglose(etiqueta, pautaDocumento.getPonderacion(),
                        false, false, null, puntajeMaximoDocumento, null, null, null));
                continue;
            }
            ultimaModificacion = masReciente(ultimaModificacion, documento.getFechaSubida());

            if (documento.getPuntajeObtenido() == null) {
                pendientes.add("Falta evaluar la entrega: " + etiqueta);
                desgloseDocumentos.add(new DocumentoDesglose(etiqueta, pautaDocumento.getPonderacion(),
                        true, false, null, puntajeMaximoDocumento, null, null,
                        limpiar(documento.getComentariosEvaluador())));
                continue;
            }

            BigDecimal logro = calculadora.logro(documento.getPuntajeObtenido(), puntajeMaximoDocumento);
            BigDecimal notaExacta = calculadora.notaDesdeLogro(logro);
            items.add(new CalculadoraNota.ItemNota(notaExacta, pautaDocumento.getPonderacion()));
            desgloseDocumentos.add(new DocumentoDesglose(etiqueta, pautaDocumento.getPonderacion(),
                    true, true, documento.getPuntajeObtenido(), puntajeMaximoDocumento,
                    porcentaje(logro), calculadora.redondear(notaExacta),
                    limpiar(documento.getComentariosEvaluador())));
        }

        // ---- Calificación sugerida: solo si no falta nada
        BigDecimal calificacion = null;
        if (pendientes.isEmpty()) {
            if (items.isEmpty()) {
                pendientes.add("La asignatura no tiene pautas de evaluación ni entregas configuradas");
            } else if (sumaPonderaciones.signum() <= 0) {
                pendientes.add("Las ponderaciones configuradas para la asignatura suman cero");
            } else {
                calificacion = calculadora.promedioPonderado(items);
            }
        }

        return new DesgloseInforme(desglosePautas, desgloseDocumentos, calificacion, sumaPonderaciones,
                calculadora.getExigenciaPorcentaje(), pendientes, ultimaModificacion);
    }

    private PautaProcesada procesarPauta(PautaEvaluacion pauta,
                                         LineaPractica linea,
                                         List<EvaluacionAplicada> hechas,
                                         List<PautaCargador.DimensionConCriterios> estructura,
                                         Map<Long, List<DetalleCriterio>> detallesPorEvaluacion,
                                         List<String> pendientes) {
        String titulo = tituloPauta(pauta);

        BigDecimal maximo = BigDecimal.ZERO;
        for (PautaCargador.DimensionConCriterios dimension : estructura) {
            for (Criterio criterio : dimension.criterios()) {
                maximo = maximo.add(criterio.getPuntajeMax());
            }
        }

        if (linea == null) {
            pendientes.add("El estudiante no tiene asignada una línea de práctica para evaluar: " + titulo);
            return new PautaProcesada(pautaPendiente(pauta, titulo, maximo, List.of(), null), null);
        }

        // El colaborador siempre evalúa; el tutor solo si la línea tiene uno asignado.
        boolean faltaEvaluacion = false;
        if (!evaluadoPor(hechas, TipoEvaluador.COLABORADOR)) {
            pendientes.add("Falta la evaluación del profesor colaborador: " + titulo);
            faltaEvaluacion = true;
        }
        if (linea.getTutor() != null && !evaluadoPor(hechas, TipoEvaluador.TUTOR)) {
            pendientes.add("Falta la evaluación del profesor supervisor: " + titulo);
            faltaEvaluacion = true;
        }
        if (faltaEvaluacion) {
            return new PautaProcesada(pautaPendiente(pauta, titulo, maximo, List.of(), null), null);
        }

        LocalDateTime fecha = null;
        Map<Long, List<DetalleCriterio>> porCriterio = new HashMap<>();
        for (EvaluacionAplicada evaluacion : hechas) {
            fecha = masReciente(fecha, evaluacion.getFecha());
            for (DetalleCriterio detalle : detallesPorEvaluacion.getOrDefault(evaluacion.getIdEvaluacion(), List.of())) {
                porCriterio.computeIfAbsent(detalle.getCriterio().getIdCriterio(), k -> new ArrayList<>()).add(detalle);
            }
        }

        if (maximo.signum() <= 0) {
            pendientes.add("La pauta «" + titulo + "» no tiene criterios definidos");
            return new PautaProcesada(pautaPendiente(pauta, titulo, maximo, List.of(), fecha), null);
        }

        BigDecimal totalObtenido = BigDecimal.ZERO;
        boolean incompleta = false;
        List<DimensionDesglose> dimensiones = new ArrayList<>();
        for (PautaCargador.DimensionConCriterios dimension : estructura) {
            BigDecimal obtenidoDimension = BigDecimal.ZERO;
            BigDecimal maximoDimension = BigDecimal.ZERO;
            List<CriterioDesglose> criterios = new ArrayList<>();
            for (Criterio criterio : dimension.criterios()) {
                maximoDimension = maximoDimension.add(criterio.getPuntajeMax());
                List<DetalleCriterio> detalles = porCriterio.getOrDefault(criterio.getIdCriterio(), List.of());
                if (detalles.isEmpty()) {
                    incompleta = true;
                    criterios.add(new CriterioDesglose(criterio.getDescripcion(), null, criterio.getPuntajeMax(), null));
                    continue;
                }
                BigDecimal puntaje = promedio(detalles);
                obtenidoDimension = obtenidoDimension.add(puntaje);
                criterios.add(new CriterioDesglose(criterio.getDescripcion(), puntaje, criterio.getPuntajeMax(),
                        comentarioDe(detalles)));
            }
            totalObtenido = totalObtenido.add(obtenidoDimension);
            dimensiones.add(new DimensionDesglose(dimension.dimension().getNombre(),
                    obtenidoDimension, maximoDimension, criterios));
        }

        if (incompleta) {
            pendientes.add("La evaluación «" + titulo + "» está incompleta: faltan puntajes de algunos criterios");
            return new PautaProcesada(pautaPendiente(pauta, titulo, maximo, dimensiones, fecha), null);
        }

        BigDecimal logro = calculadora.logro(totalObtenido, maximo);
        BigDecimal notaExacta = calculadora.notaDesdeLogro(logro);
        PautaDesglose desglose = new PautaDesglose(pauta.getIdPautaEvaluacion(), titulo, pauta.getPonderacion(),
                true, totalObtenido, maximo, porcentaje(logro), calculadora.redondear(notaExacta), fecha, dimensiones);
        return new PautaProcesada(desglose, notaExacta);
    }

    private static boolean evaluadoPor(List<EvaluacionAplicada> evaluaciones, TipoEvaluador evaluador) {
        return evaluaciones.stream().anyMatch(e -> e.getTipoEvaluador() == evaluador);
    }

    private static PautaDesglose pautaPendiente(PautaEvaluacion pauta, String titulo, BigDecimal maximo,
                                                List<DimensionDesglose> dimensiones, LocalDateTime fecha) {
        return new PautaDesglose(pauta.getIdPautaEvaluacion(), titulo, pauta.getPonderacion(),
                false, null, maximo, null, null, fecha, dimensiones);
    }

    private Map<Long, List<DetalleCriterio>> detallesPorEvaluacion(List<EvaluacionAplicada> evaluaciones) {
        if (evaluaciones.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = evaluaciones.stream().map(EvaluacionAplicada::getIdEvaluacion).toList();
        return detalleRepository.findByEvaluaciones(ids).stream()
                .collect(Collectors.groupingBy(d -> d.getEvaluacion().getIdEvaluacion()));
    }

    private static BigDecimal promedio(List<DetalleCriterio> detalles) {
        BigDecimal suma = BigDecimal.ZERO;
        for (DetalleCriterio detalle : detalles) {
            suma = suma.add(detalle.getPuntajeObtenido());
        }
        return suma.divide(BigDecimal.valueOf(detalles.size()), 2, RoundingMode.HALF_UP);
    }

    private static String comentarioDe(List<DetalleCriterio> detalles) {
        String texto = detalles.stream()
                .map(DetalleCriterio::getTranscripcionComentario)
                .filter(c -> c != null && !c.isBlank())
                .map(String::strip)
                .distinct()
                .collect(Collectors.joining(" · "));
        return texto.isEmpty() ? null : texto;
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    private static BigDecimal porcentaje(BigDecimal logro) {
        return logro.multiply(CIEN).setScale(1, RoundingMode.HALF_UP);
    }

    private static LocalDateTime masReciente(LocalDateTime actual, LocalDateTime candidata) {
        if (candidata == null) {
            return actual;
        }
        return actual == null || candidata.isAfter(actual) ? candidata : actual;
    }

    private static String tituloPauta(PautaEvaluacion pauta) {
        return nombreLinea(pauta.getTipoLinea()) + " · aplicación " + pauta.getNumeroAplicacion();
    }

    private static String nombreLinea(TipoLinea tipo) {
        if (tipo == null) {
            return "Línea";
        }
        return switch (tipo) {
            case UNICA -> "Línea única";
            case ESPECIALIDAD -> "Especialidad";
            case ORIENTACION -> "Orientación";
        };
    }

    private static String etiquetaDocumento(PautaDocumento pauta) {
        TipoDocumento tipo = pauta.getTipo();
        return switch (tipo) {
            case INFORME -> "Informe " + pauta.getNumero();
            case PRODUCTO -> "Producto " + pauta.getNumero();
            case PROYECTO_INTERVENCION -> "Proyecto de intervención";
        };
    }
}
