package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.models.CategoriaPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.InformeAsistente;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.repositories.CategoriaPlanificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeAsistenteRepository;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Respuesta;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Solicitud;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException.Causa;
import com.grupo1.isw.practicas_pedagogicas.services.informe.CalculadorInforme;
import com.grupo1.isw.practicas_pedagogicas.services.informe.InstruccionesInforme;
import com.grupo1.isw.practicas_pedagogicas.services.informe.ResumenInforme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * El informe del asistente para el profesor de la asignatura: las cifras de una sección y un análisis redactado
 * por el asistente a partir de ellas.
 *
 * <p>Privacidad: no se muestra nada, ni se envía nada al proveedor, mientras menos de N estudiantes (cinco por
 * defecto) hayan usado el asistente, porque con pocos sería posible reconocer a quién corresponde cada cifra.
 *
 * <p>Los métodos no son transaccionales a propósito, por la misma razón que en el chat: la llamada al asistente
 * puede tardar y una transacción abierta retendría una conexión de la base de datos todo ese tiempo.
 */
@Service
public class InformeAsistenteService {
    private static final Logger log = LoggerFactory.getLogger(InformeAsistenteService.class);

    /** Tope de seguridad para lo que se guarda del análisis. */
    public static final int MAX_CARACTERES_ANALISIS = 10_000;

    private static final String PEDIDO = "Redacta el análisis del curso con los datos entregados.";

    private final InformeAsistenteRepository informeRepository;
    private final CategoriaPlanificacionRepository categoriaRepository;
    private final ChatbotClient chatbotClient;
    private final int minimoEstudiantes;
    private final int maxAnalisisPorDia;

    public InformeAsistenteService(InformeAsistenteRepository informeRepository,
                                   CategoriaPlanificacionRepository categoriaRepository,
                                   ChatbotClient chatbotClient,
                                   @Value("${informe.minimo-estudiantes:5}") int minimoEstudiantes,
                                   @Value("${informe.max-analisis-por-dia:5}") int maxAnalisisPorDia) {
        this.informeRepository = informeRepository;
        this.categoriaRepository = categoriaRepository;
        this.chatbotClient = chatbotClient;
        this.minimoEstudiantes = minimoEstudiantes;
        this.maxAnalisisPorDia = maxAnalisisPorDia;
    }

    public record OpcionSeccion(Long idSeccion, String asignatura, String periodo) {
    }

    public record AnalisisGuardado(String contenido, LocalDateTime fecha, int numEstudiantes, int numPlanificaciones) {
    }

    /**
     * Lo que necesita la pantalla.
     *
     * @param resumen null mientras no haya estudiantes suficientes: en ese caso no se expone ninguna cifra
     * @param analisis null si todavía no se generó ninguno
     */
    public record VistaInforme(OpcionSeccion seccion,
                               boolean suficienteDatos,
                               int estudiantesConAsistente,
                               int minimoEstudiantes,
                               ResumenInforme resumen,
                               AnalisisGuardado analisis,
                               int analisisRestantesHoy) {
    }

    /** Las secciones que dicta el profesor, las más recientes primero. */
    public List<OpcionSeccion> listarSecciones(Long idProfesor) {
        return informeRepository.findSeccionesDelProfesor(idProfesor).stream().map(this::opcion).toList();
    }

    /** @throws ResponseStatusException 404 si la sección no existe o no la dicta este profesor */
    public VistaInforme obtenerInforme(Long idProfesor, Long idSeccion) {
        SeccionAsignatura seccion = seccionDelProfesor(idProfesor, idSeccion);
        ResumenInforme resumen = calcularResumen(idSeccion);
        boolean suficiente = resumen.estudiantesConAsistente() >= minimoEstudiantes;

        AnalisisGuardado analisis = informeRepository
                .findFirstBySeccion_IdSeccionOrderByFechaGeneracionDescIdInformeDesc(idSeccion)
                .map(i -> new AnalisisGuardado(i.getContenido(), i.getFechaGeneracion(),
                        i.getNumEstudiantes(), i.getNumPlanificaciones()))
                .orElse(null);

        int restantes = (int) Math.max(0L, maxAnalisisPorDia - analisisGeneradosHoy(idSeccion));
        return new VistaInforme(opcion(seccion), suficiente, resumen.estudiantesConAsistente(), minimoEstudiantes,
                suficiente ? resumen : null, analisis, restantes);
    }

    /**
     * Pide al asistente que redacte el análisis de la sección y lo guarda.
     *
     * @throws ResponseStatusException      404 si la sección no existe o no la dicta este profesor
     * @throws IllegalStateException        se alcanzó el límite diario, o todavía no hay estudiantes suficientes
     * @throws ChatbotNoDisponibleException el asistente no pudo responder; no se guarda nada
     */
    public void generarAnalisis(Long idProfesor, Long idSeccion) {
        SeccionAsignatura seccion = seccionDelProfesor(idProfesor, idSeccion);

        if (analisisGeneradosHoy(idSeccion) >= maxAnalisisPorDia) {
            throw new IllegalStateException("Ya se generaron " + maxAnalisisPorDia
                    + " análisis hoy para esta sección. Podrás generar otro mañana.");
        }

        ResumenInforme resumen = calcularResumen(idSeccion);
        if (resumen.estudiantesConAsistente() < minimoEstudiantes) {
            throw new IllegalStateException("Todavía no hay suficientes datos: se necesitan al menos "
                    + minimoEstudiantes + " estudiantes que hayan usado el asistente y hay "
                    + resumen.estudiantesConAsistente() + ".");
        }

        String texto = redactar(resumen, idSeccion);
        informeRepository.save(new InformeAsistente(seccion, LocalDateTime.now(), recortar(texto),
                resumen.estudiantesConAsistente(), resumen.planificacionesRevisadas()));
    }

    private String redactar(ResumenInforme resumen, Long idSeccion) {
        Respuesta respuesta;
        try {
            respuesta = chatbotClient.responder(
                    new Solicitud(InstruccionesInforme.armar(resumen), List.of(), List.of(), PEDIDO));
        } catch (ChatbotNoDisponibleException e) {
            log.warn("El asistente no pudo redactar el análisis de la sección {} (causa: {})", idSeccion, e.getCausa());
            throw e;
        } catch (RuntimeException e) {
            // Solo se registra el tipo de error: el mensaje podría contener datos del curso.
            log.error("Falla inesperada del asistente al redactar el análisis de la sección {}: {}", idSeccion,
                    e.getClass().getSimpleName());
            throw new ChatbotNoDisponibleException(Causa.ERROR, "El asistente falló", e);
        }

        if (respuesta == null || respuesta.texto().isBlank()) {
            throw new ChatbotNoDisponibleException(Causa.ERROR, "El asistente devolvió un análisis vacío");
        }
        return respuesta.texto().strip();
    }

    private ResumenInforme calcularResumen(Long idSeccion) {
        List<CategoriaPlanificacion> categorias = categoriaRepository.findByActivaTrueOrderByOrdenAsc();
        return CalculadorInforme.calcular(
                (int) informeRepository.contarEstudiantesDeLaSeccion(idSeccion),
                categorias,
                informeRepository.filasPlanificaciones(idSeccion),
                informeRepository.filasInteracciones(idSeccion),
                informeRepository.filasHallazgos(idSeccion),
                informeRepository.filasEvaluaciones(idSeccion));
    }

    /** Una sección ajena y una que no existe dan el mismo 404: no se revela cuál es cuál. */
    private SeccionAsignatura seccionDelProfesor(Long idProfesor, Long idSeccion) {
        return informeRepository.findSeccionesDelProfesor(idProfesor).stream()
                .filter(s -> s.getIdSeccion().equals(idSeccion))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada"));
    }

    private long analisisGeneradosHoy(Long idSeccion) {
        return informeRepository.countBySeccion_IdSeccionAndFechaGeneracionGreaterThanEqual(
                idSeccion, LocalDate.now().atStartOfDay());
    }

    private OpcionSeccion opcion(SeccionAsignatura seccion) {
        return new OpcionSeccion(seccion.getIdSeccion(), seccion.getAsignatura().getNombre(),
                seccion.getAnio() + "-" + seccion.getSemestre());
    }

    private String recortar(String texto) {
        return texto.length() <= MAX_CARACTERES_ANALISIS ? texto : texto.substring(0, MAX_CARACTERES_ANALISIS);
    }
}
