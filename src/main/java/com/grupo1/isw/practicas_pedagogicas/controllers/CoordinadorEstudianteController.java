package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.services.CoordinadorEstudianteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * HU-37: el botón "Estudiantes" del coordinador. La lista con todos los nombres; al elegir uno se abre una ventana
 * con sus cursos y el estado de sus actividades. Con ?fragmento devuelve solo el contenido de esa ventana; sin él,
 * la página completa (sirve también si el navegador no ejecuta JavaScript).
 */
@Controller
@RequestMapping("/coordinador/estudiantes")
@PreAuthorize("hasRole('COORDINADOR')")
public class CoordinadorEstudianteController {
    private final CoordinadorEstudianteService estudianteService;

    public CoordinadorEstudianteController(CoordinadorEstudianteService estudianteService) {
        this.estudianteService = estudianteService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("estudiantes", estudianteService.listar(q));
        return "coordinador/estudiantes/lista";
    }

    @GetMapping("/{idEstudiante}")
    public String detalle(@PathVariable Long idEstudiante,
                          @RequestParam(required = false) String fragmento,
                          Model model) {
        model.addAttribute("estudiante", estudianteService.detalle(idEstudiante));
        return fragmento != null ? "coordinador/estudiantes/detalle :: contenido" : "coordinador/estudiantes/detalle";
    }
}
