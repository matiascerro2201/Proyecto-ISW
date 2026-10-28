package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.models.NivelHallazgo;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Mensaje;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Respuesta;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Rol;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Solicitud;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException.Causa;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotOpenAiCompatible;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.JsonSencillo;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El cliente se prueba contra un servidor de mentira que levanta el propio JDK, así que se comprueba el
 * envío real: la dirección, las cabeceras, el cuerpo, los tiempos de espera y cada código de error.
 */
class ChatbotOpenAiCompatibleTest {

    private static final String CLAVE = "clave-secreta-de-prueba";
    private static final String MODELO = "modelo-de-prueba";
    private static final String SALIDA_VALIDA = "{\"respuesta\":\"Revisa tus objetivos\",\"hallazgos\":["
            + "{\"categoria\":\"OBJETIVOS\",\"nivel\":\"DEBIL\",\"observacion\":\"No son medibles\"},"
            + "{\"categoria\":\"EVALUACION\",\"nivel\":\"ADECUADO\",\"observacion\":\"Bien planteada\"}]}";

    private HttpServer servidor;
    private final AtomicReference<String> metodo = new AtomicReference<>();
    private final AtomicReference<String> ruta = new AtomicReference<>();
    private final AtomicReference<String> autorizacion = new AtomicReference<>();
    private final AtomicReference<String> tipoDeContenido = new AtomicReference<>();
    private final AtomicReference<String> cuerpo = new AtomicReference<>();
    private volatile int estado = 200;
    private volatile String respuesta = "";
    private volatile long demoraMs = 0;

    @BeforeEach
    void iniciarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", intercambio -> {
            metodo.set(intercambio.getRequestMethod());
            ruta.set(intercambio.getRequestURI().getPath());
            autorizacion.set(intercambio.getRequestHeaders().getFirst("Authorization"));
            tipoDeContenido.set(intercambio.getRequestHeaders().getFirst("Content-Type"));
            cuerpo.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (demoraMs > 0) {
                try {
                    Thread.sleep(demoraMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            intercambio.sendResponseHeaders(estado, bytes.length == 0 ? -1 : bytes.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(bytes);
            }
        });
        servidor.start();
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop(0);
    }

    // ------------------------------------------------------------------ ayudas

    private String urlBase() {
        return "http://localhost:" + servidor.getAddress().getPort() + "/v1/";
    }

    private ChatbotOpenAiCompatible cliente() {
        return new ChatbotOpenAiCompatible(urlBase(), MODELO, CLAVE, 10, false);
    }

    private Solicitud solicitud() {
        return new Solicitud("Instrucciones de prueba", List.of("OBJETIVOS", "EVALUACION"),
                List.of(new Mensaje(Rol.ESTUDIANTE, "pregunta 1"), new Mensaje(Rol.ASISTENTE, "respuesta 1")),
                "pregunta nueva");
    }

    /** La respuesta del proveedor en el formato de chat/completions, con este texto del modelo. */
    private String sobre(String textoDelModelo) {
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":"
                + JsonSencillo.escapar(textoDelModelo) + "}}]}";
    }

    private ChatbotNoDisponibleException falla(ChatbotOpenAiCompatible cliente) {
        return assertThrows(ChatbotNoDisponibleException.class, () -> cliente.responder(solicitud()));
    }

    // ---------------------------------------------------------- camino normal

    @Test
    @DisplayName("HU-26. Envía un POST a chat/completions con la clave y devuelve la respuesta con sus hallazgos")
    void caminoNormal() {
        respuesta = sobre(SALIDA_VALIDA);

        Respuesta resultado = cliente().responder(solicitud());

        assertEquals("POST", metodo.get());
        assertEquals("/v1/chat/completions", ruta.get());
        assertEquals("Bearer " + CLAVE, autorizacion.get());
        assertTrue(tipoDeContenido.get().startsWith("application/json"));
        assertEquals("Revisa tus objetivos", resultado.texto());
        assertEquals(2, resultado.hallazgos().size());
        assertEquals("OBJETIVOS", resultado.hallazgos().get(0).categoria());
        assertEquals(NivelHallazgo.DEBIL, resultado.hallazgos().get(0).nivel());
        assertEquals("Bien planteada", resultado.hallazgos().get(1).observacion());
    }

    @Test
    @DisplayName("HU-26. El cuerpo lleva el modelo y los mensajes en orden: instrucciones, historial y mensaje nuevo")
    void cuerpoDeLaPeticion() {
        respuesta = sobre(SALIDA_VALIDA);

        cliente().responder(solicitud());

        Map<?, ?> enviado = (Map<?, ?>) JsonSencillo.parsear(cuerpo.get());
        assertEquals(MODELO, enviado.get("model"));
        List<?> mensajes = (List<?>) enviado.get("messages");
        assertEquals(4, mensajes.size());
        String[] roles = {"system", "user", "assistant", "user"};
        String[] contenidos = {"Instrucciones de prueba", "pregunta 1", "respuesta 1", "pregunta nueva"};
        for (int i = 0; i < 4; i++) {
            Map<?, ?> mensaje = (Map<?, ?>) mensajes.get(i);
            assertEquals(roles[i], mensaje.get("role"));
            assertEquals(contenidos[i], mensaje.get("content"));
        }
    }

    @Test
    @DisplayName("HU-26. Comillas, saltos de línea, tildes y emojis viajan intactos en el cuerpo")
    void textosRarosViajanIntactos() {
        String raro = "Dijo \"hola\"\ny una barra \\ con ñandú ✓ \uD83D\uDE00\ty control \u0001";
        respuesta = sobre(SALIDA_VALIDA);

        cliente().responder(new Solicitud(raro, List.of(), List.of(), raro));

        List<?> mensajes = (List<?>) ((Map<?, ?>) JsonSencillo.parsear(cuerpo.get())).get("messages");
        assertEquals(raro, ((Map<?, ?>) mensajes.get(0)).get("content"));
        assertEquals(raro, ((Map<?, ?>) mensajes.get(1)).get("content"));
    }

    @Test
    @DisplayName("HU-26. La dirección base funciona con o sin barra final")
    void direccionBase() {
        respuesta = sobre(SALIDA_VALIDA);
        String sinBarra = "http://localhost:" + servidor.getAddress().getPort() + "/v1";
        String variasBarras = "http://localhost:" + servidor.getAddress().getPort() + "/v1///";

        new ChatbotOpenAiCompatible(sinBarra, MODELO, CLAVE, 10, false).responder(solicitud());
        assertEquals("/v1/chat/completions", ruta.get());
        new ChatbotOpenAiCompatible(variasBarras, MODELO, CLAVE, 10, false).responder(solicitud());
        assertEquals("/v1/chat/completions", ruta.get());
    }

    // ------------------------------------------------- lo que devuelve el modelo

    @Test
    @DisplayName("HU-26. Si el modelo envuelve el JSON en un bloque de código, igual se interpreta")
    void salidaConBloqueDeCodigo() {
        respuesta = sobre("```json\n" + SALIDA_VALIDA + "\n```");

        assertEquals(2, cliente().responder(solicitud()).hallazgos().size());
    }

    @Test
    @DisplayName("HU-26. Si el modelo no respeta el formato, se usa su texto tal cual y sin hallazgos")
    void salidaSinFormato() {
        respuesta = sobre("  Te recomiendo revisar la claridad de tus objetivos.  ");

        Respuesta resultado = cliente().responder(solicitud());

        assertEquals("Te recomiendo revisar la claridad de tus objetivos.", resultado.texto());
        assertTrue(resultado.hallazgos().isEmpty());
    }

    // ------------------------------------------------------ fallos del proveedor

    @Test
    @DisplayName("HU-26. Cada código de error del proveedor se traduce a su causa")
    void codigosDeError() {
        int[] codigos = {429, 408, 504, 401, 403, 400, 404, 500, 502, 503};
        Causa[] esperadas = {Causa.LIMITE_DEL_PROVEEDOR, Causa.TIEMPO_AGOTADO, Causa.TIEMPO_AGOTADO, Causa.ERROR,
                Causa.ERROR, Causa.ERROR, Causa.ERROR, Causa.ERROR, Causa.ERROR, Causa.ERROR};
        respuesta = "{\"error\":{\"message\":\"detalle del proveedor\"}}";

        for (int i = 0; i < codigos.length; i++) {
            estado = codigos[i];
            assertEquals(esperadas[i], falla(cliente()).getCausa(), "código " + codigos[i]);
        }
    }

    @Test
    @DisplayName("HU-26. Un error con el cuerpo vacío o que no es JSON también se traduce, sin lanzar otra cosa")
    void errorSinCuerpoUtilizable() {
        estado = 500;
        respuesta = "";
        assertEquals(Causa.ERROR, falla(cliente()).getCausa());

        respuesta = "<html>Bad Gateway</html>";
        assertEquals(Causa.ERROR, falla(cliente()).getCausa());

        respuesta = "[{\"error\":{\"message\":\"el error viene dentro de una lista\"}}]";
        assertEquals(Causa.ERROR, falla(cliente()).getCausa());
    }

    @Test
    @DisplayName("HU-26. Si el proveedor tarda más que el tiempo de espera, es TIEMPO_AGOTADO")
    void tiempoDeEsperaAgotado() {
        respuesta = sobre(SALIDA_VALIDA);
        demoraMs = 2500;
        ChatbotOpenAiCompatible conUnSegundo = new ChatbotOpenAiCompatible(urlBase(), MODELO, CLAVE, 1, false);

        long inicio = System.currentTimeMillis();
        ChatbotNoDisponibleException ex = falla(conUnSegundo);
        long transcurrido = System.currentTimeMillis() - inicio;

        assertEquals(Causa.TIEMPO_AGOTADO, ex.getCausa());
        assertTrue(transcurrido < 2400, "debía cortar a cerca de 1 s y tardó " + transcurrido + " ms");
    }

    @Test
    @DisplayName("HU-26. Si no se puede conectar con el proveedor, es ERROR")
    void sinConexion() throws IOException {
        int puertoCerrado;
        try (ServerSocket reserva = new ServerSocket(0)) {
            puertoCerrado = reserva.getLocalPort();
        }
        ChatbotOpenAiCompatible sinServidor =
                new ChatbotOpenAiCompatible("http://localhost:" + puertoCerrado + "/v1/", MODELO, CLAVE, 5, false);

        assertEquals(Causa.ERROR, falla(sinServidor).getCausa());
    }

    @Test
    @DisplayName("HU-26. Una respuesta exitosa sin texto utilizable es ERROR")
    void respuestaExitosaSinTexto() {
        String[] cuerpos = {"{\"id\":\"x\"}", "{\"choices\":[]}", "{\"choices\":[{\"message\":{}}]}",
                sobre("   "), "<html>no es JSON</html>", "", "{\"choices\":[{\"message\":{\"content\":null}}]}"};

        for (String cuerpoDeRespuesta : cuerpos) {
            respuesta = cuerpoDeRespuesta;
            assertEquals(Causa.ERROR, falla(cliente()).getCausa(), "cuerpo: " + cuerpoDeRespuesta);
        }
    }

    @Test
    @DisplayName("HU-26. La clave no aparece en los mensajes de error")
    void laClaveNoSeFiltra() {
        estado = 401;
        respuesta = "{\"error\":{\"message\":\"clave inválida\"}}";

        ChatbotNoDisponibleException ex = falla(cliente());

        assertFalse(ex.getMessage().contains(CLAVE));
        assertFalse(ex.getMessage().contains("Bearer"));
    }

    @Test
    @DisplayName("HU-26. Lee el consumo de tokens de la respuesta, y es null si el proveedor no lo informa")
    void consumoDeTokens() {
        assertEquals(Integer.valueOf(1234), ChatbotOpenAiCompatible.tokensUsados(
                "{\"choices\":[],\"usage\":{\"prompt_tokens\":1000,\"total_tokens\":1234}}"));
        assertNull(ChatbotOpenAiCompatible.tokensUsados("{\"choices\":[]}"));
        assertNull(ChatbotOpenAiCompatible.tokensUsados("{\"usage\":{\"total_tokens\":\"muchos\"}}"));
        assertNull(ChatbotOpenAiCompatible.tokensUsados("{\"usage\":null}"));
        assertNull(ChatbotOpenAiCompatible.tokensUsados("<html>no es JSON</html>"));
    }

    // -------------------------------------------------------------- configuración

    @Test
    @DisplayName("HU-26. Sin clave, dirección o modelo, la aplicación no arranca y dice qué falta")
    void configuracionIncompleta() {
        IllegalStateException sinClave = assertThrows(IllegalStateException.class,
                () -> new ChatbotOpenAiCompatible(urlBase(), MODELO, "  ", 10, false));
        assertTrue(sinClave.getMessage().contains("CHATBOT_API_KEY"));

        IllegalStateException sinClaveNula = assertThrows(IllegalStateException.class,
                () -> new ChatbotOpenAiCompatible(urlBase(), MODELO, null, 10, false));
        assertTrue(sinClaveNula.getMessage().contains("CHATBOT_API_KEY"));

        IllegalStateException sinUrl = assertThrows(IllegalStateException.class,
                () -> new ChatbotOpenAiCompatible("", MODELO, CLAVE, 10, false));
        assertTrue(sinUrl.getMessage().contains("chatbot.url-base"));

        IllegalStateException sinModelo = assertThrows(IllegalStateException.class,
                () -> new ChatbotOpenAiCompatible(urlBase(), " ", CLAVE, 10, false));
        assertTrue(sinModelo.getMessage().contains("chatbot.modelo"));

        IllegalStateException sinTiempo = assertThrows(IllegalStateException.class,
                () -> new ChatbotOpenAiCompatible(urlBase(), MODELO, CLAVE, 0, false));
        assertTrue(sinTiempo.getMessage().contains("tiempo-espera"));
    }
}
