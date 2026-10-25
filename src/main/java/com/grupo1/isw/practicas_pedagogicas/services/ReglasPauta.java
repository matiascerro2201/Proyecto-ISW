package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;

import java.util.Objects;

final class ReglasPauta {
    private ReglasPauta() {}

    static boolean aplicaALinea(PautaEvaluacion pauta, LineaPractica linea) {
        Long asignaturaDeLaLinea = linea.getInscripcion().getSeccion().getAsignatura().getIdAsignatura();
        boolean mismaAsignatura = Objects.equals(pauta.getAsignatura().getIdAsignatura(), asignaturaDeLaLinea);
        boolean mismoTipo = String.valueOf(pauta.getTipoLinea()).equals(String.valueOf(linea.getTipo()));
        return mismaAsignatura && mismoTipo;
    }
}
