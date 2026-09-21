package com.grupo1.isw.practicas_pedagogicas.service;

import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;

import java.util.Objects;

/** Regla que el esquema SQL no garantiza: la pauta debe ser de la asignatura y del tipo de la línea. */
final class ReglasPauta {

    private ReglasPauta() {}

    static boolean aplicaALinea(PautaEvaluacion pauta, LineaPractica linea) {
        Long asignaturaDeLaLinea = linea.getInscripcion().getSeccion().getAsignatura().getIdAsignatura();
        boolean mismaAsignatura = Objects.equals(pauta.getAsignatura().getIdAsignatura(), asignaturaDeLaLinea);
        // String.valueOf funciona igual si tipo/tipoLinea son enum o String
        boolean mismoTipo = String.valueOf(pauta.getTipoLinea()).equals(String.valueOf(linea.getTipo()));
        return mismaAsignatura && mismoTipo;
    }
}
