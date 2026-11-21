package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.CursoEstudiantesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Con ?fragmento devuelve solo la lista, para la ventana del inicio. */
@Controller
@RequestMapping("/cursos")
@PreAuthorize("hasAnyRole('PROFESOR_ASIGNATURA', 'COLABORADOR', 'TUTOR')")
public class CursoEstudiantesController {
    private final CursoEstudiantesService cursoEstudiantesService;

    public CursoEstudiantesController(CursoEstudiantesService cursoEstudiantesService) {
        this.cursoEstudiantesService = cursoEstudiantesService;
    }

    @GetMapping("/{idSeccion}/estudiantes")
    public String estudiantes(@PathVariable Long idSeccion,
                              @RequestParam(required = false) String fragmento,
                              @AuthenticationPrincipal UsuarioDetails usuario,
                              Model model) {
        model.addAttribute("curso",
                cursoEstudiantesService.listar(usuario.getIdUsuario(), usuario.getRol(), idSeccion));
        return fragmento != null ? "fragments/estudiantes-curso :: lista" : "cursos/estudiantes";
    }
}
