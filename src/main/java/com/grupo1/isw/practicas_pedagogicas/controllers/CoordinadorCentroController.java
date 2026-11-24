package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.CentroFormulario;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.Etiquetas;
import com.grupo1.isw.practicas_pedagogicas.models.TipoCentro;
import com.grupo1.isw.practicas_pedagogicas.services.CoordinadorCentroService;
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

/** El coordinador administra los centros de práctica. Una sola pantalla de formulario sirve para crear y editar. */
@Controller
@RequestMapping("/coordinador/centros")
@PreAuthorize("hasRole('COORDINADOR')")
public class CoordinadorCentroController {
    private static final String BASE = "/coordinador/centros";
    private static final String VISTA_LISTA = "coordinador/centros/lista";
    private static final String VISTA_FORMULARIO = "coordinador/centros/formulario";

    /** Opción del selector de tipo: el valor del enum y su texto para el usuario. */
    public record OpcionTipo(TipoCentro valor, String etiqueta) {
    }

    private static final List<OpcionTipo> TIPOS = Arrays.stream(TipoCentro.values())
            .map(tipo -> new OpcionTipo(tipo, Etiquetas.tipoCentro(tipo)))
            .toList();

    private final CoordinadorCentroService centroService;

    public CoordinadorCentroController(CoordinadorCentroService centroService) {
        this.centroService = centroService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("centros", centroService.listar());
        return VISTA_LISTA;
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("formulario", new CentroFormulario());
        cargarFormulario(model, null);
        return VISTA_FORMULARIO;
    }

    @PostMapping
    public String crear(@ModelAttribute("formulario") CentroFormulario formulario,
                        Model model,
                        RedirectAttributes redirect) {
        try {
            centroService.crear(formulario);
            redirect.addFlashAttribute("mensaje", "Centro de práctica creado.");
            return "redirect:" + BASE;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            cargarFormulario(model, null);
            return VISTA_FORMULARIO;
        }
    }

    @GetMapping("/{idCentro}/editar")
    public String editar(@PathVariable Long idCentro, Model model) {
        model.addAttribute("formulario", centroService.formularioDe(idCentro));
        cargarFormulario(model, idCentro);
        return VISTA_FORMULARIO;
    }

    @PostMapping("/{idCentro}")
    public String actualizar(@PathVariable Long idCentro,
                             @ModelAttribute("formulario") CentroFormulario formulario,
                             Model model,
                             RedirectAttributes redirect) {
        try {
            centroService.actualizar(idCentro, formulario);
            redirect.addFlashAttribute("mensaje", "Centro de práctica actualizado.");
            return "redirect:" + BASE;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            cargarFormulario(model, idCentro);
            return VISTA_FORMULARIO;
        }
    }

    @PostMapping("/{idCentro}/eliminar")
    public String eliminar(@PathVariable Long idCentro, RedirectAttributes redirect) {
        try {
            centroService.eliminar(idCentro);
            redirect.addFlashAttribute("mensaje", "Centro de práctica eliminado.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE;
    }

    /** Datos que el formulario necesita siempre; {@code idCentro} es null al crear. */
    private void cargarFormulario(Model model, Long idCentro) {
        model.addAttribute("editando", idCentro != null);
        model.addAttribute("idCentro", idCentro);
        model.addAttribute("tipos", TIPOS);
    }
}
