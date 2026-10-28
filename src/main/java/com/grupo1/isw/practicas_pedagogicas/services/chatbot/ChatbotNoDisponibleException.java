package com.grupo1.isw.practicas_pedagogicas.services.chatbot;

/** El asistente no pudo responder por un problema del proveedor o de la conexión, no del estudiante. */
public class ChatbotNoDisponibleException extends RuntimeException {

    public enum Causa {
        /** El proveedor rechazó la solicitud por exceso de uso (por ejemplo, el límite gratuito). */
        LIMITE_DEL_PROVEEDOR(
                "El asistente está recibiendo muchas consultas en este momento. "
                        + "Espera unos minutos e inténtalo de nuevo; si sigue sin responder, es posible que se haya "
                        + "agotado su cupo del día. Tu mensaje no se perdió.",
                "El asistente está recibiendo muchas consultas en este momento. "
                        + "Inténtalo de nuevo en unos minutos; si sigue igual, es posible que se haya agotado "
                        + "su cupo del día."),
        /** El proveedor no respondió dentro del tiempo permitido. */
        TIEMPO_AGOTADO(
                "El asistente tardó demasiado en responder. "
                        + "Inténtalo de nuevo en un momento. Tu mensaje no se perdió.",
                "El asistente tardó demasiado en responder. Inténtalo de nuevo en un momento."),
        /** Cualquier otro fallo: conexión, error del proveedor, respuesta inutilizable. */
        ERROR(
                "El asistente no está disponible en este momento. "
                        + "Inténtalo más tarde. Tu mensaje no se perdió.",
                "El asistente no está disponible en este momento. Inténtalo más tarde.");

        private final String mensajeParaElEstudiante;
        private final String mensajeParaElProfesor;

        Causa(String mensajeParaElEstudiante, String mensajeParaElProfesor) {
            this.mensajeParaElEstudiante = mensajeParaElEstudiante;
            this.mensajeParaElProfesor = mensajeParaElProfesor;
        }

        /** Lo que se le muestra al estudiante; su mensaje sigue en el cuadro de texto para reintentar. */
        public String getMensajeParaElEstudiante() {
            return mensajeParaElEstudiante;
        }

        /** Lo que se le muestra al profesor cuando no se pudo generar el análisis del curso. */
        public String getMensajeParaElProfesor() {
            return mensajeParaElProfesor;
        }
    }

    private final Causa causa;

    public ChatbotNoDisponibleException(Causa causa, String mensaje) {
        super(mensaje);
        this.causa = causa;
    }

    public ChatbotNoDisponibleException(Causa causa, String mensaje, Throwable origen) {
        super(mensaje, origen);
        this.causa = causa;
    }

    public Causa getCausa() {
        return causa;
    }
}
