package com.grupo1.isw.practicas_pedagogicas.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** HU-01: la pauta tiene uno o más errores; se informan todos juntos, igual que en {@link EvaluacionInvalidaException}. */
public class PautaInvalidaException extends ResponseStatusException {
    private static final long serialVersionUID = 1L;

    private final transient List<String> errores;

    public PautaInvalidaException(List<String> errores) {
        super(HttpStatus.BAD_REQUEST, String.join("; ", errores));
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
