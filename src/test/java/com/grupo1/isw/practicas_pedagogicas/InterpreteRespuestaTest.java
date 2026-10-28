package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.models.NivelHallazgo;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Hallazgo;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Respuesta;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.InterpreteRespuesta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterpreteRespuestaTest {

    private static final String VALIDO = "{\"respuesta\":\"Revisa tus objetivos\",\"hallazgos\":["
            + "{\"categoria\":\"OBJETIVOS\",\"nivel\":\"DEBIL\",\"observacion\":\"No son medibles\"},"
            + "{\"categoria\":\"EVALUACION\",\"nivel\":\"ADECUADO\",\"observacion\":\"Bien planteada\"}]}";

    private Respuesta interpretar(String salida) {
        Optional<Respuesta> respuesta = InterpreteRespuesta.interpretar(salida);
        assertTrue(respuesta.isPresent(), "debía interpretarse: " + salida);
        return respuesta.get();
    }

    @Test
    @DisplayName("HU-26. Interpreta el JSON con la forma pedida")
    void jsonValido() {
        Respuesta respuesta = interpretar(VALIDO);

        assertEquals("Revisa tus objetivos", respuesta.texto());
        assertEquals(2, respuesta.hallazgos().size());
        Hallazgo primero = respuesta.hallazgos().get(0);
        assertEquals("OBJETIVOS", primero.categoria());
        assertEquals(NivelHallazgo.DEBIL, primero.nivel());
        assertEquals("No son medibles", primero.observacion());
        assertEquals(NivelHallazgo.ADECUADO, respuesta.hallazgos().get(1).nivel());
    }

    @Test
    @DisplayName("HU-26. Tolera que el modelo envuelva el JSON en un bloque de código")
    void conBloqueDeCodigo() {
        assertEquals(2, interpretar("```json\n" + VALIDO + "\n```").hallazgos().size());
    }

    @Test
    @DisplayName("HU-26. Tolera frases antes y después del JSON")
    void conTextoAlrededor() {
        Respuesta respuesta = interpretar("Claro, aquí está mi revisión:\n" + VALIDO + "\nEspero que sirva.");

        assertEquals("Revisa tus objetivos", respuesta.texto());
        assertEquals(2, respuesta.hallazgos().size());
    }

    @Test
    @DisplayName("HU-26. Acepta el nivel con tilde, en minúsculas o con espacios")
    void nivelesConVariaciones() {
        Respuesta respuesta = interpretar("{\"respuesta\":\"ok\",\"hallazgos\":["
                + "{\"categoria\":\"A\",\"nivel\":\"Débil\"},{\"categoria\":\"B\",\"nivel\":\" adecuado \"},"
                + "{\"categoria\":\"C\",\"nivel\":\"DÉBIL\"}]}");

        assertEquals(3, respuesta.hallazgos().size());
        assertEquals(NivelHallazgo.DEBIL, respuesta.hallazgos().get(0).nivel());
        assertEquals(NivelHallazgo.ADECUADO, respuesta.hallazgos().get(1).nivel());
        assertEquals(NivelHallazgo.DEBIL, respuesta.hallazgos().get(2).nivel());
    }

    @Test
    @DisplayName("HU-26. Descarta los hallazgos inutilizables sin perder los buenos")
    void hallazgosInutilizables() {
        Respuesta respuesta = interpretar("{\"respuesta\":\"ok\",\"hallazgos\":["
                + "{\"categoria\":\"A\",\"nivel\":\"REGULAR\"},"        // nivel desconocido
                + "{\"categoria\":\"B\",\"nivel\":3},"                  // nivel que no es texto
                + "{\"categoria\":\"\",\"nivel\":\"DEBIL\"},"           // categoría vacía
                + "{\"nivel\":\"DEBIL\"},"                              // sin categoría
                + "\"no soy un objeto\","                               // elemento que no es un objeto
                + "{\"categoria\":\"BUENO\",\"nivel\":\"DEBIL\",\"observacion\":12}]}");  // observación que no es texto

        assertEquals(1, respuesta.hallazgos().size());
        assertEquals("BUENO", respuesta.hallazgos().get(0).categoria());
        assertNull(respuesta.hallazgos().get(0).observacion());
    }

    @Test
    @DisplayName("HU-26. Sin hallazgos, o con hallazgos que no son una lista, igual entrega la respuesta")
    void sinHallazgos() {
        assertTrue(interpretar("{\"respuesta\":\"solo texto\"}").hallazgos().isEmpty());
        assertTrue(interpretar("{\"respuesta\":\"solo texto\",\"hallazgos\":\"ninguno\"}").hallazgos().isEmpty());
        assertEquals("solo texto", interpretar("{\"respuesta\":\"solo texto\",\"hallazgos\":null}").texto());
    }

    @Test
    @DisplayName("HU-26. Sin un campo respuesta utilizable no interpreta nada")
    void sinRespuestaUtilizable() {
        String[] salidas = {"{\"hallazgos\":[]}", "{\"respuesta\":\"\"}", "{\"respuesta\":\"   \"}",
                "{\"respuesta\":42}", "{\"respuesta\":null}", "{\"respuesta\":[\"a\"]}"};

        for (String salida : salidas) {
            assertTrue(InterpreteRespuesta.interpretar(salida).isEmpty(), "no debía interpretarse: " + salida);
        }
    }

    @Test
    @DisplayName("HU-26. Lo que no es JSON, o es JSON roto, no interpreta nada y no lanza errores")
    void noEsJson() {
        String[] salidas = {null, "", "   ", "Te recomiendo revisar tus objetivos.", "[1, 2, 3]",
                "{\"respuesta\": \"se cortó", "{\"respuesta\": \"x\", ", "} al revés {", "{sin comillas: 1}"};

        for (String salida : salidas) {
            assertTrue(InterpreteRespuesta.interpretar(salida).isEmpty(), "no debía interpretarse: " + salida);
        }
    }

    @Test
    @DisplayName("HU-26. Conserva los saltos de línea y las llaves dentro de la respuesta")
    void saltosDeLineaYLlaves() {
        Respuesta respuesta = interpretar("{\"respuesta\":\"Primero:\n1) objetivos {claros}\n2) evaluación\","
                + "\"hallazgos\":[]}");

        assertEquals("Primero:\n1) objetivos {claros}\n2) evaluación", respuesta.texto());
    }

    @Test
    @DisplayName("HU-26. Quita los espacios sobrantes de la respuesta")
    void recortaEspacios() {
        assertEquals("texto", interpretar("{\"respuesta\":\"   texto \\n \"}").texto());
    }
}
