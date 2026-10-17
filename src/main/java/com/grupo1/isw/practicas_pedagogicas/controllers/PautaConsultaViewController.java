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

@Controller
@RequestMapping("/mis-pautas")
@PreAuthorize("hasRole('ESTUDIANTE')")
public class PautaConsultaViewController {
    private final PautaConsultaService pautaConsultaService;

    public PautaConsultaViewController(PautaConsultaService pautaConsultaService) {
        this.pautaConsultaService = pautaConsultaService;
    }

    @GetMapping
    public String listarAsignaturas(@AuthenticationPrincipal UsuarioDetails usuarioLogueado, Model model) {
        model.addAttribute("asignaturas",
                pautaConsultaService.listarAsignaturasConPautas(usuarioLogueado.getIdUsuario()));
        return "pautas/asignaturas";
    }

    @GetMapping("/asignatura/{idAsignatura}")
    public String verPautas(@PathVariable Long idAsignatura,
                            @AuthenticationPrincipal UsuarioDetails usuarioLogueado,
                            Model model) {
        model.addAttribute("resultado",
                pautaConsultaService.consultarPautasDeAsignatura(idAsignatura, usuarioLogueado.getIdUsuario()));
        return "pautas/detalle";
    }
}
