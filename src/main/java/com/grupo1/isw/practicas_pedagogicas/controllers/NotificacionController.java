package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.NotificacionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** HU-22: cada usuario ve y marca sus propias notificaciones, el usuario siempre sale de la sesión. */
@Controller
@RequestMapping("/notificaciones")
@PreAuthorize("isAuthenticated()")
public class NotificacionController {
    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("notificaciones", notificacionService.listar(usuario.getIdUsuario()));
        return "notificaciones/lista";
    }

    @PostMapping("/{id}/leida")
    public String marcarLeida(@PathVariable Long id, @AuthenticationPrincipal UsuarioDetails usuario) {
        notificacionService.marcarLeida(usuario.getIdUsuario(), id);
        return "redirect:/notificaciones";
    }
}
