package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.GeneracionInformeService;
import com.grupo1.isw.practicas_pedagogicas.services.InformeNoGenerableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** HU-10: el profesor de la asignatura genera y revisa el informe final de desempeño de sus estudiantes. */
@Controller
@RequestMapping("/profesor/informes")
@PreAuthorize("hasRole('PROFESOR_ASIGNATURA')")
public class GeneracionInformeViewController {
    private final GeneracionInformeService generacionInformeService;

    public GeneracionInformeViewController(GeneracionInformeService generacionInformeService) {
        this.generacionInformeService = generacionInformeService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails profesor, Model model) {
        model.addAttribute("estudiantes", generacionInformeService.listarEstudiantes(profesor.getIdUsuario()));
        return "profesor/informes/lista";
    }

    @GetMapping("/{idInforme}")
    public String detalle(@PathVariable Long idInforme,
                          @AuthenticationPrincipal UsuarioDetails profesor,
                          Model model) {
        model.addAttribute("informe", generacionInformeService.obtenerParaProfesor(profesor.getIdUsuario(), idInforme));
        return "profesor/informes/detalle";
    }

    @PostMapping("/inscripciones/{idInscripcion}/generar")
    public String generar(@PathVariable Long idInscripcion,
                          @AuthenticationPrincipal UsuarioDetails profesor,
                          RedirectAttributes redirect) {
        try {
            Long idInforme = generacionInformeService.generar(profesor.getIdUsuario(), idInscripcion);
            redirect.addFlashAttribute("mensaje", "El informe final se generó correctamente.");
            return "redirect:/profesor/informes/" + idInforme;
        } catch (InformeNoGenerableException ex) {
            redirect.addFlashAttribute("errores", ex.getMotivos());
            return "redirect:/profesor/informes";
        }
    }
}
