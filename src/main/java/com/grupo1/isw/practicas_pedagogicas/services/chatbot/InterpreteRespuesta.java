package com.grupo1.isw.practicas_pedagogicas.services.chatbot;

import com.grupo1.isw.practicas_pedagogicas.models.NivelHallazgo;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Hallazgo;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Respuesta;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Convierte lo que escribió el modelo en una {@link Respuesta}. Las instrucciones le piden un JSON con la
 * respuesta para el estudiante y una valoración por aspecto, pero un modelo puede envolverlo en un bloque
 * de código, anteponerle una frase, escribir el nivel con tilde o saltarse un campo. Aquí se tolera todo eso;
 * lo que no se puede interpretar se descarta sin lanzar errores.
 */
public final class InterpreteRespuesta {

    private InterpreteRespuesta() {
    }

    /** Vacío si el texto no trae un JSON con un campo "respuesta" utilizable. */
    public static Optional<Respuesta> interpretar(String salidaDelModelo) {
        String json = extraerObjeto(salidaDelModelo);
        if (json == null) {
            return Optional.empty();
        }

        Object valor;
        try {
            valor = JsonSencillo.parsear(json);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        if (!(valor instanceof Map<?, ?> objeto)) {
            return Optional.empty();
        }
        if (!(objeto.get("respuesta") instanceof String texto) || texto.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Respuesta(texto.strip(), hallazgos(objeto.get("hallazgos"))));
    }

    /** Del primer { al último }: descarta bloques de código y frases antes o después del JSON. */
    private static String extraerObjeto(String salida) {
        if (salida == null) {
            return null;
        }
        int inicio = salida.indexOf('{');
        int fin = salida.lastIndexOf('}');
        if (inicio < 0 || fin <= inicio) {
            return null;
        }
        return salida.substring(inicio, fin + 1);
    }

    private static List<Hallazgo> hallazgos(Object valor) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        if (!(valor instanceof List<?> elementos)) {
            return hallazgos;
        }
        for (Object elemento : elementos) {
            if (!(elemento instanceof Map<?, ?> campos)) {
                continue;
            }
            if (!(campos.get("categoria") instanceof String categoria) || categoria.isBlank()) {
                continue;
            }
            NivelHallazgo nivel = nivel(campos.get("nivel"));
            if (nivel == null) {
                continue;
            }
            String observacion = campos.get("observacion") instanceof String texto ? texto : null;
            hallazgos.add(new Hallazgo(categoria.strip(), nivel, observacion));
        }
        return hallazgos;
    }

    /** Acepta DEBIL, DÉBIL, débil, adecuado, etc.; cualquier otro valor se descarta. */
    private static NivelHallazgo nivel(Object valor) {
        if (!(valor instanceof String texto)) {
            return null;
        }
        String normalizado = Normalizer.normalize(texto.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        return switch (normalizado) {
            case "DEBIL" -> NivelHallazgo.DEBIL;
            case "ADECUADO" -> NivelHallazgo.ADECUADO;
            default -> null;
        };
    }
}
