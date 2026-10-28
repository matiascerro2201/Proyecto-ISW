package com.grupo1.isw.practicas_pedagogicas.services.chatbot;

import com.grupo1.isw.practicas_pedagogicas.models.NivelHallazgo;

import java.util.List;
import java.util.Objects;

/**
 * Contrato entre la aplicación y el asistente de planificaciones. El servicio del chat solo conoce esta
 * interfaz: detrás puede haber el bot de prueba (ChatbotFalso) o un proveedor real, y se elige con la
 * propiedad chatbot.proveedor.
 *
 * <p>Lo que hace cada parte:
 * <ul>
 *   <li>El servicio arma las instrucciones completas (reglas, aspectos a valorar, formato de salida y el
 *       texto de la planificación) y el historial. El cliente no decide nada de eso.</li>
 *   <li>El cliente envía la solicitud al proveedor y devuelve la respuesta ya convertida a {@link Respuesta}.</li>
 *   <li>Si el proveedor no responde (límite de uso, tiempo agotado, caída), lanza
 *       {@link ChatbotNoDisponibleException}; el servicio decide qué mostrarle al estudiante.</li>
 *   <li>Un cliente no debe registrar en logs el contenido de las conversaciones: son textos de estudiantes.</li>
 * </ul>
 */
public interface ChatbotClient {

    Respuesta responder(Solicitud solicitud);

    enum Rol {
        ESTUDIANTE,
        ASISTENTE
    }

    /** Un mensaje de la conversación, de quien lo escribió. */
    record Mensaje(Rol rol, String contenido) {
        public Mensaje {
            Objects.requireNonNull(rol, "rol");
            Objects.requireNonNull(contenido, "contenido");
        }
    }

    /**
     * @param instrucciones instrucciones completas para el asistente, armadas por el servicio
     * @param categorias    códigos de los aspectos que debe valorar (un cliente real puede usarlos para
     *                      restringir el formato de la respuesta; el servicio ignora los desconocidos)
     * @param historial     mensajes anteriores en orden cronológico, sin incluir el nuevo
     * @param mensaje       lo que el estudiante acaba de escribir
     */
    record Solicitud(String instrucciones, List<String> categorias, List<Mensaje> historial, String mensaje) {
        public Solicitud {
            Objects.requireNonNull(instrucciones, "instrucciones");
            Objects.requireNonNull(mensaje, "mensaje");
            categorias = categorias == null ? List.of() : List.copyOf(categorias);
            historial = historial == null ? List.of() : List.copyOf(historial);
        }
    }

    /**
     * Valoración de un aspecto de la planificación.
     *
     * @param categoria   código del aspecto (por ejemplo, OBJETIVOS)
     * @param observacion frase breve, sin datos personales ni texto copiado del estudiante (máx. 300 caracteres)
     */
    record Hallazgo(String categoria, NivelHallazgo nivel, String observacion) {
        public Hallazgo {
            Objects.requireNonNull(categoria, "categoria");
            Objects.requireNonNull(nivel, "nivel");
        }
    }

    /**
     * @param texto     lo que se le muestra al estudiante
     * @param hallazgos valoración por aspecto; puede estar vacía si el proveedor no la entregó
     */
    record Respuesta(String texto, List<Hallazgo> hallazgos) {
        public Respuesta {
            Objects.requireNonNull(texto, "texto");
            hallazgos = hallazgos == null ? List.of() : List.copyOf(hallazgos);
        }
    }
}
