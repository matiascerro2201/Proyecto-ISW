package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.GestionPautaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** HU-01: el profesor de la asignatura define las pautas de evaluación de sus asignaturas. */
@Controller
@RequestMapping("/profesor/pautas")
@PreAuthorize("hasRole('PROFESOR_ASIGNATURA')")
public class GestionPautaController {
    private static final String BASE = "/profesor/pautas";
    private static final String VISTA_LISTA = "profesor/pautas/lista";
    private static final String VISTA_NUEVA = "profesor/pautas/nueva";
    private static final String VISTA_DETALLE = "profesor/pautas/detalle";

    private final GestionPautaService gestionPautaService;

    public GestionPautaController(GestionPautaService gestionPautaService) {
        this.gestionPautaService = gestionPautaService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("asignaturas", gestionPautaService.listarAsignaturas(usuario.getIdUsuario()));
        return VISTA_LISTA;
    }

    @GetMapping("/nueva")
    public String nueva(@RequestParam Long asignatura,
                        @AuthenticationPrincipal UsuarioDetails usuario,
                        Model model) {
        model.addAttribute("asignatura", gestionPautaService.obtenerAsignatura(usuario.getIdUsuario(), asignatura));
        model.addAttribute("tiposLinea", TipoLinea.values());
        return VISTA_NUEVA;
    }

    @PostMapping
    public String crear(@RequestParam Long asignatura,
                        @RequestParam(defaultValue = "") String tipoLinea,
                        @RequestParam(defaultValue = "") String numeroAplicacion,
                        @RequestParam(defaultValue = "") String ponderacion,
                        @AuthenticationPrincipal UsuarioDetails usuario,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        Long idProfesor = usuario.getIdUsuario();
        try {
            Long idPauta = gestionPautaService.crearPauta(
                    idProfesor, asignatura, tipoLinea, numeroAplicacion, ponderacion);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Pauta creada. Ahora agrega sus dimensiones y criterios.");
            return "redirect:" + BASE + "/" + idPauta;
        } catch (IllegalArgumentException e) {
            // Datos inválidos: se vuelve al formulario conservando lo que escribió el profesor.
            model.addAttribute("asignatura", gestionPautaService.obtenerAsignatura(idProfesor, asignatura));
            model.addAttribute("tiposLinea", TipoLinea.values());
            model.addAttribute("tipoIngresado", tipoLinea);
            model.addAttribute("numeroIngresado", numeroAplicacion);
            model.addAttribute("ponderacionIngresada", ponderacion);
            model.addAttribute("error", e.getMessage());
            return VISTA_NUEVA;
        }
    }

    @GetMapping("/{idPauta}")
    public String detalle(@PathVariable Long idPauta,
                          @AuthenticationPrincipal UsuarioDetails usuario,
                          Model model) {
        model.addAttribute("pauta", gestionPautaService.obtenerPauta(usuario.getIdUsuario(), idPauta));
        model.addAttribute("tiposLinea", TipoLinea.values());
        return VISTA_DETALLE;
    }

    @PostMapping("/{idPauta}/datos")
    public String actualizarDatos(@PathVariable Long idPauta,
                                  @RequestParam(defaultValue = "") String tipoLinea,
                                  @RequestParam(defaultValue = "") String numeroAplicacion,
                                  @RequestParam(defaultValue = "") String ponderacion,
                                  @AuthenticationPrincipal UsuarioDetails usuario,
                                  RedirectAttributes redirectAttributes) {
        return ejecutar(idPauta, redirectAttributes, "Datos de la pauta guardados.", () ->
                gestionPautaService.actualizarDatos(
                        usuario.getIdUsuario(), idPauta, tipoLinea, numeroAplicacion, ponderacion));
    }

    @PostMapping("/{idPauta}/eliminar")
    public String eliminar(@PathVariable Long idPauta,
                           @AuthenticationPrincipal UsuarioDetails usuario,
                           RedirectAttributes redirectAttributes) {
        try {
            gestionPautaService.eliminarPauta(usuario.getIdUsuario(), idPauta);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + BASE + "/" + idPauta;
        }
        redirectAttributes.addFlashAttribute("mensaje", "Pauta eliminada.");
        return "redirect:" + BASE;
    }

    @PostMapping("/{idPauta}/dimensiones")
    public String agregarDimension(@PathVariable Long idPauta,
                                   @RequestParam(defaultValue = "") String nombre,
                                   @AuthenticationPrincipal UsuarioDetails usuario,
                                   RedirectAttributes redirectAttributes) {
        return ejecutar(idPauta, redirectAttributes, "Dimensión agregada.", () ->
                gestionPautaService.agregarDimension(usuario.getIdUsuario(), idPauta, nombre));
    }

    @PostMapping("/{idPauta}/dimensiones/{idDimension}/eliminar")
    public String eliminarDimension(@PathVariable Long idPauta,
                                    @PathVariable Long idDimension,
                                    @AuthenticationPrincipal UsuarioDetails usuario,
                                    RedirectAttributes redirectAttributes) {
        return ejecutar(idPauta, redirectAttributes, "Dimensión eliminada junto con sus criterios.", () ->
                gestionPautaService.eliminarDimension(usuario.getIdUsuario(), idPauta, idDimension));
    }

    @PostMapping("/{idPauta}/dimensiones/{idDimension}/criterios")
    public String agregarCriterio(@PathVariable Long idPauta,
                                  @PathVariable Long idDimension,
                                  @RequestParam(defaultValue = "") String descripcion,
                                  @RequestParam(defaultValue = "") String puntajeMax,
                                  @AuthenticationPrincipal UsuarioDetails usuario,
                                  RedirectAttributes redirectAttributes) {
        return ejecutar(idPauta, redirectAttributes, "Criterio agregado.", () ->
                gestionPautaService.agregarCriterio(
                        usuario.getIdUsuario(), idPauta, idDimension, descripcion, puntajeMax));
    }

    @PostMapping("/{idPauta}/criterios/{idCriterio}/eliminar")
    public String eliminarCriterio(@PathVariable Long idPauta,
                                   @PathVariable Long idCriterio,
                                   @AuthenticationPrincipal UsuarioDetails usuario,
                                   RedirectAttributes redirectAttributes) {
        return ejecutar(idPauta, redirectAttributes, "Criterio eliminado.", () ->
                gestionPautaService.eliminarCriterio(usuario.getIdUsuario(), idPauta, idCriterio));
    }

    /** Ejecuta una acción sobre la pauta y vuelve a su detalle con el mensaje de éxito o de error. */
    private String ejecutar(Long idPauta, RedirectAttributes redirectAttributes,
                            String mensajeExito, Runnable accion) {
        try {
            accion.run();
            redirectAttributes.addFlashAttribute("mensaje", mensajeExito);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE + "/" + idPauta;
    }
}
