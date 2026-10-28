package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.services.chatbot.JsonSencillo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSencilloTest {

    private Map<?, ?> objeto(String json) {
        return (Map<?, ?>) JsonSencillo.parsear(json);
    }

    @Test
    @DisplayName("HU-26. Lee un objeto con todos los tipos de valor")
    void objetoConTodosLosTipos() {
        Map<?, ?> m = objeto("{\"texto\":\"hola\",\"decimal\":12.5,\"entero\":-3,\"si\":true,\"no\":false,"
                + "\"nada\":null,\"lista\":[1,\"dos\",[3]],\"anidado\":{\"a\":1}}");

        assertEquals("hola", m.get("texto"));
        assertEquals(0, new BigDecimal("12.5").compareTo((BigDecimal) m.get("decimal")));
        assertEquals(0, new BigDecimal("-3").compareTo((BigDecimal) m.get("entero")));
        assertEquals(Boolean.TRUE, m.get("si"));
        assertEquals(Boolean.FALSE, m.get("no"));
        assertTrue(m.containsKey("nada"));
        assertNull(m.get("nada"));
        List<?> lista = (List<?>) m.get("lista");
        assertEquals(3, lista.size());
        assertEquals("dos", lista.get(1));
        assertEquals(1, ((List<?>) lista.get(2)).size());
        assertEquals(0, BigDecimal.ONE.compareTo((BigDecimal) ((Map<?, ?>) m.get("anidado")).get("a")));
    }

    @Test
    @DisplayName("HU-26. Acepta espacios y saltos de línea entre los elementos")
    void espaciosEntreElementos() {
        Map<?, ?> m = objeto("  {\n  \"a\" : 1 ,\n  \"b\" :\t[ 1 , 2 ]\n}  ");

        assertEquals(2, m.size());
        assertEquals(2, ((List<?>) m.get("b")).size());
    }

    @Test
    @DisplayName("HU-26. Interpreta los escapes, incluidos \\u y los pares de sustitución (emojis)")
    void escapes() {
        Map<?, ?> m = objeto("{\"t\":\"a\\\"b\\\\c\\/d\\n\\t\\u00f1\\ud83d\\ude00\"}");

        assertEquals("a\"b\\c/d\n\tñ\uD83D\uDE00", m.get("t"));
    }

    @Test
    @DisplayName("HU-26. Acepta saltos de línea sin escapar dentro de un texto, que es lo que escriben los modelos")
    void saltosDeLineaSinEscapar() {
        assertEquals("línea 1\nlínea 2", objeto("{\"r\":\"línea 1\nlínea 2\"}").get("r"));
    }

    @Test
    @DisplayName("HU-26. Tolera una coma de más al final de un objeto o una lista")
    void comasFinales() {
        assertEquals(1, objeto("{\"a\":1,}").size());
        assertEquals(2, ((List<?>) objeto("{\"l\":[1,2,]}").get("l")).size());
    }

    @Test
    @DisplayName("HU-26. Lee objetos y listas vacíos")
    void vacios() {
        assertTrue(objeto("{}").isEmpty());
        assertTrue(((List<?>) JsonSencillo.parsear("[]")).isEmpty());
    }

    @Test
    @DisplayName("HU-26. Rechaza lo que no es JSON con IllegalArgument")
    void entradasInvalidas() {
        String[] invalidas = {"", "   ", "{", "{\"a\"}", "{\"a\":}", "[1,2", "\"sin cerrar", "{\"a\":1} basura",
                "nulo", "{\"a\":\"\\x\"}", "{\"a\":\"\\u12\"}", "{\"a\":\"\\uZZZZ\"}", "-", "{a:1}"};

        for (String entrada : invalidas) {
            assertThrows(IllegalArgumentException.class, () -> JsonSencillo.parsear(entrada),
                    "debía rechazar: [" + entrada + "]");
        }
        assertThrows(IllegalArgumentException.class, () -> JsonSencillo.parsear(null));
    }

    @Test
    @DisplayName("HU-26. Limita la profundidad para que un texto malicioso no agote la memoria de la aplicación")
    void profundidadMaxima() {
        String muyAnidado = "[".repeat(100) + "]".repeat(100);
        String razonable = "[".repeat(40) + "]".repeat(40);

        assertThrows(IllegalArgumentException.class, () -> JsonSencillo.parsear(muyAnidado));
        assertEquals(1, ((List<?>) JsonSencillo.parsear(razonable)).size());
    }

    @Test
    @DisplayName("HU-26. El mensaje de error dice la posición pero no copia el texto leído")
    void elErrorNoCopiaElTexto() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> JsonSencillo.parsear("{\"secreto\": clave-del-estudiante}"));

        assertTrue(ex.getMessage().contains("posición"));
        assertFalse(ex.getMessage().contains("secreto"));
        assertFalse(ex.getMessage().contains("clave-del-estudiante"));
    }

    @Test
    @DisplayName("HU-26. Escapar produce un literal JSON con comillas y escapes")
    void escapar() {
        assertEquals("\"hola\"", JsonSencillo.escapar("hola"));
        assertEquals("\"a\\\"b\\\\c\\nd\\te\"", JsonSencillo.escapar("a\"b\\c\nd\te"));
        assertEquals("\"\\u0001\"", JsonSencillo.escapar("\u0001"));
        assertEquals("\"ñandú ✓ \uD83D\uDE00\"", JsonSencillo.escapar("ñandú ✓ \uD83D\uDE00"));
    }

    @Test
    @DisplayName("HU-26. Lo que se escapa se lee de vuelta idéntico")
    void idaYVuelta() {
        String[] textos = {"", "simple", "con \"comillas\" y \\ barras", "líneas\nde\r\ntexto\ty tabs",
                "control \u0001\u001f", "acentos áéíóú ñ ü", "emoji \uD83D\uDE00 y símbolos ✓ — «»",
                "{\"parece\": \"json\"}"};

        for (String texto : textos) {
            assertEquals(texto, JsonSencillo.parsear(JsonSencillo.escapar(texto)), "ida y vuelta de: [" + texto + "]");
        }
    }
}
