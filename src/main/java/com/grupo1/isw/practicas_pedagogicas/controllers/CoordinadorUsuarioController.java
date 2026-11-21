package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.Etiquetas;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.OpcionRol;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.UsuarioFormulario;
import com.grupo1.isw.practicas_pedagogicas.models.RolUsuario;
import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.CoordinadorUsuarioService;
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

import java.util.Arrays;
import java.util.List;

/** HU-36 y HU-39: cuentas y roles. Una sola pantalla de formulario sirve para crear y para editar. */
@Controller
@RequestMapping("/coordinador/usuarios")
@PreAuthorize("hasRole('COORDINADOR')")
public class CoordinadorUsuarioController {
    private static final String BASE = "/coordinador/usuarios";
    private static final String VISTA_LISTA = "coordinador/usuarios/lista";
    private static final String VISTA_FORMULARIO = "coordinador/usuarios/formulario";
    private static final List<OpcionRol> ROLES = Arrays.stream(RolUsuario.values())
            .map(rol -> new OpcionRol(rol, Etiquetas.rol(rol)))
            .toList();

    private final CoordinadorUsuarioService usuarioService;

    public CoordinadorUsuarioController(CoordinadorUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) RolUsuario rol,
                         @RequestParam(required = false) String estado,
                         @AuthenticationPrincipal UsuarioDetails coordinador,
                         Model model) {
        Boolean activo = "ACTIVOS".equals(estado) ? Boolean.TRUE : "INACTIVOS".equals(estado) ? Boolean.FALSE : null;
        model.addAttribute("usuarios", usuarioService.listar(q, rol, activo));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("rol", rol);
        model.addAttribute("estado", estado == null ? "" : estado);
        model.addAttribute("roles", ROLES);
        model.addAttribute("idActual", coordinador.getIdUsuario());
        return VISTA_LISTA;
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("formulario", UsuarioFormulario.vacio());
        cargarFormulario(model, null, false);
        return VISTA_FORMULARIO;
    }

    @PostMapping
    public String crear(@ModelAttribute("formulario") UsuarioFormulario formulario,
                        Model model,
                        RedirectAttributes redirect) {
        try {
            usuarioService.crear(formulario);
            redirect.addFlashAttribute("mensaje", "Cuenta creada.");
            return "redirect:" + BASE;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            cargarFormulario(model, null, false);
            return VISTA_FORMULARIO;
        }
    }

    @GetMapping("/{idUsuario}/editar")
    public String editar(@PathVariable Long idUsuario,
                         @AuthenticationPrincipal UsuarioDetails coordinador,
                         Model model) {
        model.addAttribute("formulario", usuarioService.formularioDe(idUsuario));
        cargarFormulario(model, idUsuario, idUsuario.equals(coordinador.getIdUsuario()));
        return VISTA_FORMULARIO;
    }

    @PostMapping("/{idUsuario}")
    public String actualizar(@PathVariable Long idUsuario,
                             @ModelAttribute("formulario") UsuarioFormulario formulario,
                             @AuthenticationPrincipal UsuarioDetails coordinador,
                             Model model,
                             RedirectAttributes redirect) {
        try {
            usuarioService.actualizar(coordinador.getIdUsuario(), idUsuario, formulario);
            redirect.addFlashAttribute("mensaje", "Cuenta actualizada.");
            return "redirect:" + BASE;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            cargarFormulario(model, idUsuario, idUsuario.equals(coordinador.getIdUsuario()));
            return VISTA_FORMULARIO;
        }
    }

    @PostMapping("/{idUsuario}/desactivar")
    public String desactivar(@PathVariable Long idUsuario,
                             @AuthenticationPrincipal UsuarioDetails coordinador,
                             RedirectAttributes redirect) {
        try {
            usuarioService.desactivar(coordinador.getIdUsuario(), idUsuario);
            redirect.addFlashAttribute("mensaje", "Cuenta desactivada. Ya no puede iniciar sesión.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE;
    }

    @PostMapping("/{idUsuario}/activar")
    public String activar(@PathVariable Long idUsuario, RedirectAttributes redirect) {
        usuarioService.activar(idUsuario);
        redirect.addFlashAttribute("mensaje", "Cuenta activada.");
        return "redirect:" + BASE;
    }

    private void cargarFormulario(Model model, Long idUsuario, boolean esLaPropiaCuenta) {
        model.addAttribute("idUsuario", idUsuario);
        model.addAttribute("editando", idUsuario != null);
        model.addAttribute("propia", esLaPropiaCuenta);
        model.addAttribute("roles", ROLES);
        model.addAttribute("centros", usuarioService.centros());
    }
}
