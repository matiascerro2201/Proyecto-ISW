package com.grupo1.isw.practicas_pedagogicas.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** HU-10: el informe no se puede generar; lleva todos los motivos para mostrarlos juntos al profesor. */
public class InformeNoGenerableException extends ResponseStatusException {
    private static final long serialVersionUID = 1L;

    private final transient List<String> motivos;

    public InformeNoGenerableException(List<String> motivos) {
        super(HttpStatus.CONFLICT, String.join("; ", motivos));
        this.motivos = List.copyOf(motivos);
    }

    public List<String> getMotivos() {
        return motivos;
    }
}
