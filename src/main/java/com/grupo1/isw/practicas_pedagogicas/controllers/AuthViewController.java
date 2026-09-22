package com.grupo1.isw.practicas_pedagogicas.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Vistas mínimas de autenticación. La página de inicio es provisoria:
 * el equipo puede reemplazarla por su propio menú (usando sec:authorize por rol).
 */
@Controller
public class AuthViewController {

    @GetMapping("/login")
    public String login() {
        return "login";   // templates/login.html
    }

    @GetMapping("/")
    public String inicio() {
        return "inicio";  // templates/inicio.html
    }
}
