package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.InscripcionFormulario;
import com.grupo1.isw.practicas_pedagogicas.services.CoordinadorInscripcionService;
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

/** N-02: inscripciones de estudiantes en una sección y sus líneas de práctica. */
@Controller
@RequestMapping("/coordinador/semestre/{idSeccion}/inscripciones")
@PreAuthorize("hasRole('COORDINADOR')")
public class CoordinadorInscripcionController {
    private static final String VISTA_LISTA = "coordinador/inscripciones/lista";
    private static final String VISTA_FORMULARIO = "coordinador/inscripciones/formulario";

    private final CoordinadorInscripcionService servicio;

    public CoordinadorInscripcionController(CoordinadorInscripcionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String listar(@PathVariable Long idSeccion, Model model) {
        model.addAttribute("seccion", servicio.seccion(idSeccion));
        model.addAttribute("inscripciones", servicio.listar(idSeccion));
        return VISTA_LISTA;
    }

    @GetMapping("/nueva")
    public String nueva(@PathVariable Long idSeccion, Model model) {
        model.addAttribute("formulario", servicio.formularioVacio(idSeccion));
        cargarFormulario(model, idSeccion, null);
        return VISTA_FORMULARIO;
    }

    @PostMapping
    public String inscribir(@PathVariable Long idSeccion,
                            @ModelAttribute("formulario") InscripcionFormulario formulario,
                            Model model,
                            RedirectAttributes redirect) {
        try {
            servicio.inscribir(idSeccion, formulario);
            redirect.addFlashAttribute("mensaje", "Estudiante inscrito.");
            return redireccionALista(idSeccion);
        } catch (EstructuraInvalidaException e) {
            model.addAttribute("errores", e.getErrores());
            cargarFormulario(model, idSeccion, null);
            return VISTA_FORMULARIO;
        }
    }

    @GetMapping("/{idInscripcion}/editar")
    public String editar(@PathVariable Long idSeccion, @PathVariable Long idInscripcion, Model model) {
        model.addAttribute("formulario", servicio.formularioDe(idSeccion, idInscripcion));
        cargarFormulario(model, idSeccion, idInscripcion);
        return VISTA_FORMULARIO;
    }

    @PostMapping("/{idInscripcion}")
    public String actualizar(@PathVariable Long idSeccion,
                             @PathVariable Long idInscripcion,
                             @ModelAttribute("formulario") InscripcionFormulario formulario,
                             Model model,
                             RedirectAttributes redirect) {
        try {
            servicio.actualizar(idSeccion, idInscripcion, formulario);
            redirect.addFlashAttribute("mensaje", "Inscripción actualizada.");
            return redireccionALista(idSeccion);
        } catch (EstructuraInvalidaException e) {
            model.addAttribute("errores", e.getErrores());
            cargarFormulario(model, idSeccion, idInscripcion);
            return VISTA_FORMULARIO;
        }
    }

    @PostMapping("/{idInscripcion}/quitar")
    public String quitar(@PathVariable Long idSeccion, @PathVariable Long idInscripcion, RedirectAttributes redirect) {
        try {
            servicio.quitar(idSeccion, idInscripcion);
            redirect.addFlashAttribute("mensaje", "Estudiante quitado de la sección.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return redireccionALista(idSeccion);
    }

    private void cargarFormulario(Model model, Long idSeccion, Long idInscripcion) {
        boolean editando = idInscripcion != null;
        model.addAttribute("seccion", servicio.seccion(idSeccion));
        model.addAttribute("editando", editando);
        model.addAttribute("idInscripcion", idInscripcion);
        model.addAttribute("estudiantes", servicio.estudiantesDisponibles(idSeccion));
        model.addAttribute("estudianteActual", editando ? servicio.inscripcion(idSeccion, idInscripcion).estudiante() : null);
        model.addAttribute("centros", servicio.centros());
        model.addAttribute("colaboradores", servicio.colaboradores());
        model.addAttribute("tutores", servicio.tutores());
    }

    private String redireccionALista(Long idSeccion) {
        return "redirect:/coordinador/semestre/" + idSeccion + "/inscripciones";
    }
}
