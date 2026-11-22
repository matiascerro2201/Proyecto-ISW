package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository.ContextoEntrega;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository.FilaEstudianteEntrega;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository.ResumenEntrega;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository.SeccionDatos;
import com.grupo1.isw.practicas_pedagogicas.services.EntregaEstudianteService.DescargaEntrega;
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
import java.util.List;

/**
 * N-12 a N-15: el profesor de la asignatura ve quiénes entregaron y quiénes faltan, descarga cada PDF, pone la nota
 * (1,0 a 7,0) y un comentario, cambia el 1,0 automático y publica cada nota al estudiante.
 */
@Service
public class EntregaProfesorService {
    static final int LARGO_COMENTARIO = 2000;
    public static final String TIPO_NOTIFICACION = "NOTA_ENTREGA";

    public record DetalleEntrega(SeccionDatos seccion, ResumenEntrega entrega, boolean plazoVencido,
                                 List<FilaEstudianteEntrega> estudiantes) {
    }

    private final SeccionEstructuraRepository secciones;
    private final EntregaEstudianteRepository repositorio;
    private final EntregaEstudianteService entregaEstudianteService;
    private final NotaAutomaticaService notaAutomatica;
    private final NotificacionService notificaciones;
    private final Clock reloj;

    @Autowired
    public EntregaProfesorService(SeccionEstructuraRepository secciones, EntregaEstudianteRepository repositorio,
                                  EntregaEstudianteService entregaEstudianteService,
                                  NotaAutomaticaService notaAutomatica, NotificacionService notificaciones) {
        this(secciones, repositorio, entregaEstudianteService, notaAutomatica, notificaciones,
                Clock.systemDefaultZone());
    }

    public EntregaProfesorService(SeccionEstructuraRepository secciones, EntregaEstudianteRepository repositorio,
                                  EntregaEstudianteService entregaEstudianteService,
                                  NotaAutomaticaService notaAutomatica, NotificacionService notificaciones,
                                  Clock reloj) {
        this.secciones = secciones;
        this.repositorio = repositorio;
        this.entregaEstudianteService = entregaEstudianteService;
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

    /** N-12: cada entrega del semestre con cuántos entregaron y cuántos faltan. */
    @Transactional
    public List<ResumenEntrega> resumen(long idProfesor, long idSeccion) {
        seccionDelProfesor(idProfesor, idSeccion);
        notaAutomatica.aplicar(idSeccion);
        return repositorio.resumenDeLaSeccion(idSeccion);
    }

    /** N-12: quiénes entregaron y quiénes no en una entrega, con su nota y estado de publicación. */
    @Transactional
    public DetalleEntrega detalle(long idProfesor, long idSeccion, long idEntrega) {
        SeccionDatos seccion = seccionDelProfesor(idProfesor, idSeccion);
        notaAutomatica.aplicar(idSeccion);
        ResumenEntrega entrega = repositorio.resumenDeLaSeccion(idSeccion).stream()
                .filter(r -> r.idEntrega().equals(idEntrega))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrega no encontrada"));
        boolean vencido = EntregaEstudianteService.plazoVencido(entrega.fechaLimite(), LocalDate.now(reloj));
        return new DetalleEntrega(seccion, entrega, vencido, new ArrayList<>(
                repositorio.estudiantesDeLaEntrega(idSeccion, idEntrega)));
    }

    /** N-12: el profesor descarga el PDF que entregó un estudiante. */
    @Transactional(readOnly = true)
    public DescargaEntrega descargar(long idProfesor, long idSeccion, long idEntrega, long idInscripcion) {
        seccionDelProfesor(idProfesor, idSeccion);
        ContextoEntrega contexto = contexto(idSeccion, idEntrega, idInscripcion);
        DescargaEntrega descarga = entregaEstudianteService.descarga(contexto);
        return descarga;
    }

    /**
     * N-13 y N-14: pone la nota y el comentario. Sirve para calificar un trabajo entregado y también para cambiar el
     * 1,0 automático de quien no entregó. Una nota puesta por el profesor deja de ser automática.
     *
     * @throws EstructuraInvalidaException si la nota o el comentario no son válidos
     * @throws IllegalStateException       si el estudiante no ha entregado y su plazo sigue abierto
     */
    @Transactional
    public void calificar(long idProfesor, long idSeccion, long idEntrega, long idInscripcion,
                          String nota, String comentario) {
        seccionDelProfesor(idProfesor, idSeccion);
        ContextoEntrega contexto = contexto(idSeccion, idEntrega, idInscripcion);
        if (contexto.idRegistro() == null) {
            throw new IllegalStateException(
                    "El estudiante todavía no entrega y su plazo sigue abierto: no se puede calificar");
        }
        List<String> errores = new ArrayList<>();
        BigDecimal valor = EntradaNota.leer(nota, errores);
        String texto = comentario == null ? "" : comentario.trim();
        if (texto.length() > LARGO_COMENTARIO) {
            errores.add("El comentario no puede superar " + LARGO_COMENTARIO + " caracteres");
        }
        if (!errores.isEmpty()) {
            throw new EstructuraInvalidaException(errores);
        }
        repositorio.guardarNota(contexto.idRegistro(), valor, texto.isEmpty() ? null : texto,
                LocalDateTime.now(reloj));
    }

    /** N-15: publica la nota y el comentario de una entrega al estudiante y se lo avisa. */
    @Transactional
    public void publicar(long idProfesor, long idSeccion, long idEntrega, long idInscripcion) {
        seccionDelProfesor(idProfesor, idSeccion);
        ContextoEntrega contexto = contexto(idSeccion, idEntrega, idInscripcion);
        if (contexto.idRegistro() == null || contexto.nota() == null) {
            throw new IllegalStateException("Primero pon la nota; sin nota no se puede publicar");
        }
        if (contexto.publicada()) {
            throw new IllegalStateException("La nota de esta entrega ya está publicada");
        }
        repositorio.cambiarPublicada(contexto.idRegistro(), true);
        notificaciones.crear(contexto.idEstudiante(), TIPO_NOTIFICACION,
                "Se publicó tu nota de «" + contexto.nombreEntrega() + "»: "
                        + contexto.nota().toPlainString().replace('.', ','),
                "/estudiante/entregas");
    }

    /** N-15: deja de mostrar al estudiante la nota de una entrega (por ejemplo, para corregirla). */
    @Transactional
    public void retirarPublicacion(long idProfesor, long idSeccion, long idEntrega, long idInscripcion) {
        seccionDelProfesor(idProfesor, idSeccion);
        ContextoEntrega contexto = contexto(idSeccion, idEntrega, idInscripcion);
        if (contexto.idRegistro() == null || !contexto.publicada()) {
            throw new IllegalStateException("La nota de esta entrega no está publicada");
        }
        repositorio.cambiarPublicada(contexto.idRegistro(), false);
    }

    private SeccionDatos seccionDelProfesor(long idProfesor, long idSeccion) {
        SeccionDatos seccion = secciones.buscar(idSeccion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada"));
        if (!seccion.idProfesor().equals(idProfesor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No eres el profesor de esta sección");
        }
        return seccion;
    }

    /** La inscripción y la entrega deben ser de la sección del profesor. */
    private ContextoEntrega contexto(long idSeccion, long idEntrega, long idInscripcion) {
        ContextoEntrega contexto = repositorio.contexto(idInscripcion, idEntrega)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrega no encontrada"));
        if (!contexto.idSeccionInscripcion().equals(idSeccion) || !contexto.idSeccionEntrega().equals(idSeccion)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La entrega no es de esta sección");
        }
        return contexto;
    }
}
