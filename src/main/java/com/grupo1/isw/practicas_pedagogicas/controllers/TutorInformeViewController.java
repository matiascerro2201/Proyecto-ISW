package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.InformeTutorService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * HU-14: el profesor supervisor (rol TUTOR) consulta los informes finales publicados de sus estudiantes.
 * Solo lectura: lista (GET /tutor/informes) y detalle (GET /tutor/informes/{id}).
 */
@Controller
@RequestMapping("/tutor/informes")
@PreAuthorize("hasRole('TUTOR')")
public class TutorInformeViewController {
    private final InformeTutorService informeTutorService;

    public TutorInformeViewController(InformeTutorService informeTutorService) {
        this.informeTutorService = informeTutorService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("informes", informeTutorService.listar(usuario.getIdUsuario()));
        return "tutor/informes/lista";
    }

    @GetMapping("/{idInforme}")
    public String detalle(@PathVariable Long idInforme,
                          @AuthenticationPrincipal UsuarioDetails usuario,
                          Model model) {
        model.addAttribute("informe", informeTutorService.obtener(usuario.getIdUsuario(), idInforme));
        return "tutor/informes/detalle";
    }
}
