package com.grupo1.isw.practicas_pedagogicas.models;

/**
 * Qué pidió el estudiante al asistente. Las revisiones tienen un tope por planificación (HU-30);
 * las preguntas solo cuentan para el límite diario de mensajes.
 */
public enum TipoInteraccion {
    /** Revisión general de la planificación, pedida con el botón. */
    REVISION,
    /** Mensaje normal: una duda o aclaración. */
    PREGUNTA
}
