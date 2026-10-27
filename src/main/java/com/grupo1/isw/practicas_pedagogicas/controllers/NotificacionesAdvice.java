package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.NotificacionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** en las vistas sin usuario el contador es 0 */
@ControllerAdvice
public class NotificacionesAdvice {
    private final NotificacionService notificacionService;

    public NotificacionesAdvice(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @ModelAttribute("notificacionesNoLeidas")
    public long notificacionesNoLeidas(@AuthenticationPrincipal UsuarioDetails usuario) {
        return usuario == null ? 0 : notificacionService.contarNoLeidas(usuario.getIdUsuario());
    }
}
