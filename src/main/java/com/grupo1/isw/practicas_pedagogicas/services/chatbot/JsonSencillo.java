package com.grupo1.isw.practicas_pedagogicas.services.chatbot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector y escritor mínimo de JSON para hablar con el proveedor del asistente.
 *
 * <p>Se escribió aparte, en vez de usar una librería, para no depender de la versión de Jackson que trae
 * cada versión de Spring Boot. Es deliberadamente tolerante en dos cosas que los modelos hacen seguido:
 * escriben saltos de línea sin escapar dentro de los textos y dejan comas de más al final de una lista.
 *
 * <p>Convierte JSON en: Map (objeto), List (arreglo), String, BigDecimal (número), Boolean o null.
 * Los mensajes de error indican la posición, nunca copian el texto leído (puede ser de un estudiante).
 */
public final class JsonSencillo {

    private static final int PROFUNDIDAD_MAXIMA = 50;

    private final String texto;
    private int posicion;

    private JsonSencillo(String texto) {
        this.texto = texto;
    }

    /** @throws IllegalArgumentException si el texto no es JSON */
    public static Object parsear(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("No hay texto JSON que leer");
        }
        JsonSencillo lector = new JsonSencillo(texto);
        lector.saltarEspacios();
        Object valor = lector.leerValor(0);
        lector.saltarEspacios();
        if (lector.posicion < texto.length()) {
            throw lector.error("hay texto después del JSON");
        }
        return valor;
    }

    /** Devuelve el texto como un literal JSON, con sus comillas. */
    public static String escapar(String texto) {
        StringBuilder resultado = new StringBuilder(texto.length() + 2).append('"');
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"' -> resultado.append("\\\"");
                case '\\' -> resultado.append("\\\\");
                case '\n' -> resultado.append("\\n");
                case '\r' -> resultado.append("\\r");
                case '\t' -> resultado.append("\\t");
                case '\b' -> resultado.append("\\b");
                case '\f' -> resultado.append("\\f");
                default -> {
                    if (c < 0x20) {
                        resultado.append(String.format("\\u%04x", (int) c));
                    } else {
                        resultado.append(c);
                    }
                }
            }
        }
        return resultado.append('"').toString();
    }

    private Object leerValor(int profundidad) {
        if (profundidad > PROFUNDIDAD_MAXIMA) {
            throw error("está demasiado anidado");
        }
        if (posicion >= texto.length()) {
            throw error("termina antes de tiempo");
        }
        char c = texto.charAt(posicion);
        if (c == '{') {
            return leerObjeto(profundidad);
        }
        if (c == '[') {
            return leerArreglo(profundidad);
        }
        if (c == '"') {
            return leerCadena();
        }
        if (c == '-' || (c >= '0' && c <= '9')) {
            return leerNumero();
        }
        if (texto.startsWith("true", posicion)) {
            posicion += 4;
            return Boolean.TRUE;
        }
        if (texto.startsWith("false", posicion)) {
            posicion += 5;
            return Boolean.FALSE;
        }
        if (texto.startsWith("null", posicion)) {
            posicion += 4;
            return null;
        }
        throw error("carácter inesperado");
    }

    private Map<String, Object> leerObjeto(int profundidad) {
        Map<String, Object> objeto = new LinkedHashMap<>();
        posicion++;
        saltarEspacios();
        if (consumir('}')) {
            return objeto;
        }
        while (true) {
            saltarEspacios();
            if (posicion >= texto.length() || texto.charAt(posicion) != '"') {
                throw error("se esperaba el nombre de un campo");
            }
            String nombre = leerCadena();
            saltarEspacios();
            if (!consumir(':')) {
                throw error("se esperaba ':'");
            }
            saltarEspacios();
            objeto.put(nombre, leerValor(profundidad + 1));
            saltarEspacios();
            if (consumir(',')) {
                saltarEspacios();
                if (consumir('}')) {
                    return objeto;
                }
                continue;
            }
            if (consumir('}')) {
                return objeto;
            }
            throw error("se esperaba ',' o '}'");
        }
    }

    private List<Object> leerArreglo(int profundidad) {
        List<Object> lista = new ArrayList<>();
        posicion++;
        saltarEspacios();
        if (consumir(']')) {
            return lista;
        }
        while (true) {
            saltarEspacios();
            lista.add(leerValor(profundidad + 1));
            saltarEspacios();
            if (consumir(',')) {
                saltarEspacios();
                if (consumir(']')) {
                    return lista;
                }
                continue;
            }
            if (consumir(']')) {
                return lista;
            }
            throw error("se esperaba ',' o ']'");
        }
    }

    private String leerCadena() {
        StringBuilder cadena = new StringBuilder();
        posicion++;
        while (posicion < texto.length()) {
            char c = texto.charAt(posicion++);
            if (c == '"') {
                return cadena.toString();
            }
            if (c != '\\') {
                cadena.append(c);
                continue;
            }
            if (posicion >= texto.length()) {
                break;
            }
            char escapado = texto.charAt(posicion++);
            switch (escapado) {
                case '"', '\\', '/' -> cadena.append(escapado);
                case 'n' -> cadena.append('\n');
                case 'r' -> cadena.append('\r');
                case 't' -> cadena.append('\t');
                case 'b' -> cadena.append('\b');
                case 'f' -> cadena.append('\f');
                case 'u' -> cadena.append(leerUnicode());
                default -> throw error("secuencia de escape desconocida");
            }
        }
        throw error("un texto no se cerró");
    }

    private char leerUnicode() {
        if (posicion + 4 > texto.length()) {
            throw error("secuencia \\u incompleta");
        }
        int codigo = 0;
        for (int i = 0; i < 4; i++) {
            int digito = Character.digit(texto.charAt(posicion + i), 16);
            if (digito < 0) {
                throw error("secuencia \\u inválida");
            }
            codigo = codigo * 16 + digito;
        }
        posicion += 4;
        return (char) codigo;
    }

    private BigDecimal leerNumero() {
        int inicio = posicion;
        if (texto.charAt(posicion) == '-') {
            posicion++;
        }
        while (posicion < texto.length() && "0123456789.eE+-".indexOf(texto.charAt(posicion)) >= 0) {
            posicion++;
        }
        try {
            return new BigDecimal(texto.substring(inicio, posicion));
        } catch (NumberFormatException e) {
            throw error("número inválido");
        }
    }

    private void saltarEspacios() {
        while (posicion < texto.length() && Character.isWhitespace(texto.charAt(posicion))) {
            posicion++;
        }
    }

    private boolean consumir(char esperado) {
        if (posicion < texto.length() && texto.charAt(posicion) == esperado) {
            posicion++;
            return true;
        }
        return false;
    }

    private IllegalArgumentException error(String motivo) {
        return new IllegalArgumentException("JSON inválido en la posición " + posicion + ": " + motivo);
    }
}
