package com.grupo1.isw.practicas_pedagogicas.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Spring rechaza los archivos que superan spring.servlet.multipart.max-file-size
 * antes de elegir controlador, por eso este manejador es global (@ControllerAdvice)
 * y no puede vivir dentro de DocumentoEstudianteController.
 * Redirige a la lista con un mensaje de un solo uso (flash), igual que el resto de
 * los mensajes de la pantalla: se muestra una vez y no queda pegado a la URL.
 */
@ControllerAdvice
public class ManejadorErroresSubida {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoDemasiadoGrande(HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request)
                .put("error", "El archivo supera el tamaño máximo de 20 MB.");
        return "redirect:/estudiante/documentos";
    }
}
