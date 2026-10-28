package com.grupo1.isw.practicas_pedagogicas.models;

/**
 * Ciclo de vida de una planificación:
 * BORRADOR -> ENVIADA -> APROBADA o RECHAZADA, y una RECHAZADA vuelve a ENVIADA al reenviarse.
 * Solo se puede editar (y conversar con el asistente) mientras es BORRADOR o RECHAZADA.
 */
public enum EstadoPlanificacion {
    BORRADOR,
    ENVIADA,
    APROBADA,
    RECHAZADA
}
