package com.grupo1.isw.practicas_pedagogicas.controllers;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Spring rechaza los archivos que superan spring.servlet.multipart.max-file-size
 * antes de elegir controlador, por eso este manejador es global (@ControllerAdvice)
 * y no puede vivir dentro de DocumentoEstudianteController.
 * Redirige a la lista con un parámetro que la plantilla convierte en mensaje.
 */
@ControllerAdvice
public class ManejadorErroresSubida {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoDemasiadoGrande() {
        return "redirect:/estudiante/documentos?archivoMuyGrande";
    }
}
