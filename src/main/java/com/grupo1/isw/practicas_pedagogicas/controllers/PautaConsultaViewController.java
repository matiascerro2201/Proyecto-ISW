package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.PautaConsultaService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Vistas Thymeleaf de HU-07. Solo GET.
 * Las plantillas leen ÚNICAMENTE los atributos del Model que se cargan aquí
 * ("asignaturas" y "resultado"); no hacen llamadas por JavaScript.
 * Los errores (403) los renderiza templates/error/403.html.
 */
@Controller
@RequestMapping("/mis-pautas")
@PreAuthorize("hasRole('ESTUDIANTE')")
public class PautaConsultaViewController {

    private final PautaConsultaService pautaConsultaService;

    public PautaConsultaViewController(PautaConsultaService pautaConsultaService) {
        this.pautaConsultaService = pautaConsultaService;
    }

    /** Asignaturas del estudiante que ya tienen pautas, cada una con enlace a su detalle. */
    @GetMapping
    public String listarAsignaturas(@AuthenticationPrincipal UsuarioDetails usuarioLogueado, Model model) {
        model.addAttribute("asignaturas",
                pautaConsultaService.listarAsignaturasConPautas(usuarioLogueado.getIdUsuario()));
        return "pautas/asignaturas";     // templates/pautas/asignaturas.html
    }

    /** Pautas de una asignatura. Si el estudiante no está inscrito en ella -> 403. */
    @GetMapping("/asignatura/{idAsignatura}")
    public String verPautas(@PathVariable Long idAsignatura,
                            @AuthenticationPrincipal UsuarioDetails usuarioLogueado,
                            Model model) {
        model.addAttribute("resultado",
                pautaConsultaService.consultarPautasDeAsignatura(idAsignatura, usuarioLogueado.getIdUsuario()));
        return "pautas/detalle";         // templates/pautas/detalle.html
    }
}
