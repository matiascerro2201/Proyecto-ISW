package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.MisPautasService;
import com.grupo1.isw.practicas_pedagogicas.services.MisPautasService.PautaVista;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/mis-pautas")
@PreAuthorize("hasRole('ESTUDIANTE')")
public class PautaConsultaViewController {
    private final MisPautasService misPautasService;

    public PautaConsultaViewController(MisPautasService misPautasService) {
        this.misPautasService = misPautasService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuarioLogueado, Model model) {
        model.addAttribute("pautas", misPautasService.listar(usuarioLogueado.getIdUsuario()));
        return "pautas/lista";
    }

    /** Lo usa el inicio desde "Mis cursos": la misma lista, solo con las pautas de esa asignatura. */
    @GetMapping("/asignatura/{idAsignatura}")
    public String listarDeAsignatura(@PathVariable Long idAsignatura,
                                     @AuthenticationPrincipal UsuarioDetails usuarioLogueado,
                                     Model model) {
        List<PautaVista> pautas = misPautasService.listar(usuarioLogueado.getIdUsuario()).stream()
                .filter(pauta -> pauta.idAsignatura().equals(idAsignatura))
                .toList();
        if (pautas.isEmpty()) {
            return "redirect:/mis-pautas";
        }
        model.addAttribute("pautas", pautas);
        model.addAttribute("soloAsignatura", pautas.get(0).asignatura());
        return "pautas/lista";
    }

    @GetMapping("/{idPauta}")
    public String ver(@PathVariable Long idPauta,
                      @AuthenticationPrincipal UsuarioDetails usuarioLogueado,
                      Model model) {
        model.addAttribute("pauta", misPautasService.obtener(usuarioLogueado.getIdUsuario(), idPauta));
        return "pautas/detalle";
    }
}
