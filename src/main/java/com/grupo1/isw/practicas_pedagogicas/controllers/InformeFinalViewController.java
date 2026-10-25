package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.InformeFinalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mis-informes")
@PreAuthorize("hasRole('ESTUDIANTE')")
public class InformeFinalViewController {
    private final InformeFinalService informeFinalService;

    public InformeFinalViewController(InformeFinalService informeFinalService) {
        this.informeFinalService = informeFinalService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuarioLogueado, Model model) {
        model.addAttribute("informes",
                informeFinalService.listarInformesPublicados(usuarioLogueado.getIdUsuario()));
        return "informes/lista";
    }

    @GetMapping("/{idInforme}")
    public String detalle(@PathVariable Long idInforme,
                          @AuthenticationPrincipal UsuarioDetails usuarioLogueado,
                          Model model) {
        model.addAttribute("informe",
                informeFinalService.obtenerInformeParaEstudiante(idInforme, usuarioLogueado.getIdUsuario()));
        // HU-10: cómo fue evaluado y por qué
        model.addAttribute("desglose",
                informeFinalService.obtenerDesgloseParaEstudiante(idInforme, usuarioLogueado.getIdUsuario()));
        return "informes/detalle";
    }
}
