package com.grupo1.isw.practicas_pedagogicas.services.chatbot;

import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException.Causa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Cliente para cualquier proveedor que acepte el formato de la API de OpenAI (chat/completions): Gemini,
 * OpenAI, Groq, Ollama, etc. Cambiar de proveedor es cambiar la URL, el modelo y la clave, sin tocar código.
 *
 * <p>Se activa con chatbot.proveedor=openai-compatible. La clave nunca va en un archivo del proyecto:
 * se lee de la variable de entorno CHATBOT_API_KEY (o de tu .env, que no se sube al repositorio).
 *
 * <p>No registra el contenido de las conversaciones. La excepción es chatbot.registrar-respuestas=true,
 * pensada solo para ajustar las instrucciones en desarrollo con planificaciones de ejemplo: deja en el log
 * lo que devuelve el modelo.
 */
@Component
@ConditionalOnProperty(name = "chatbot.proveedor", havingValue = "openai-compatible")
public class ChatbotOpenAiCompatible implements ChatbotClient {
    private static final Logger log = LoggerFactory.getLogger(ChatbotOpenAiCompatible.class);

    private static final Duration TIEMPO_PARA_CONECTAR = Duration.ofSeconds(10);
    private static final int MAX_CARACTERES_DETALLE_DEL_PROVEEDOR = 200;

    private final HttpClient http;
    private final URI endpoint;
    private final String modelo;
    private final String clave;
    private final Duration tiempoDeEspera;
    private final boolean registrarRespuestas;

    public ChatbotOpenAiCompatible(@Value("${chatbot.url-base:}") String urlBase,
                                   @Value("${chatbot.modelo:}") String modelo,
                                   @Value("${CHATBOT_API_KEY:}") String clave,
                                   @Value("${chatbot.tiempo-espera-segundos:30}") int tiempoDeEsperaSegundos,
                                   @Value("${chatbot.registrar-respuestas:false}") boolean registrarRespuestas) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalStateException("Falta la clave del asistente: define la variable de entorno "
                    + "CHATBOT_API_KEY (o ponla en tu archivo .env)");
        }
        if (urlBase == null || urlBase.isBlank()) {
            throw new IllegalStateException("Falta la propiedad chatbot.url-base (la dirección del proveedor)");
        }
        if (modelo == null || modelo.isBlank()) {
            throw new IllegalStateException("Falta la propiedad chatbot.modelo (el nombre del modelo)");
        }
        if (tiempoDeEsperaSegundos < 1) {
            throw new IllegalStateException("chatbot.tiempo-espera-segundos debe ser al menos 1");
        }

        this.endpoint = URI.create(urlBase.strip().replaceAll("/+$", "") + "/chat/completions");
        this.modelo = modelo.strip();
        this.clave = clave.strip();
        this.tiempoDeEspera = Duration.ofSeconds(tiempoDeEsperaSegundos);
        this.registrarRespuestas = registrarRespuestas;
        this.http = HttpClient.newBuilder().connectTimeout(TIEMPO_PARA_CONECTAR).build();

        // Sin la clave: solo para confirmar qué quedó configurado.
        log.info("Asistente configurado: proveedor compatible con OpenAI en {}, modelo {}, tiempo de espera {} s",
                this.endpoint.getHost(), this.modelo, tiempoDeEsperaSegundos);
    }

    @Override
    public Respuesta responder(Solicitud solicitud) {
        HttpRequest peticion = HttpRequest.newBuilder(endpoint)
                .timeout(tiempoDeEspera)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + clave)
                .POST(HttpRequest.BodyPublishers.ofString(construirCuerpo(solicitud), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> respuestaHttp = enviar(peticion);
        verificarEstado(respuestaHttp);
        String salida = extraerTexto(respuestaHttp.body());

        // Sirve para calibrar los límites diarios con el consumo real; no incluye nada del contenido.
        Integer tokens = tokensUsados(respuestaHttp.body());
        if (tokens != null) {
            log.info("Consulta al asistente: {} tokens en total", tokens);
        }

        if (registrarRespuestas) {
            log.info("[depuración] salida del asistente: {}", salida);
        }
        return InterpreteRespuesta.interpretar(salida).orElseGet(() -> {
            log.warn("El asistente no devolvió el formato esperado (JSON con respuesta y hallazgos); "
                    + "se usa su texto tal cual, sin valoraciones");
            return new Respuesta(salida.strip(), List.of());
        });
    }

    private String construirCuerpo(Solicitud solicitud) {
        StringBuilder cuerpo = new StringBuilder("{\"model\":").append(JsonSencillo.escapar(modelo))
                .append(",\"messages\":[").append(mensaje("system", solicitud.instrucciones()));
        for (Mensaje anterior : solicitud.historial()) {
            cuerpo.append(',').append(mensaje(anterior.rol() == Rol.ESTUDIANTE ? "user" : "assistant",
                    anterior.contenido()));
        }
        return cuerpo.append(',').append(mensaje("user", solicitud.mensaje())).append("]}").toString();
    }

    private String mensaje(String rol, String contenido) {
        return "{\"role\":" + JsonSencillo.escapar(rol) + ",\"content\":" + JsonSencillo.escapar(contenido) + "}";
    }

    private HttpResponse<String> enviar(HttpRequest peticion) {
        try {
            return http.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new ChatbotNoDisponibleException(Causa.TIEMPO_AGOTADO, "El proveedor no respondió a tiempo", e);
        } catch (IOException e) {
            throw new ChatbotNoDisponibleException(Causa.ERROR,
                    "No se pudo conectar con el proveedor (" + e.getClass().getSimpleName() + ")", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChatbotNoDisponibleException(Causa.ERROR, "La consulta al proveedor fue interrumpida", e);
        }
    }

    private void verificarEstado(HttpResponse<String> respuesta) {
        int estado = respuesta.statusCode();
        if (estado >= 200 && estado < 300) {
            return;
        }
        if (estado == 429) {
            throw new ChatbotNoDisponibleException(Causa.LIMITE_DEL_PROVEEDOR,
                    "El proveedor rechazó la consulta por exceso de uso (429)");
        }
        if (estado == 408 || estado == 504) {
            throw new ChatbotNoDisponibleException(Causa.TIEMPO_AGOTADO,
                    "El proveedor no respondió a tiempo (" + estado + ")");
        }

        String detalle = detalleDelProveedor(respuesta.body());
        if (estado == 401 || estado == 403) {
            log.error("El proveedor rechazó las credenciales ({}). Revisa CHATBOT_API_KEY y el acceso al modelo. {}",
                    estado, detalle);
        } else {
            log.warn("El proveedor respondió con el código {}: {}", estado, detalle);
        }
        throw new ChatbotNoDisponibleException(Causa.ERROR, "El proveedor respondió con el código " + estado);
    }

    /** choices[0].message.content de la respuesta del proveedor. */
    private String extraerTexto(String cuerpo) {
        try {
            if (JsonSencillo.parsear(cuerpo) instanceof Map<?, ?> raiz
                    && raiz.get("choices") instanceof List<?> opciones && !opciones.isEmpty()
                    && opciones.get(0) instanceof Map<?, ?> primera
                    && primera.get("message") instanceof Map<?, ?> mensaje
                    && mensaje.get("content") instanceof String contenido
                    && !contenido.isBlank()) {
                return contenido;
            }
        } catch (IllegalArgumentException e) {
            throw new ChatbotNoDisponibleException(Causa.ERROR, "La respuesta del proveedor no es JSON válido", e);
        }
        throw new ChatbotNoDisponibleException(Causa.ERROR, "La respuesta del proveedor no trae texto");
    }

    /** usage.total_tokens de la respuesta del proveedor; null si no lo trae o no se puede leer. */
    public static Integer tokensUsados(String cuerpo) {
        try {
            if (JsonSencillo.parsear(cuerpo) instanceof Map<?, ?> raiz
                    && raiz.get("usage") instanceof Map<?, ?> uso
                    && uso.get("total_tokens") instanceof java.math.BigDecimal total) {
                return total.intValue();
            }
        } catch (IllegalArgumentException e) {
            // Sin dato de consumo.
        }
        return null;
    }

    /** El mensaje de error que da el proveedor (por ejemplo, un nombre de modelo inexistente), acortado. */
    private String detalleDelProveedor(String cuerpo) {
        try {
            Object raiz = JsonSencillo.parsear(cuerpo);
            if (raiz instanceof List<?> lista && !lista.isEmpty()) {
                raiz = lista.get(0);
            }
            if (raiz instanceof Map<?, ?> objeto && objeto.get("error") instanceof Map<?, ?> error
                    && error.get("message") instanceof String mensaje) {
                String uno = mensaje.replaceAll("\\s+", " ").strip();
                return uno.length() <= MAX_CARACTERES_DETALLE_DEL_PROVEEDOR
                        ? uno : uno.substring(0, MAX_CARACTERES_DETALLE_DEL_PROVEEDOR);
            }
        } catch (IllegalArgumentException e) {
            // Sin detalle utilizable.
        }
        return "(sin detalle)";
    }
}
