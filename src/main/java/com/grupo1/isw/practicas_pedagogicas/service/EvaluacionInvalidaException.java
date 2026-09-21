package com.grupo1.isw.practicas_pedagogicas.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 400 con TODOS los problemas de la evaluación (no solo el primero).
 * La vista Thymeleaf los muestra sobre el formulario, con lo que el usuario escribió.
 */
public class EvaluacionInvalidaException extends ResponseStatusException {

    private static final long serialVersionUID = 1L;

    private final transient List<String> errores;

    public EvaluacionInvalidaException(List<String> errores) {
        super(HttpStatus.BAD_REQUEST, String.join("; ", errores));
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
