package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository.ContextoEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository.CriterioEvaluado;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository.EvaluacionEsperada;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository.SeccionDatos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * N-18: el profesor de la asignatura revisa las pautas aplicadas por colaboradores y tutores y publica cada una
 * (nota y detalle criterio por criterio) al estudiante, que recibe un aviso.
 */
@Service
public class PublicacionPautaService {
    public static final String TIPO_NOTIFICACION = "NOTA_PAUTA";
    static final String ENLACE_ESTUDIANTE = "/estudiante/evaluaciones";

    public record FilaPauta(EvaluacionEsperada datos, BigDecimal nota) {
        public boolean puedePublicar() {
            return datos.aplicada() && !datos.publicada();
        }
    }

    public record PanelPautas(SeccionDatos seccion, List<FilaPauta> filas) {
        public long aplicadas() {
            return filas.stream().filter(f -> f.datos().aplicada()).count();
        }

        public long publicadas() {
            return filas.stream().filter(f -> f.datos().publicada()).count();
        }

        public long porPublicar() {
            return filas.stream().filter(FilaPauta::puedePublicar).count();
        }
    }

    public record DetallePauta(SeccionDatos seccion, ContextoEvaluacion evaluacion, EvaluacionEsperada datos,
                               BigDecimal nota, List<CriterioEvaluado> criterios) {
    }

    private final SeccionEstructuraRepository secciones;
    private final EvaluacionesEsperadasRepository repositorio;
    private final CalculadoraNota calculadora;
    private final NotificacionService notificaciones;
    private final Clock reloj;

    @Autowired
    public PublicacionPautaService(SeccionEstructuraRepository secciones, EvaluacionesEsperadasRepository repositorio,
                                   CalculadoraNota calculadora, NotificacionService notificaciones) {
        this(secciones, repositorio, calculadora, notificaciones, Clock.systemDefaultZone());
    }

    public PublicacionPautaService(SeccionEstructuraRepository secciones, EvaluacionesEsperadasRepository repositorio,
                                   CalculadoraNota calculadora, NotificacionService notificaciones, Clock reloj) {
        this.secciones = secciones;
        this.repositorio = repositorio;
        this.calculadora = calculadora;
        this.notificaciones = notificaciones;
        this.reloj = reloj;
    }

    public List<SeccionDatos> seccionesDelProfesor(long idProfesor) {
        return secciones.delProfesor(idProfesor).stream().filter(SeccionDatos::iniciada).toList();
    }

    @Transactional(readOnly = true)
    public PanelPautas panel(long idProfesor, long idSeccion) {
        SeccionDatos seccion = seccionDelProfesor(idProfesor, idSeccion);
        List<FilaPauta> filas = repositorio.deLaSeccion(idSeccion).stream()
                .map(e -> new FilaPauta(e, NotasDePauta.redondeada(calculadora, e)))
                .toList();
        return new PanelPautas(seccion, filas);
    }

    @Transactional(readOnly = true)
    public DetallePauta detalle(long idProfesor, long idSeccion, long idEvaluacion) {
        SeccionDatos seccion = seccionDelProfesor(idProfesor, idSeccion);
        ContextoEvaluacion contexto = contexto(idSeccion, idEvaluacion);
        EvaluacionEsperada datos = repositorio.deLaInscripcion(contexto.idInscripcion()).stream()
                .filter(e -> contexto.idEvaluacion().equals(e.idEvaluacion()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluación no encontrada"));
        return new DetallePauta(seccion, contexto, datos, NotasDePauta.redondeada(calculadora, datos),
                repositorio.criterios(idEvaluacion));
    }

    /** Publica una pauta aplicada y avisa al estudiante. */
    @Transactional
    public void publicar(long idProfesor, long idSeccion, long idEvaluacion) {
        seccionDelProfesor(idProfesor, idSeccion);
        ContextoEvaluacion contexto = contexto(idSeccion, idEvaluacion);
        publicarContexto(contexto);
    }

    /** Publica de una vez todas las pautas aplicadas que aún no se han publicado. @return cuántas */
    @Transactional
    public int publicarTodas(long idProfesor, long idSeccion) {
        seccionDelProfesor(idProfesor, idSeccion);
        int publicadas = 0;
        for (EvaluacionEsperada e : repositorio.deLaSeccion(idSeccion)) {
            if (e.aplicada() && !e.publicada()) {
                publicarContexto(contexto(idSeccion, e.idEvaluacion()));
                publicadas++;
            }
        }
        return publicadas;
    }

    /** Deja de mostrar la pauta al estudiante (por ejemplo, si el evaluador va a corregirla). */
    @Transactional
    public void retirar(long idProfesor, long idSeccion, long idEvaluacion) {
        seccionDelProfesor(idProfesor, idSeccion);
        ContextoEvaluacion contexto = contexto(idSeccion, idEvaluacion);
        if (!contexto.publicada()) {
            throw new IllegalStateException("Esta pauta no está publicada");
        }
        repositorio.cambiarPublicada(idEvaluacion, false, LocalDateTime.now(reloj));
    }

    private void publicarContexto(ContextoEvaluacion contexto) {
        if (contexto.puntaje() == null) {
            throw new IllegalStateException("La pauta todavía no ha sido aplicada por el evaluador");
        }
        if (contexto.publicada()) {
            throw new IllegalStateException("Esta pauta ya está publicada");
        }
        repositorio.cambiarPublicada(contexto.idEvaluacion(), true, LocalDateTime.now(reloj));
        notificaciones.crear(contexto.idEstudiante(), TIPO_NOTIFICACION,
                "Se publicó la evaluación «" + contexto.pauta() + "» de tu práctica.", ENLACE_ESTUDIANTE);
    }

    private SeccionDatos seccionDelProfesor(long idProfesor, long idSeccion) {
        SeccionDatos seccion = secciones.buscar(idSeccion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada"));
        if (!seccion.idProfesor().equals(idProfesor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No eres el profesor de esta sección");
        }
        return seccion;
    }

    private ContextoEvaluacion contexto(long idSeccion, long idEvaluacion) {
        ContextoEvaluacion contexto = repositorio.contexto(idEvaluacion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluación no encontrada"));
        if (!contexto.idSeccion().equals(idSeccion)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La evaluación no es de esta sección");
        }
        return contexto;
    }
}
