package com.grupo1.isw.practicas_pedagogicas.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** N-01: el formulario de asignatura o sección tiene uno o más errores; se informan todos juntos. */
public class EstructuraInvalidaException extends ResponseStatusException {
    private static final long serialVersionUID = 1L;

    private final transient List<String> errores;

    public EstructuraInvalidaException(List<String> errores) {
        super(HttpStatus.BAD_REQUEST, String.join("; ", errores));
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
