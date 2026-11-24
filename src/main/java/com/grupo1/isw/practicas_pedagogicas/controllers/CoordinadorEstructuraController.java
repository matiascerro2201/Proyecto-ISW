package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.EntregaFormulario;
import com.grupo1.isw.practicas_pedagogicas.services.CoordinadorEntregaService;
import com.grupo1.isw.practicas_pedagogicas.services.EstructuraInvalidaException;
import com.grupo1.isw.practicas_pedagogicas.services.EstructuraSemestreService;
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
 * N-03, N-05, N-06 y N-07: la estructura del semestre (entregas y pautas), la suma de ponderaciones,
 * iniciar el semestre y copiar la estructura de otro semestre.
 */
@Controller
@RequestMapping("/coordinador/semestre/{idSeccion}")
@PreAuthorize("hasRole('COORDINADOR')")
public class CoordinadorEstructuraController {
    private static final String VISTA_ESTRUCTURA = "coordinador/semestre/estructura";
    private static final String VISTA_ENTREGA = "coordinador/semestre/entrega-formulario";

    private final EstructuraSemestreService estructura;
    private final CoordinadorEntregaService entregas;

    public CoordinadorEstructuraController(EstructuraSemestreService estructura, CoordinadorEntregaService entregas) {
        this.estructura = estructura;
        this.entregas = entregas;
    }

    @GetMapping
    public String ver(@PathVariable Long idSeccion, Model model) {
        model.addAttribute("estructura", estructura.vista(idSeccion));
        model.addAttribute("origenes", estructura.origenesParaCopiar(idSeccion));
        return VISTA_ESTRUCTURA;
    }

    @PostMapping("/iniciar")
    public String iniciar(@PathVariable Long idSeccion, RedirectAttributes redirect) {
        try {
            estructura.iniciar(idSeccion);
            redirect.addFlashAttribute("mensaje", "Semestre iniciado. La estructura quedó congelada.");
        } catch (EstructuraInvalidaException e) {
            redirect.addFlashAttribute("errores", e.getErrores());
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return volver(idSeccion);
    }

    @PostMapping("/copiar")
    public String copiar(@PathVariable Long idSeccion, @RequestParam Long idOrigen, RedirectAttributes redirect) {
        try {
            estructura.copiarDe(idSeccion, idOrigen);
            redirect.addFlashAttribute("mensaje",
                    "Estructura copiada. Revisa las ponderaciones y define las fechas límite de este semestre.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return volver(idSeccion);
    }

    // ---- entregas (N-03)

    @GetMapping("/entregas/nueva")
    public String nuevaEntrega(@PathVariable Long idSeccion, Model model) {
        model.addAttribute("formulario", new EntregaFormulario());
        cargarFormularioEntrega(model, idSeccion, null);
        return VISTA_ENTREGA;
    }

    @PostMapping("/entregas")
    public String crearEntrega(@PathVariable Long idSeccion,
                               @ModelAttribute("formulario") EntregaFormulario formulario,
                               Model model,
                               RedirectAttributes redirect) {
        try {
            entregas.crear(idSeccion, formulario);
            redirect.addFlashAttribute("mensaje", "Entrega creada.");
        } catch (EstructuraInvalidaException e) {
            model.addAttribute("errores", e.getErrores());
            cargarFormularioEntrega(model, idSeccion, null);
            return VISTA_ENTREGA;
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return volver(idSeccion);
    }

    @GetMapping("/entregas/{idEntrega}/editar")
    public String editarEntrega(@PathVariable Long idSeccion, @PathVariable Long idEntrega, Model model) {
        model.addAttribute("formulario", entregas.formularioDe(idSeccion, idEntrega));
        cargarFormularioEntrega(model, idSeccion, idEntrega);
        return VISTA_ENTREGA;
    }

    @PostMapping("/entregas/{idEntrega}")
    public String actualizarEntrega(@PathVariable Long idSeccion,
                                    @PathVariable Long idEntrega,
                                    @ModelAttribute("formulario") EntregaFormulario formulario,
                                    Model model,
                                    RedirectAttributes redirect) {
        try {
            entregas.actualizar(idSeccion, idEntrega, formulario);
            redirect.addFlashAttribute("mensaje", "Entrega actualizada.");
        } catch (EstructuraInvalidaException e) {
            model.addAttribute("errores", e.getErrores());
            cargarFormularioEntrega(model, idSeccion, idEntrega);
            return VISTA_ENTREGA;
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return volver(idSeccion);
    }

    @PostMapping("/entregas/{idEntrega}/habilitada")
    public String habilitarEntrega(@PathVariable Long idSeccion, @PathVariable Long idEntrega,
                                   @RequestParam boolean habilitada, RedirectAttributes redirect) {
        try {
            entregas.cambiarHabilitada(idSeccion, idEntrega, habilitada);
            redirect.addFlashAttribute("mensaje", habilitada ? "Entrega habilitada." : "Entrega deshabilitada.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return volver(idSeccion);
    }

    @PostMapping("/entregas/{idEntrega}/eliminar")
    public String eliminarEntrega(@PathVariable Long idSeccion, @PathVariable Long idEntrega,
                                  RedirectAttributes redirect) {
        try {
            entregas.eliminar(idSeccion, idEntrega);
            redirect.addFlashAttribute("mensaje", "Entrega eliminada.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return volver(idSeccion);
    }

    private void cargarFormularioEntrega(Model model, Long idSeccion, Long idEntrega) {
        model.addAttribute("seccion", entregas.seccion(idSeccion));
        model.addAttribute("editando", idEntrega != null);
        model.addAttribute("idEntrega", idEntrega);
    }

    private String volver(Long idSeccion) {
        return "redirect:/coordinador/semestre/" + idSeccion;
    }
}
