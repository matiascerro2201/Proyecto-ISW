package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.PautaSemestreFormulario;
import com.grupo1.isw.practicas_pedagogicas.services.CoordinadorPautaService;
import com.grupo1.isw.practicas_pedagogicas.services.EstructuraInvalidaException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * N-04: pautas del semestre (colaborador y tutor). Una sola pantalla crea y edita la pauta completa: sus datos, sus
 * dimensiones y sus criterios se guardan juntos. Los endpoints para agregar o quitar una dimensión o un criterio
 * suelto se mantienen para quien los use directamente (por ejemplo, desde Postman).
 */
@Controller
@RequestMapping("/coordinador/semestre/{idSeccion}/pautas")
@PreAuthorize("hasRole('COORDINADOR')")
public class CoordinadorPautaSemestreController {
    private static final String VISTA = "coordinador/semestre/pauta";

    private final CoordinadorPautaService servicio;

    public CoordinadorPautaSemestreController(CoordinadorPautaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/nueva")
    public String nueva(@PathVariable Long idSeccion, Model model) {
        model.addAttribute("formulario", PautaSemestreFormulario.vacio());
        cargarBasicos(model, idSeccion);
        model.addAttribute("editando", false);
        return VISTA;
    }

    @PostMapping
    public String crear(@PathVariable Long idSeccion,
                        @ModelAttribute("formulario") PautaSemestreFormulario formulario,
                        Model model,
                        RedirectAttributes redirect) {
        try {
            servicio.crear(idSeccion, formulario);
            redirect.addFlashAttribute("mensaje", "Pauta creada.");
            return volver(idSeccion);
        } catch (EstructuraInvalidaException e) {
            model.addAttribute("errores", e.getErrores());
            cargarBasicos(model, idSeccion);
            model.addAttribute("editando", false);
            return VISTA;
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return volver(idSeccion);
        }
    }

    @GetMapping("/{idPauta}")
    public String ver(@PathVariable Long idSeccion, @PathVariable Long idPauta, Model model) {
        model.addAttribute("formulario", servicio.formularioDe(idSeccion, idPauta));
        cargarDetalle(model, idSeccion, idPauta);
        return VISTA;
    }

    @PostMapping("/{idPauta}")
    public String actualizar(@PathVariable Long idSeccion,
                             @PathVariable Long idPauta,
                             @ModelAttribute("formulario") PautaSemestreFormulario formulario,
                             Model model,
                             RedirectAttributes redirect) {
        try {
            servicio.actualizar(idSeccion, idPauta, formulario);
            redirect.addFlashAttribute("mensaje", "Pauta actualizada.");
        } catch (EstructuraInvalidaException e) {
            model.addAttribute("errores", e.getErrores());
            cargarDetalle(model, idSeccion, idPauta);
            return VISTA;
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return irAPauta(idSeccion, idPauta);
    }

    @PostMapping("/{idPauta}/habilitada")
    public String habilitar(@PathVariable Long idSeccion, @PathVariable Long idPauta,
                            @RequestParam boolean habilitada, RedirectAttributes redirect) {
        try {
            servicio.cambiarHabilitada(idSeccion, idPauta, habilitada);
            redirect.addFlashAttribute("mensaje", habilitada ? "Pauta habilitada." : "Pauta deshabilitada.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return volver(idSeccion);
    }

    @PostMapping("/{idPauta}/eliminar")
    public String eliminar(@PathVariable Long idSeccion, @PathVariable Long idPauta, RedirectAttributes redirect) {
        try {
            servicio.eliminar(idSeccion, idPauta);
            redirect.addFlashAttribute("mensaje", "Pauta eliminada.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return irAPauta(idSeccion, idPauta);
        }
        return volver(idSeccion);
    }

    // ---- dimensiones y criterios

    @PostMapping("/{idPauta}/dimensiones")
    public String agregarDimension(@PathVariable Long idSeccion, @PathVariable Long idPauta,
                                   @RequestParam String nombre, RedirectAttributes redirect) {
        return conAviso(redirect, idSeccion, idPauta, "Dimensión agregada.",
                () -> servicio.agregarDimension(idSeccion, idPauta, nombre));
    }

    @PostMapping("/{idPauta}/dimensiones/{idDimension}/eliminar")
    public String eliminarDimension(@PathVariable Long idSeccion, @PathVariable Long idPauta,
                                    @PathVariable Long idDimension, RedirectAttributes redirect) {
        return conAviso(redirect, idSeccion, idPauta, "Dimensión eliminada.",
                () -> servicio.eliminarDimension(idSeccion, idPauta, idDimension));
    }

    @PostMapping("/{idPauta}/dimensiones/{idDimension}/criterios")
    public String agregarCriterio(@PathVariable Long idSeccion, @PathVariable Long idPauta,
                                  @PathVariable Long idDimension, @RequestParam String descripcion,
                                  @RequestParam String puntajeMaximo, RedirectAttributes redirect) {
        return conAviso(redirect, idSeccion, idPauta, "Criterio agregado.",
                () -> servicio.agregarCriterio(idSeccion, idPauta, idDimension, descripcion, puntajeMaximo));
    }

    @PostMapping("/{idPauta}/dimensiones/{idDimension}/criterios/{idCriterio}/eliminar")
    public String eliminarCriterio(@PathVariable Long idSeccion, @PathVariable Long idPauta,
                                   @PathVariable Long idDimension, @PathVariable Long idCriterio,
                                   RedirectAttributes redirect) {
        return conAviso(redirect, idSeccion, idPauta, "Criterio eliminado.",
                () -> servicio.eliminarCriterio(idSeccion, idPauta, idDimension, idCriterio));
    }

    /** Ejecuta la acción y vuelve a la pauta con un mensaje de éxito o con los errores que haya. */
    private String conAviso(RedirectAttributes redirect, Long idSeccion, Long idPauta, String exito, Runnable accion) {
        try {
            accion.run();
            redirect.addFlashAttribute("mensaje", exito);
        } catch (EstructuraInvalidaException e) {
            redirect.addFlashAttribute("errores", e.getErrores());
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return irAPauta(idSeccion, idPauta);
    }

    private void cargarBasicos(Model model, Long idSeccion) {
        model.addAttribute("seccion", servicio.seccion(idSeccion));
        model.addAttribute("lineas", servicio.lineasDisponibles(idSeccion));
    }

    private void cargarDetalle(Model model, Long idSeccion, Long idPauta) {
        cargarBasicos(model, idSeccion);
        model.addAttribute("editando", true);
        model.addAttribute("detalle", servicio.detalle(idSeccion, idPauta));
    }

    private String irAPauta(Long idSeccion, Long idPauta) {
        return "redirect:/coordinador/semestre/" + idSeccion + "/pautas/" + idPauta;
    }

    private String volver(Long idSeccion) {
        return "redirect:/coordinador/semestre/" + idSeccion;
    }
}
