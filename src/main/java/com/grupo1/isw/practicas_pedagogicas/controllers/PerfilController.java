package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.PerfilService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Perfil de cualquier usuario con sesión; el usuario siempre sale de la sesión, nunca de la URL. */
@Controller
@RequestMapping("/perfil")
@PreAuthorize("isAuthenticated()")
public class PerfilController {
    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    public String ver(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("perfil", perfilService.obtener(usuario.getIdUsuario()));
        return "perfil/ver";
    }
}
