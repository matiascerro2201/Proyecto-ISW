package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.dto.inicio.InicioResponse;
import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.InicioService;
import com.grupo1.isw.practicas_pedagogicas.services.NotificacionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthViewController {
    private static final int NOTIFICACIONES_EN_LA_BARRA = 5;

    private final InicioService inicioService;
    private final NotificacionService notificacionService;

    public AuthViewController(InicioService inicioService, NotificacionService notificacionService) {
        this.inicioService = inicioService;
        this.notificacionService = notificacionService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String inicio(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        InicioResponse inicio = inicioService.armar(usuario.getIdUsuario(), usuario.getRol());
        model.addAttribute("rolEtiqueta", InicioService.etiquetaRol(usuario.getRol()));
        model.addAttribute("cursos", inicio.cursos());
        model.addAttribute("pendientes", inicio.pendientes());
        model.addAttribute("notificacionesRecientes",
                notificacionService.listar(usuario.getIdUsuario()).stream()
                        .limit(NOTIFICACIONES_EN_LA_BARRA)
                        .toList());
        return "inicio";
    }
}
