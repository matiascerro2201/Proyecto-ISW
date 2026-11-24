package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository.EvaluacionEsperada;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeDesempenoRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeDesempenoRepository.DatosInscripcion;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeDesempenoRepository.EntregaNota;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeDesempenoRepository.InformeFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository.SeccionDatos;
import com.grupo1.isw.practicas_pedagogicas.services.CalculadoraNota.ItemNota;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * N-19: informe de desempeño con la estructura del semestre. La nota sugerida es el promedio ponderado de las
 * entregas, las pautas del colaborador y las pautas del tutor, cada una con la ponderación que fijó el coordinador.
 * El profesor genera el informe, fija la nota final y la publica (modifica HU-10 y HU-11 para los semestres nuevos).
 */
@Service
public class InformeDesempenoService {
    public static final String ENTREGA = "ENTREGA";
    public static final String PAUTA = "PAUTA";

    /** Un ítem del informe: una entrega o una pauta, con su peso y su nota (null mientras falte). */
    public record Componente(String tipo, String nombre, String detalle, BigDecimal ponderacion,
                             BigDecimal notaExacta, BigDecimal nota, String estado) {
        public boolean pendiente() {
            return notaExacta == null;
        }
    }

    public record DetalleDesempeno(DatosInscripcion inscripcion, List<Componente> componentes,
                                   BigDecimal sumaPonderaciones, BigDecimal sugerida, List<String> pendientes,
                                   InformeFila informe) {
        public boolean completo() {
            return pendientes.isEmpty() && !componentes.isEmpty();
        }

        public boolean cerrado() {
            return informe != null && informe.validado() != null;
        }

        public boolean puedeGenerar() {
            return completo() && !cerrado();
        }

        public boolean puedeFijarNota() {
            return informe != null && informe.publicado() == null;
        }

        public boolean puedePublicar() {
            return informe != null && informe.finalNota() != null && informe.publicado() == null;
        }

        public boolean ponderacionesSuman100() {
            return sumaPonderaciones.compareTo(new BigDecimal("100")) == 0;
        }

        /** True si el informe ya generado quedó distinto de lo que se calcularía hoy. */
        public boolean desactualizado() {
            return informe != null && sugerida != null && informe.sugerida() != null
                    && informe.sugerida().compareTo(sugerida) != 0;
        }
    }

    public record FilaEstudiante(DatosInscripcion inscripcion, int pendientes, BigDecimal sugerida,
                                 InformeFila informe) {
        public String estado() {
            if (informe != null) {
                return informe.estado();
            }
            return pendientes == 0 ? "LISTO" : "PENDIENTE";
        }
    }

    private final SeccionEstructuraRepository secciones;
    private final InformeDesempenoRepository repositorio;
    private final EvaluacionesEsperadasRepository evaluaciones;
    private final CalculadoraNota calculadora;
    private final NotaAutomaticaService notaAutomatica;
    private final NotificacionService notificaciones;
    private final Clock reloj;

    @Autowired
    public InformeDesempenoService(SeccionEstructuraRepository secciones, InformeDesempenoRepository repositorio,
                                   EvaluacionesEsperadasRepository evaluaciones, CalculadoraNota calculadora,
                                   NotaAutomaticaService notaAutomatica, NotificacionService notificaciones) {
        this(secciones, repositorio, evaluaciones, calculadora, notaAutomatica, notificaciones,
                Clock.systemDefaultZone());
    }

    public InformeDesempenoService(SeccionEstructuraRepository secciones, InformeDesempenoRepository repositorio,
                                   EvaluacionesEsperadasRepository evaluaciones, CalculadoraNota calculadora,
                                   NotaAutomaticaService notaAutomatica, NotificacionService notificaciones,
                                   Clock reloj) {
        this.secciones = secciones;
        this.repositorio = repositorio;
        this.evaluaciones = evaluaciones;
        this.calculadora = calculadora;
        this.notaAutomatica = notaAutomatica;
        this.notificaciones = notificaciones;
        this.reloj = reloj;
    }

    public List<SeccionDatos> seccionesDelProfesor(long idProfesor) {
        return secciones.delProfesor(idProfesor).stream().filter(SeccionDatos::iniciada).toList();
    }

    public SeccionDatos seccion(long idProfesor, long idSeccion) {
        return seccionDelProfesor(idProfesor, idSeccion);
    }

    /** Los estudiantes de la sección con cuánto les falta y el estado de su informe. */
    @Transactional
    public List<FilaEstudiante> estudiantes(long idProfesor, long idSeccion) {
        seccionDelProfesor(idProfesor, idSeccion);
        notaAutomatica.aplicar(idSeccion);
        Map<Long, InformeFila> informes = new HashMap<>();
        repositorio.informesDeLaSeccion(idSeccion).forEach(i -> informes.put(i.idInscripcion(), i));
        Map<Long, List<EvaluacionEsperada>> pautas = new HashMap<>();
        evaluaciones.deLaSeccion(idSeccion).forEach(e ->
                pautas.computeIfAbsent(e.idInscripcion(), k -> new ArrayList<>()).add(e));

        List<FilaEstudiante> filas = new ArrayList<>();
        for (DatosInscripcion inscripcion : repositorio.inscripcionesDeLaSeccion(idSeccion)) {
            DetalleDesempeno detalle = armar(inscripcion, repositorio.entregas(inscripcion.idInscripcion()),
                    pautas.getOrDefault(inscripcion.idInscripcion(), List.of()),
                    informes.get(inscripcion.idInscripcion()));
            filas.add(new FilaEstudiante(inscripcion, detalle.pendientes().size(), detalle.sugerida(),
                    informes.get(inscripcion.idInscripcion())));
        }
        return filas;
    }

    @Transactional
    public DetalleDesempeno detalle(long idProfesor, long idInscripcion) {
        DatosInscripcion inscripcion = inscripcionDelProfesor(idProfesor, idInscripcion);
        notaAutomatica.aplicar(inscripcion.idSeccion());
        return armar(inscripcion);
    }

    /**
     * Calcula la nota sugerida y crea (o actualiza) el informe.
     *
     * @throws EstructuraInvalidaException si falta alguna nota o evaluación: trae todo lo que falta
     * @throws IllegalStateException       si el informe ya fue validado o publicado
     */
    @Transactional
    public BigDecimal generar(long idProfesor, long idInscripcion) {
        DatosInscripcion inscripcion = inscripcionDelProfesor(idProfesor, idInscripcion);
        notaAutomatica.aplicar(inscripcion.idSeccion());
        DetalleDesempeno detalle = armar(inscripcion);
        if (detalle.cerrado()) {
            throw new IllegalStateException("El informe ya fue validado o publicado y no se puede volver a generar");
        }
        if (detalle.componentes().isEmpty()) {
            throw new EstructuraInvalidaException(List.of("El semestre no tiene entregas ni pautas habilitadas"));
        }
        if (!detalle.pendientes().isEmpty()) {
            throw new EstructuraInvalidaException(detalle.pendientes());
        }
        repositorio.guardarSugerida(idInscripcion, idProfesor, detalle.sugerida(), LocalDateTime.now(reloj));
        return detalle.sugerida();
    }

    /**
     * Fija la nota final (1,0 a 7,0) que ve el estudiante cuando el profesor publica el informe.
     */
    @Transactional
    public void fijarNotaFinal(long idProfesor, long idInscripcion, String nota) {
        inscripcionDelProfesor(idProfesor, idInscripcion);
        InformeFila informe = repositorio.informe(idInscripcion)
                .orElseThrow(() -> new IllegalStateException("Primero genera el informe"));
        if (informe.publicado() != null) {
            throw new IllegalStateException("El informe ya fue publicado y la nota no se puede cambiar");
        }
        List<String> errores = new ArrayList<>();
        BigDecimal valor = EntradaNota.leer(nota, errores);
        if (!errores.isEmpty()) {
            throw new EstructuraInvalidaException(errores);
        }
        repositorio.guardarFinal(idInscripcion, valor, LocalDateTime.now(reloj));
    }

    /** Publica el informe: desde ese momento el estudiante ve su nota final y recibe un aviso. */
    @Transactional
    public void publicar(long idProfesor, long idInscripcion) {
        DatosInscripcion inscripcion = inscripcionDelProfesor(idProfesor, idInscripcion);
        InformeFila informe = repositorio.informe(idInscripcion)
                .orElseThrow(() -> new IllegalStateException("Primero genera el informe"));
        if (informe.publicado() != null) {
            throw new IllegalStateException("El informe ya fue publicado");
        }
        if (informe.finalNota() == null || informe.validado() == null) {
            throw new IllegalStateException("Fija la nota final antes de publicar el informe");
        }
        repositorio.publicar(idInscripcion, LocalDateTime.now(reloj));
        notificaciones.crear(inscripcion.idEstudiante(), NotificacionService.TIPO_INFORME_FINAL,
                "Tu informe final de desempeño de " + inscripcion.asignatura() + " (semestre "
                        + inscripcion.semestre() + " de " + inscripcion.anio() + ") ya está disponible.",
                "/estudiante/evaluaciones");
    }

    private DetalleDesempeno armar(DatosInscripcion inscripcion) {
        return armar(inscripcion, repositorio.entregas(inscripcion.idInscripcion()),
                evaluaciones.deLaInscripcion(inscripcion.idInscripcion()),
                repositorio.informe(inscripcion.idInscripcion()).orElse(null));
    }

    private DetalleDesempeno armar(DatosInscripcion inscripcion, List<EntregaNota> entregas,
                                   List<EvaluacionEsperada> pautas, InformeFila informe) {
        LocalDate hoy = LocalDate.now(reloj);
        List<Componente> componentes = new ArrayList<>();
        List<String> pendientes = new ArrayList<>();

        for (EntregaNota e : entregas) {
            if (e.nota() != null) {
                componentes.add(new Componente(ENTREGA, e.nombre(), e.automatica() ? "Entrega (no entregada)"
                        : "Entrega", e.ponderacion(), e.nota(), e.nota(), "CALIFICADA"));
            } else if (e.tieneArchivo()) {
                componentes.add(new Componente(ENTREGA, e.nombre(), "Entrega", e.ponderacion(), null, null,
                        "SIN_CALIFICAR"));
                pendientes.add("Falta calificar la entrega «" + e.nombre() + "»");
            } else {
                boolean vencida = e.fechaLimite() != null && e.fechaLimite().isBefore(hoy);
                componentes.add(new Componente(ENTREGA, e.nombre(), "Entrega", e.ponderacion(), null, null,
                        vencida ? "SIN_NOTA" : "NO_ENTREGADA"));
                pendientes.add(vencida ? "La entrega «" + e.nombre() + "» no tiene nota"
                        : "Falta la entrega «" + e.nombre() + "» (el plazo sigue abierto)");
            }
        }
        for (EvaluacionEsperada p : pautas) {
            BigDecimal exacta = NotasDePauta.exacta(calculadora, p);
            String titulo = p.pauta() + " · " + p.etiquetaLinea() + " · aplicación " + p.etiquetaAplicacion();
            if (exacta != null) {
                componentes.add(new Componente(PAUTA, titulo, p.etiquetaEvaluador(), p.ponderacion(), exacta,
                        calculadora.redondear(exacta), "APLICADA"));
            } else {
                componentes.add(new Componente(PAUTA, titulo, p.etiquetaEvaluador(), p.ponderacion(), null, null,
                        "SIN_APLICAR"));
                pendientes.add("Falta la evaluación del " + p.etiquetaEvaluador().toLowerCase() + ": " + titulo);
            }
        }

        BigDecimal suma = componentes.stream().map(Componente::ponderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sugerida = null;
        if (pendientes.isEmpty() && !componentes.isEmpty() && suma.signum() > 0) {
            sugerida = calculadora.promedioPonderado(componentes.stream()
                    .map(c -> new ItemNota(c.notaExacta(), c.ponderacion())).toList());
        }
        return new DetalleDesempeno(inscripcion, List.copyOf(componentes), suma, sugerida, List.copyOf(pendientes),
                informe);
    }

    private SeccionDatos seccionDelProfesor(long idProfesor, long idSeccion) {
        SeccionDatos seccion = secciones.buscar(idSeccion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada"));
        if (!seccion.idProfesor().equals(idProfesor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No eres el profesor de esta sección");
        }
        return seccion;
    }

    private DatosInscripcion inscripcionDelProfesor(long idProfesor, long idInscripcion) {
        DatosInscripcion inscripcion = repositorio.inscripcion(idInscripcion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscripción no encontrada"));
        if (!inscripcion.idProfesor().equals(idProfesor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No eres el profesor de este estudiante");
        }
        if (!inscripcion.iniciada()) {
            throw new IllegalStateException("El semestre todavía no se inicia");
        }
        return inscripcion;
    }
}
