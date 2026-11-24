package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.AsignaturaFormulario;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.Etiquetas;
import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;
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

import java.util.Arrays;
import java.util.List;

/** N-01: el coordinador administra las asignaturas. Una sola pantalla de formulario sirve para crear y editar. */
@Controller
@RequestMapping("/coordinador/asignaturas")
@PreAuthorize("hasRole('COORDINADOR')")
public class CoordinadorAsignaturaController {
    private static final String BASE = "/coordinador/asignaturas";
    private static final String VISTA_LISTA = "coordinador/asignaturas/lista";
    private static final String VISTA_FORMULARIO = "coordinador/asignaturas/formulario";

    /** Opción del selector de ciclo: el valor del enum y su texto para el usuario. */
    public record OpcionCiclo(Ciclo valor, String etiqueta) {
    }

    private static final List<OpcionCiclo> CICLOS = Arrays.stream(Ciclo.values())
            .map(ciclo -> new OpcionCiclo(ciclo, Etiquetas.ciclo(ciclo)))
            .toList();

    private final CoordinadorAsignaturaService asignaturaService;

    public CoordinadorAsignaturaController(CoordinadorAsignaturaService asignaturaService) {
        this.asignaturaService = asignaturaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("asignaturas", asignaturaService.listarAsignaturas());
        return VISTA_LISTA;
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("formulario", new AsignaturaFormulario());
        cargarFormulario(model, null);
        return VISTA_FORMULARIO;
    }

    @PostMapping
    public String crear(@ModelAttribute("formulario") AsignaturaFormulario formulario,
                        Model model,
                        RedirectAttributes redirect) {
        try {
            asignaturaService.crearAsignatura(formulario);
            redirect.addFlashAttribute("mensaje", "Asignatura creada.");
            return "redirect:" + BASE;
        } catch (EstructuraInvalidaException e) {
            model.addAttribute("errores", e.getErrores());
            cargarFormulario(model, null);
            return VISTA_FORMULARIO;
        }
    }

    @GetMapping("/{idAsignatura}/editar")
    public String editar(@PathVariable Long idAsignatura, Model model) {
        model.addAttribute("formulario", asignaturaService.formularioDeAsignatura(idAsignatura));
        cargarFormulario(model, idAsignatura);
        return VISTA_FORMULARIO;
    }

    @PostMapping("/{idAsignatura}")
    public String actualizar(@PathVariable Long idAsignatura,
                             @ModelAttribute("formulario") AsignaturaFormulario formulario,
                             Model model,
                             RedirectAttributes redirect) {
        try {
            asignaturaService.actualizarAsignatura(idAsignatura, formulario);
            redirect.addFlashAttribute("mensaje", "Asignatura actualizada.");
            return "redirect:" + BASE;
        } catch (EstructuraInvalidaException e) {
            model.addAttribute("errores", e.getErrores());
            cargarFormulario(model, idAsignatura);
            return VISTA_FORMULARIO;
        }
    }

    @PostMapping("/{idAsignatura}/eliminar")
    public String eliminar(@PathVariable Long idAsignatura, RedirectAttributes redirect) {
        try {
            asignaturaService.eliminarAsignatura(idAsignatura);
            redirect.addFlashAttribute("mensaje", "Asignatura eliminada.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE;
    }

    /** Datos que el formulario necesita siempre; {@code idAsignatura} es null al crear. */
    private void cargarFormulario(Model model, Long idAsignatura) {
        boolean editando = idAsignatura != null;
        model.addAttribute("editando", editando);
        model.addAttribute("idAsignatura", idAsignatura);
        model.addAttribute("ciclos", CICLOS);
        // El número de líneas se bloquea cuando ya hay estudiantes inscritos.
        model.addAttribute("lineasBloqueadas",
                editando && asignaturaService.asignaturaTieneInscripciones(idAsignatura));
    }
}
