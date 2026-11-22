package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.SeccionFormulario;
import com.grupo1.isw.practicas_pedagogicas.services.CoordinadorAsignaturaService;
import com.grupo1.isw.practicas_pedagogicas.services.EstructuraInvalidaException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** N-01: el coordinador administra las secciones y asigna su profesor. Un solo formulario sirve para crear y editar. */
@Controller
@RequestMapping("/coordinador/secciones")
@PreAuthorize("hasRole('COORDINADOR')")
public class CoordinadorSeccionController {
    private static final String BASE = "/coordinador/secciones";
    private static final String VISTA_LISTA = "coordinador/secciones/lista";
    private static final String VISTA_FORMULARIO = "coordinador/secciones/formulario";

    private final CoordinadorAsignaturaService asignaturaService;

    public CoordinadorSeccionController(CoordinadorAsignaturaService asignaturaService) {
        this.asignaturaService = asignaturaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("secciones", asignaturaService.listarSecciones());
        return VISTA_LISTA;
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("formulario", new SeccionFormulario());
        cargarFormulario(model, null);
        return VISTA_FORMULARIO;
    }

    @PostMapping
    public String crear(@ModelAttribute("formulario") SeccionFormulario formulario,
                        Model model,
                        RedirectAttributes redirect) {
        try {
            asignaturaService.crearSeccion(formulario);
            redirect.addFlashAttribute("mensaje", "Sección creada.");
            return "redirect:" + BASE;
        } catch (EstructuraInvalidaException e) {
            model.addAttribute("errores", e.getErrores());
            cargarFormulario(model, null);
            return VISTA_FORMULARIO;
        }
    }

    @GetMapping("/{idSeccion}/editar")
    public String editar(@PathVariable Long idSeccion, Model model) {
        model.addAttribute("formulario", asignaturaService.formularioDeSeccion(idSeccion));
        cargarFormulario(model, idSeccion);
        return VISTA_FORMULARIO;
    }

    @PostMapping("/{idSeccion}")
    public String actualizar(@PathVariable Long idSeccion,
                             @ModelAttribute("formulario") SeccionFormulario formulario,
                             Model model,
                             RedirectAttributes redirect) {
        try {
            asignaturaService.actualizarSeccion(idSeccion, formulario);
            redirect.addFlashAttribute("mensaje", "Sección actualizada.");
            return "redirect:" + BASE;
        } catch (EstructuraInvalidaException e) {
            model.addAttribute("errores", e.getErrores());
            cargarFormulario(model, idSeccion);
            return VISTA_FORMULARIO;
        }
    }

    @PostMapping("/{idSeccion}/eliminar")
    public String eliminar(@PathVariable Long idSeccion, RedirectAttributes redirect) {
        try {
            asignaturaService.eliminarSeccion(idSeccion);
            redirect.addFlashAttribute("mensaje", "Sección eliminada.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE;
    }

    /** Datos que el formulario necesita siempre; {@code idSeccion} es null al crear. */
    private void cargarFormulario(Model model, Long idSeccion) {
        boolean editando = idSeccion != null;
        model.addAttribute("editando", editando);
        model.addAttribute("idSeccion", idSeccion);
        model.addAttribute("asignaturas", asignaturaService.listarAsignaturas());
        model.addAttribute("profesores", asignaturaService.profesoresDisponibles());
        model.addAttribute("anioMinimo", asignaturaService.anioMinimo());
        model.addAttribute("anioMaximo", asignaturaService.anioMaximo());
        // Asignatura, año y semestre se bloquean cuando ya hay estudiantes inscritos; el profesor no.
        model.addAttribute("periodoBloqueado", editando && asignaturaService.seccionTieneInscripciones(idSeccion));
    }
}
