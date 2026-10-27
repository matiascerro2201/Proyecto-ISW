package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.models.CategoriaPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.InteraccionChatbot;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.repositories.CategoriaPlanificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.InteraccionChatbotRepository;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionService.PlanificacionVista;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Hallazgo;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Mensaje;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Respuesta;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Rol;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Solicitud;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException.Causa;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.InstruccionesChatbot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Conversación del estudiante con el asistente sobre una de sus planificaciones.
 *
 * <p>Solo se puede conversar mientras la planificación se puede editar (BORRADOR o RECHAZADA).
 * Una consulta que no obtiene respuesta del asistente no se guarda, de modo que el estudiante puede
 * reintentarla sin perder lo que escribió.
 */
@Service
public class ChatbotService {
    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    /** Anterior a cualquier interacción: en un borrador cuenta toda la conversación. */
    public static final LocalDateTime DESDE_SIEMPRE = LocalDateTime.of(2000, 1, 1, 0, 0);
    /** Tamaño de la columna observacion de hallazgo_chatbot. */
    public static final int MAX_CARACTERES_OBSERVACION = 300;
    /** Tope de seguridad para lo que se guarda de la respuesta del asistente. */
    public static final int MAX_CARACTERES_RESPUESTA = 10_000;

    private final PlanificacionService planificacionService;
    private final InteraccionChatbotRepository interaccionRepository;
    private final CategoriaPlanificacionRepository categoriaRepository;
    private final InstruccionesChatbot instrucciones;
    private final ChatbotClient chatbotClient;
    private final int maxMensajesPorDia;
    private final int maxCaracteresMensaje;
    private final int maxIntercambiosHistorial;

    public ChatbotService(PlanificacionService planificacionService,
                          InteraccionChatbotRepository interaccionRepository,
                          CategoriaPlanificacionRepository categoriaRepository,
                          InstruccionesChatbot instrucciones,
                          ChatbotClient chatbotClient,
                          @Value("${chatbot.max-mensajes-por-dia:20}") int maxMensajesPorDia,
                          @Value("${chatbot.max-caracteres-mensaje:1500}") int maxCaracteresMensaje,
                          @Value("${chatbot.max-intercambios-historial:6}") int maxIntercambiosHistorial) {
        this.planificacionService = planificacionService;
        this.interaccionRepository = interaccionRepository;
        this.categoriaRepository = categoriaRepository;
        this.instrucciones = instrucciones;
        this.chatbotClient = chatbotClient;
        this.maxMensajesPorDia = maxMensajesPorDia;
        this.maxCaracteresMensaje = maxCaracteresMensaje;
        this.maxIntercambiosHistorial = maxIntercambiosHistorial;
    }

    /** Un intercambio de la conversación: lo que escribió el estudiante y lo que respondió el asistente. */
    public record Turno(String mensajeEstudiante, String respuestaBot, LocalDateTime fecha) {
    }

    /** Lo que necesita la pantalla del chat. */
    public record VistaChat(Long idPlanificacion,
                            String etiquetaLinea,
                            String contenido,
                            String retroalimentacion,
                            boolean rechazada,
                            List<Turno> turnos,
                            int mensajesRestantesHoy,
                            int maxCaracteresMensaje) {
    }

    /**
     * Envía un mensaje al asistente y guarda el intercambio con lo que el asistente valoró.
     *
     * <p>No es transaccional a propósito: la llamada al asistente puede tardar, y una transacción abierta
     * retendría una conexión de la base de datos todo ese tiempo. Lo único que se guarda de una vez
     * (la interacción con sus hallazgos) es una sola operación del repositorio.
     *
     * @throws org.springframework.web.server.ResponseStatusException 404 si no existe, 403 si no es suya
     * @throws IllegalStateException    la planificación no admite chat, o se alcanzó el límite diario
     * @throws IllegalArgumentException mensaje vacío o demasiado largo
     * @throws ChatbotNoDisponibleException el asistente no pudo responder; no se guarda nada
     */
    public void enviarMensaje(Long idEstudiante, Long idPlanificacion, String mensaje) {
        Planificacion planificacion = planificacionService.obtenerPlanificacionDelEstudiante(idEstudiante, idPlanificacion);
        validarQueSePuedeConversar(planificacion.getEstado());
        String texto = validarMensaje(mensaje);
        validarLimiteDiario(idEstudiante);

        List<CategoriaPlanificacion> categorias = categoriaRepository.findByActivaTrueOrderByOrdenAsc();
        if (categorias.isEmpty()) {
            throw new ChatbotNoDisponibleException(Causa.ERROR, "No hay aspectos de revisión configurados");
        }

        LocalDateTime desde = desde(planificacion.getEstado(), planificacion.getFechaRetro());
        List<Mensaje> historial = historialParaElAsistente(interaccionRepository
                .findByPlanificacion_IdPlanificacionAndFechaGreaterThanEqualOrderByFechaAscIdInteraccionAsc(
                        idPlanificacion, desde));
        String reglas = instrucciones.armar(categorias, planificacion.getContenido(),
                planificacion.getRetroalimentacion());
        List<String> codigos = categorias.stream().map(CategoriaPlanificacion::getCodigo).toList();

        Respuesta respuesta = consultarAlAsistente(new Solicitud(reglas, codigos, historial, texto), idPlanificacion);

        InteraccionChatbot interaccion = new InteraccionChatbot(planificacion, texto,
                recortar(respuesta.texto().strip(), MAX_CARACTERES_RESPUESTA), LocalDateTime.now());
        agregarHallazgos(interaccion, respuesta.hallazgos(), categorias);
        interaccionRepository.save(interaccion);
    }

    /**
     * La conversación actual de una planificación: desde el rechazo vigente si fue rechazada,
     * completa si es un borrador.
     *
     * @throws IllegalStateException la planificación no admite chat
     */
    public VistaChat obtenerConversacion(Long idEstudiante, Long idPlanificacion) {
        PlanificacionVista planificacion = planificacionService.obtenerVistaDelEstudiante(idEstudiante, idPlanificacion);
        validarQueSePuedeConversar(planificacion.estado());

        List<Turno> turnos = interaccionRepository
                .findByPlanificacion_IdPlanificacionAndFechaGreaterThanEqualOrderByFechaAscIdInteraccionAsc(
                        idPlanificacion, desde(planificacion.estado(), planificacion.fechaRetro()))
                .stream()
                .map(i -> new Turno(i.getMensajeEstudiante(), i.getRespuestaBot(), i.getFecha()))
                .toList();

        long enviadosHoy = mensajesEnviadosHoy(idEstudiante);
        int restantes = (int) Math.max(0L, maxMensajesPorDia - enviadosHoy);

        return new VistaChat(planificacion.idPlanificacion(), planificacion.etiquetaLinea(),
                planificacion.contenido(), planificacion.retroalimentacion(),
                planificacion.estado() == EstadoPlanificacion.RECHAZADA, turnos, restantes, maxCaracteresMensaje);
    }

    private void validarQueSePuedeConversar(EstadoPlanificacion estado) {
        if (!PlanificacionService.esEditable(estado)) {
            throw new IllegalStateException(
                    "Solo se puede conversar con el asistente sobre una planificación en borrador o rechazada");
        }
    }

    private String validarMensaje(String mensaje) {
        // El navegador cuenta cada salto de línea como 1 carácter dentro del cuadro de texto, pero al enviar el
        // formulario lo manda como CRLF (2). Se normaliza para que el máximo sea el mismo que ve el estudiante.
        String texto = mensaje == null ? "" : mensaje.strip().replace("\r\n", "\n");
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("Escribe un mensaje para el asistente");
        }
        if (texto.length() > maxCaracteresMensaje) {
            throw new IllegalArgumentException(
                    "El mensaje es demasiado largo: el máximo es de " + maxCaracteresMensaje + " caracteres");
        }
        return texto;
    }

    private void validarLimiteDiario(Long idEstudiante) {
        if (mensajesEnviadosHoy(idEstudiante) >= maxMensajesPorDia) {
            throw new IllegalStateException("Llegaste al límite de " + maxMensajesPorDia
                    + " mensajes por día con el asistente. Podrás seguir mañana.");
        }
    }

    private long mensajesEnviadosHoy(Long idEstudiante) {
        return interaccionRepository
                .countByPlanificacion_Linea_Inscripcion_Estudiante_IdUsuarioAndFechaGreaterThanEqual(
                        idEstudiante, LocalDate.now().atStartOfDay());
    }

    /** En una rechazada solo cuenta lo conversado desde el rechazo (el texto anterior ya no existe). */
    private LocalDateTime desde(EstadoPlanificacion estado, LocalDateTime fechaRetro) {
        if (estado == EstadoPlanificacion.RECHAZADA && fechaRetro != null) {
            return fechaRetro;
        }
        return DESDE_SIEMPRE;
    }

    /** Solo los últimos intercambios, para no gastar de más la cuota del proveedor. */
    private List<Mensaje> historialParaElAsistente(List<InteraccionChatbot> previas) {
        int total = previas.size();
        List<InteraccionChatbot> recientes = total > maxIntercambiosHistorial
                ? previas.subList(total - maxIntercambiosHistorial, total)
                : previas;

        List<Mensaje> historial = new ArrayList<>();
        for (InteraccionChatbot interaccion : recientes) {
            historial.add(new Mensaje(Rol.ESTUDIANTE, interaccion.getMensajeEstudiante()));
            historial.add(new Mensaje(Rol.ASISTENTE, interaccion.getRespuestaBot()));
        }
        return historial;
    }

    private Respuesta consultarAlAsistente(Solicitud solicitud, Long idPlanificacion) {
        Respuesta respuesta;
        try {
            respuesta = chatbotClient.responder(solicitud);
        } catch (ChatbotNoDisponibleException e) {
            log.warn("El asistente no respondió para la planificación {} (causa: {})", idPlanificacion, e.getCausa());
            throw e;
        } catch (RuntimeException e) {
            // Solo se registra el tipo de error: el mensaje podría contener texto del estudiante.
            log.error("Falla inesperada del asistente para la planificación {}: {}", idPlanificacion,
                    e.getClass().getSimpleName());
            throw new ChatbotNoDisponibleException(Causa.ERROR, "El asistente falló", e);
        }

        if (respuesta == null || respuesta.texto().isBlank()) {
            throw new ChatbotNoDisponibleException(Causa.ERROR, "El asistente devolvió una respuesta vacía");
        }
        return respuesta;
    }

    /**
     * Guarda las valoraciones del asistente. Ignora los códigos que no conoce y repite solo una vez cada
     * aspecto (la base de datos no admite dos hallazgos del mismo aspecto en una interacción).
     */
    private void agregarHallazgos(InteraccionChatbot interaccion, List<Hallazgo> hallazgos,
                                  List<CategoriaPlanificacion> categorias) {
        Map<String, CategoriaPlanificacion> porCodigo = categorias.stream()
                .collect(Collectors.toMap(c -> normalizar(c.getCodigo()), Function.identity()));
        Set<String> yaAgregados = new HashSet<>();

        for (Hallazgo hallazgo : hallazgos) {
            String codigo = normalizar(hallazgo.categoria());
            CategoriaPlanificacion categoria = porCodigo.get(codigo);
            if (categoria == null || !yaAgregados.add(codigo)) {
                continue;
            }
            interaccion.agregarHallazgo(categoria, hallazgo.nivel(), observacionLimpia(hallazgo.observacion()));
        }
    }

    private String normalizar(String codigo) {
        return codigo.strip().toUpperCase(Locale.ROOT);
    }

    private String observacionLimpia(String observacion) {
        if (observacion == null || observacion.isBlank()) {
            return null;
        }
        return recortar(observacion.strip(), MAX_CARACTERES_OBSERVACION);
    }

    private String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
