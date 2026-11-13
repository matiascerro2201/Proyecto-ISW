package com.grupo1.isw.practicas_pedagogicas.dto.inicio;

import java.util.List;

public record InicioResponse(List<CursoInicio> cursos, List<PendienteInicio> pendientes) {
    public static InicioResponse vacio() {
        return new InicioResponse(List.of(), List.of());
    }
}
