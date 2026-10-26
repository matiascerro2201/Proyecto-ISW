package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.dto.PautaFormulario;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.PautaDefinicionService;
import com.grupo1.isw.practicas_pedagogicas.services.PautaDefinicionService.AsignaturaConPautas;
import com.grupo1.isw.practicas_pedagogicas.services.PautaDefinicionService.PautaEditable;
import com.grupo1.isw.practicas_pedagogicas.services.PautaInvalidaException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * HU-01: el profesor de la asignatura define las pautas de evaluación de sus asignaturas.
 *
 * <p>Una sola pantalla de formulario sirve para crear y para editar. Las filas de dimensiones y criterios se
 * agregan y quitan con botones que reenvían el formulario (parámetro {@code accion}); solo la acción
 * {@code guardar} intenta guardar.
 */
@Controller
@RequestMapping("/profesor/pautas")
@PreAuthorize("hasRole('PROFESOR_ASIGNATURA')")
public class PautaDefinicionController {
    private static final String BASE = "/profesor/pautas";
    private static final String VISTA_LISTA = "profesor/pautas/lista";
    private static final String VISTA_FORMULARIO = "profesor/pautas/formulario";

    private final PautaDefinicionService pautaDefinicionService;

    public PautaDefinicionController(PautaDefinicionService pautaDefinicionService) {
        this.pautaDefinicionService = pautaDefinicionService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("asignaturas", pautaDefinicionService.listarAsignaturas(usuario.getIdUsuario()));
        return VISTA_LISTA;
    }

    @GetMapping("/nueva")
    public String nueva(@RequestParam Long asignatura,
                        @AuthenticationPrincipal UsuarioDetails usuario,
                        Model model) {
        AsignaturaConPautas datos = pautaDefinicionService.obtenerAsignatura(usuario.getIdUsuario(), asignatura);
        model.addAttribute("formulario", PautaFormulario.vacio());
        mostrarFormulario(model, datos.nombre(), datos.idAsignatura(), null, false);
        return VISTA_FORMULARIO;
    }

    @PostMapping("/nueva")
    public String crear(@RequestParam Long asignatura,
                        @ModelAttribute("formulario") PautaFormulario formulario,
                        @RequestParam(defaultValue = "guardar") String accion,
                        @AuthenticationPrincipal UsuarioDetails usuario,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        Long idProfesor = usuario.getIdUsuario();
        AsignaturaConPautas datos = pautaDefinicionService.obtenerAsignatura(idProfesor, asignatura);

        if (formulario.aplicarAccion(accion)) {
            try {
                pautaDefinicionService.crear(idProfesor, asignatura, formulario);
                redirectAttributes.addFlashAttribute("mensaje", "Pauta creada.");
                return "redirect:" + BASE;
            } catch (PautaInvalidaException e) {
                model.addAttribute("errores", e.getErrores());
            }
        }
        mostrarFormulario(model, datos.nombre(), datos.idAsignatura(), null, false);
        return VISTA_FORMULARIO;
    }

    @GetMapping("/{idPauta}/editar")
    public String editar(@PathVariable Long idPauta,
                         @AuthenticationPrincipal UsuarioDetails usuario,
                         Model model) {
        PautaEditable pauta = pautaDefinicionService.obtenerParaEditar(usuario.getIdUsuario(), idPauta);
        model.addAttribute("formulario", pauta.formulario());
        mostrarFormulario(model, pauta.nombreAsignatura(), pauta.idAsignatura(), idPauta, pauta.enUso());
        return VISTA_FORMULARIO;
    }

    @PostMapping("/{idPauta}/editar")
    public String actualizar(@PathVariable Long idPauta,
                             @ModelAttribute("formulario") PautaFormulario formulario,
                             @RequestParam(defaultValue = "guardar") String accion,
                             @AuthenticationPrincipal UsuarioDetails usuario,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        Long idProfesor = usuario.getIdUsuario();
        PautaEditable actual = pautaDefinicionService.obtenerParaEditar(idProfesor, idPauta);

        if (formulario.aplicarAccion(accion)) {
            try {
                pautaDefinicionService.actualizar(idProfesor, idPauta, formulario);
                redirectAttributes.addFlashAttribute("mensaje", "Pauta guardada.");
                return "redirect:" + BASE;
            } catch (PautaInvalidaException e) {
                model.addAttribute("errores", e.getErrores());
            } catch (IllegalStateException e) {
                model.addAttribute("errores", List.of(e.getMessage()));
            }
        }
        mostrarFormulario(model, actual.nombreAsignatura(), actual.idAsignatura(), idPauta, actual.enUso());
        return VISTA_FORMULARIO;
    }

    @PostMapping("/{idPauta}/eliminar")
    public String eliminar(@PathVariable Long idPauta,
                           @AuthenticationPrincipal UsuarioDetails usuario,
                           RedirectAttributes redirectAttributes) {
        try {
            pautaDefinicionService.eliminar(usuario.getIdUsuario(), idPauta);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + BASE + "/" + idPauta + "/editar";
        }
        redirectAttributes.addFlashAttribute("mensaje", "Pauta eliminada.");
        return "redirect:" + BASE;
    }

    private void mostrarFormulario(Model model, String nombreAsignatura, Long idAsignatura,
                                   Long idPauta, boolean soloLectura) {
        model.addAttribute("nombreAsignatura", nombreAsignatura);
        model.addAttribute("idAsignatura", idAsignatura);
        model.addAttribute("idPauta", idPauta);
        model.addAttribute("soloLectura", soloLectura);
        model.addAttribute("tiposLinea", TipoLinea.values());
    }
}
