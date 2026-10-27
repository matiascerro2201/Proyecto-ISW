package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.models.NivelHallazgo;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Hallazgo;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Mensaje;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Respuesta;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Rol;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Solicitud;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotFalso;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException.Causa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatbotFalsoTest {

    private static final List<String> CATEGORIAS = List.of("OBJETIVOS", "INICIO", "DESARROLLO");

    private final ChatbotFalso bot = new ChatbotFalso();

    private Solicitud solicitud(String mensaje, List<Mensaje> historial) {
        return new Solicitud("Instrucciones de prueba", CATEGORIAS, historial, mensaje);
    }

    private List<Mensaje> intercambios(int cantidad) {
        List<Mensaje> historial = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            historial.add(new Mensaje(Rol.ESTUDIANTE, "pregunta " + i));
            historial.add(new Mensaje(Rol.ASISTENTE, "respuesta " + i));
        }
        return historial;
    }

    private List<NivelHallazgo> niveles(Respuesta respuesta) {
        return respuesta.hallazgos().stream().map(Hallazgo::nivel).toList();
    }

    @Test
    @DisplayName("HU-26. En la primera consulta responde con texto y valora todos los aspectos como DEBIL")
    void primeraConsulta_todoDebil() {
        Respuesta respuesta = bot.responder(solicitud("Revisa mi planificación", List.of()));

        assertFalse(respuesta.texto().isBlank());
        assertEquals(List.of("OBJETIVOS", "INICIO", "DESARROLLO"),
                respuesta.hallazgos().stream().map(Hallazgo::categoria).toList());
        assertEquals(List.of(NivelHallazgo.DEBIL, NivelHallazgo.DEBIL, NivelHallazgo.DEBIL), niveles(respuesta));
    }

    @ParameterizedTest(name = "HU-26. Con {0} intercambios previos mejoran {1} aspectos")
    @CsvSource({"1,1", "2,2", "3,3", "10,3"})
    void conformeAvanzaLaConversacion_mejoranLosAspectos(int previos, int adecuados) {
        Respuesta respuesta = bot.responder(solicitud("Otra consulta", intercambios(previos)));

        long cantidadAdecuados = niveles(respuesta).stream().filter(n -> n == NivelHallazgo.ADECUADO).count();
        assertEquals(adecuados, cantidadAdecuados);
        // Los primeros aspectos de la lista son los que mejoran primero.
        assertEquals(NivelHallazgo.ADECUADO, respuesta.hallazgos().get(0).nivel());
    }

    @Test
    @DisplayName("HU-26. Con 2 intercambios previos: los dos primeros ADECUADOS y el último DEBIL")
    void dosIntercambiosPrevios_ordenDeMejora() {
        Respuesta respuesta = bot.responder(solicitud("Otra consulta", intercambios(2)));

        assertEquals(List.of(NivelHallazgo.ADECUADO, NivelHallazgo.ADECUADO, NivelHallazgo.DEBIL), niveles(respuesta));
    }

    @ParameterizedTest(name = "HU-26. El mensaje [{0}] provoca la falla {1}")
    @CsvSource({
            "[falla-limite], LIMITE_DEL_PROVEEDOR",
            "Hola [FALLA-TIEMPO] chao, TIEMPO_AGOTADO",
            "texto antes [Falla-Error], ERROR"
    })
    void palabrasDeFalla_provocanLaExcepcion(String mensaje, Causa causaEsperada) {
        ChatbotNoDisponibleException ex = assertThrows(ChatbotNoDisponibleException.class,
                () -> bot.responder(solicitud(mensaje, List.of())));

        assertEquals(causaEsperada, ex.getCausa());
    }

    @Test
    @DisplayName("HU-26. Un mensaje normal no provoca ninguna falla")
    void mensajeNormal_noFalla() {
        assertFalse(bot.responder(solicitud("Hola, ¿puedes revisar mis objetivos?", List.of())).texto().isEmpty());
    }

    @Test
    @DisplayName("HU-26. Sin aspectos que valorar, la respuesta no trae hallazgos")
    void sinCategorias_sinHallazgos() {
        Respuesta respuesta = bot.responder(new Solicitud("Instrucciones", List.of(), List.of(), "Hola"));

        assertTrue(respuesta.hallazgos().isEmpty());
        assertFalse(respuesta.texto().isBlank());
    }

    @Test
    @DisplayName("HU-26. Las observaciones caben en la columna de la base de datos (300 caracteres)")
    void observaciones_cabenEnLaColumna() {
        Respuesta respuesta = bot.responder(solicitud("Hola", intercambios(1)));

        assertTrue(respuesta.hallazgos().stream().allMatch(h -> h.observacion().length() <= 300));
    }

    @Test
    @DisplayName("HU-26. La solicitud trata las listas nulas como vacías y exige instrucciones y mensaje")
    void solicitud_nulosYObligatorios() {
        Solicitud sinListas = new Solicitud("Instrucciones", null, null, "Hola");

        assertTrue(sinListas.categorias().isEmpty());
        assertTrue(sinListas.historial().isEmpty());
        assertThrows(NullPointerException.class, () -> new Solicitud(null, List.of(), List.of(), "Hola"));
        assertThrows(NullPointerException.class, () -> new Solicitud("Instrucciones", List.of(), List.of(), null));
    }

    @Test
    @DisplayName("HU-26. La respuesta exige texto y trata los hallazgos nulos como vacíos")
    void respuesta_textoObligatorio() {
        assertTrue(new Respuesta("Texto", null).hallazgos().isEmpty());
        assertThrows(NullPointerException.class, () -> new Respuesta(null, List.of()));
    }
}
