package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

/** Un semestre con cursos en el sistema (primer nivel de "Cursos"). */
public record SemestreResumen(int anio, int semestre, long cursos, long estudiantes) {
    public String etiqueta() {
        return "Semestre " + semestre + " de " + anio;
    }

    public String enlace() {
        return "/coordinador/cursos/semestre/" + anio + "/" + semestre;
    }
}
