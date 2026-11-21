package com.grupo1.isw.practicas_pedagogicas.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/**
 * Si la petición supera el límite de multipart, Spring falla antes de llegar al controlador, así que el
 * try/catch del controlador no lo ve. Solo se responde JSON para la subida del PDF de planificación;
 * el resto de las rutas conserva su comportamiento actual.
 */
@RestControllerAdvice
public class SubidaExcedidaAdvice {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> excedida(MaxUploadSizeExceededException e, HttpServletRequest request) {
        if (!request.getRequestURI().endsWith("/estudiante/planificaciones/archivo")) {
            throw e;
        }
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", "El archivo supera el tamaño máximo de 20 MB"));
    }
}
