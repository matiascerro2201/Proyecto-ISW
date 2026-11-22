package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.PautasEvaluadorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** N-16: el profesor colaborador o el tutor ven sus pautas por estudiante, con fecha límite y estado. */
@Controller
@RequestMapping("/evaluador/pautas")
@PreAuthorize("hasAnyRole('COLABORADOR','TUTOR')")
public class EvaluadorPautasController {
    private final PautasEvaluadorService servicio;

    public EvaluadorPautasController(PautasEvaluadorService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuario, HttpServletRequest peticion, Model model) {
        String evaluador = peticion.isUserInRole("TUTOR") ? "TUTOR" : "COLABORADOR";
        model.addAttribute("estudiantes", servicio.listar(usuario.getIdUsuario(), evaluador));
        model.addAttribute("esTutor", "TUTOR".equals(evaluador));
        return "evaluador/pautas";
    }
}
