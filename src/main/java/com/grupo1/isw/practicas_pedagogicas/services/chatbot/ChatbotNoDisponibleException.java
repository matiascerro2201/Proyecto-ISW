package com.grupo1.isw.practicas_pedagogicas.services.chatbot;

/** El asistente no pudo responder por un problema del proveedor o de la conexión, no del estudiante. */
public class ChatbotNoDisponibleException extends RuntimeException {

    public enum Causa {
        /** El proveedor rechazó la solicitud por exceso de uso (por ejemplo, el límite gratuito). */
        LIMITE_DEL_PROVEEDOR,
        /** El proveedor no respondió dentro del tiempo permitido. */
        TIEMPO_AGOTADO,
        /** Cualquier otro fallo: conexión, error del proveedor, respuesta inutilizable. */
        ERROR
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
