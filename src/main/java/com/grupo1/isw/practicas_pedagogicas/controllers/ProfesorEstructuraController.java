package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.EstructuraSemestreService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/** N-08: el profesor de la asignatura consulta la estructura de sus semestres, sin poder modificarla. */
@Controller
@RequestMapping("/profesor/estructura")
@PreAuthorize("hasRole('PROFESOR_ASIGNATURA')")
public class ProfesorEstructuraController {
    private final EstructuraSemestreService servicio;

    public ProfesorEstructuraController(EstructuraSemestreService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("secciones", servicio.seccionesDelProfesor(usuario.getIdUsuario()));
        return "profesor/estructura/lista";
    }

    @GetMapping("/{idSeccion}")
    public String ver(@PathVariable Long idSeccion, @AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("estructura", servicio.vistaParaProfesor(usuario.getIdUsuario(), idSeccion));
        return "profesor/estructura/detalle";
    }
}
