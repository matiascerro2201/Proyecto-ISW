package com.grupo1.isw.practicas_pedagogicas.controller;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.service.InformeFinalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Vistas Thymeleaf de HU-15 y HU-16.
 * Llaman directamente al service (que aplica las reglas de publicación y propiedad),
 * por lo que no hace falta que la vista consuma el endpoint JSON.
 *
 * Los errores no se manejan aquí: el service lanza 403/404 y Spring Boot renderiza
 * templates/error/403.html y templates/error/404.html.
 */
@Controller
@RequestMapping("/mis-informes")
@PreAuthorize("hasRole('ESTUDIANTE')")
public class InformeFinalViewController {

    private final InformeFinalService informeFinalService;

    public InformeFinalViewController(InformeFinalService informeFinalService) {
        this.informeFinalService = informeFinalService;
    }

    /** Lista de informes publicados del estudiante autenticado. */
    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuarioLogueado, Model model) {
        model.addAttribute("informes",
                informeFinalService.listarInformesPublicados(usuarioLogueado.getIdUsuario()));
        return "informes/lista";      // templates/informes/lista.html
    }

    /**
     * Detalle de un informe. Si el id pertenece a otro estudiante -> 403;
     * si no existe o aún no está publicado -> 404 (aunque se manipule la URL).
     */
    @GetMapping("/{idInforme}")
    public String detalle(@PathVariable Long idInforme,
                          @AuthenticationPrincipal UsuarioDetails usuarioLogueado,
                          Model model) {
        model.addAttribute("informe",
                informeFinalService.obtenerInformeParaEstudiante(idInforme, usuarioLogueado.getIdUsuario()));
        return "informes/detalle";    // templates/informes/detalle.html
    }
}
