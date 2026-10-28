package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException.Causa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatbotNoDisponibleExceptionTest {

    @ParameterizedTest(name = "HU-26. La causa {0} trae un mensaje para el estudiante que le dice que no perdió su texto")
    @EnumSource(Causa.class)
    void cadaCausa_tieneMensajeParaElEstudiante(Causa causa) {
        String mensaje = causa.getMensajeParaElEstudiante();

        assertFalse(mensaje.isBlank());
        // Es cierto porque el servicio no guarda nada cuando el asistente falla y la pantalla conserva el texto.
        assertTrue(mensaje.contains("Tu mensaje no se perdió"), mensaje);
    }

    @Test
    @DisplayName("HU-26. Cada causa tiene un mensaje distinto, para que el estudiante sepa qué hacer")
    void losMensajesSonDistintos() {
        long distintos = new HashSet<>(Arrays.stream(Causa.values())
                .map(Causa::getMensajeParaElEstudiante).toList()).size();

        assertEquals(Causa.values().length, distintos);
    }

    @ParameterizedTest(name = "HU-26. La causa {0} trae un mensaje para el profesor, sin hablar de mensajes de estudiantes")
    @EnumSource(Causa.class)
    void cadaCausa_tieneMensajeParaElProfesor(Causa causa) {
        String mensaje = causa.getMensajeParaElProfesor();

        assertFalse(mensaje.isBlank());
        assertFalse(mensaje.contains("Tu mensaje"), mensaje);
    }

    @Test
    @DisplayName("HU-26. La excepción expone su causa, su mensaje y el error de origen")
    void exponeCausaMensajeYOrigen() {
        RuntimeException origen = new RuntimeException("falla de red");

        ChatbotNoDisponibleException ex = new ChatbotNoDisponibleException(Causa.TIEMPO_AGOTADO, "sin respuesta", origen);

        assertEquals(Causa.TIEMPO_AGOTADO, ex.getCausa());
        assertEquals("sin respuesta", ex.getMessage());
        assertSame(origen, ex.getCause());
    }

    @Test
    @DisplayName("HU-26. Sin error de origen, getCause es null")
    void sinOrigen() {
        ChatbotNoDisponibleException ex = new ChatbotNoDisponibleException(Causa.ERROR, "x");

        assertEquals(Causa.ERROR, ex.getCausa());
        assertNull(ex.getCause());
    }
}
