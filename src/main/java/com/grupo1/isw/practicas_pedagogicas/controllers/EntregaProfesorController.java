package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.EntregaEstudianteService.DescargaEntrega;
import com.grupo1.isw.practicas_pedagogicas.services.EntregaProfesorService;
import com.grupo1.isw.practicas_pedagogicas.services.EstructuraInvalidaException;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

/** N-12 a N-15: el profesor revisa, califica y publica las entregas de sus estudiantes. */
@Controller
@RequestMapping("/profesor/entregas")
@PreAuthorize("hasRole('PROFESOR_ASIGNATURA')")
public class EntregaProfesorController {
    private final EntregaProfesorService servicio;

    public EntregaProfesorController(EntregaProfesorService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String secciones(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("secciones", servicio.seccionesDelProfesor(usuario.getIdUsuario()));
        return "profesor/entregas/lista";
    }

    @GetMapping("/{idSeccion}")
    public String entregas(@PathVariable Long idSeccion, @AuthenticationPrincipal UsuarioDetails usuario,
                           Model model) {
        model.addAttribute("seccion", servicio.seccion(usuario.getIdUsuario(), idSeccion));
        model.addAttribute("entregas", servicio.resumen(usuario.getIdUsuario(), idSeccion));
        return "profesor/entregas/seccion";
    }

    @GetMapping("/{idSeccion}/{idEntrega}")
    public String detalle(@PathVariable Long idSeccion, @PathVariable Long idEntrega,
                          @AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("detalle", servicio.detalle(usuario.getIdUsuario(), idSeccion, idEntrega));
        return "profesor/entregas/detalle";
    }

    @GetMapping("/{idSeccion}/{idEntrega}/{idInscripcion}/descargar")
    public ResponseEntity<Resource> descargar(@PathVariable Long idSeccion, @PathVariable Long idEntrega,
                                              @PathVariable Long idInscripcion,
                                              @AuthenticationPrincipal UsuarioDetails usuario) {
        DescargaEntrega descarga = servicio.descargar(usuario.getIdUsuario(), idSeccion, idEntrega, idInscripcion);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(descarga.nombreSugerido()).build().toString())
                .body(new PathResource(descarga.ruta()));
    }

    @PostMapping("/{idSeccion}/{idEntrega}/{idInscripcion}/nota")
    public String calificar(@PathVariable Long idSeccion, @PathVariable Long idEntrega,
                            @PathVariable Long idInscripcion,
                            @RequestParam(required = false) String nota,
                            @RequestParam(required = false) String comentario,
                            @AuthenticationPrincipal UsuarioDetails usuario, RedirectAttributes redirect) {
        try {
            servicio.calificar(usuario.getIdUsuario(), idSeccion, idEntrega, idInscripcion, nota, comentario);
            redirect.addFlashAttribute("mensaje", "Nota guardada. El estudiante la verá cuando la publiques.");
        } catch (EstructuraInvalidaException e) {
            redirect.addFlashAttribute("errores", e.getErrores());
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return volver(idSeccion, idEntrega);
    }

    @PostMapping("/{idSeccion}/{idEntrega}/{idInscripcion}/publicar")
    public String publicar(@PathVariable Long idSeccion, @PathVariable Long idEntrega,
                           @PathVariable Long idInscripcion,
                           @AuthenticationPrincipal UsuarioDetails usuario, RedirectAttributes redirect) {
        try {
            servicio.publicar(usuario.getIdUsuario(), idSeccion, idEntrega, idInscripcion);
            redirect.addFlashAttribute("mensaje", "Nota publicada.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return volver(idSeccion, idEntrega);
    }

    @PostMapping("/{idSeccion}/{idEntrega}/{idInscripcion}/retirar")
    public String retirar(@PathVariable Long idSeccion, @PathVariable Long idEntrega,
                          @PathVariable Long idInscripcion,
                          @AuthenticationPrincipal UsuarioDetails usuario, RedirectAttributes redirect) {
        try {
            servicio.retirarPublicacion(usuario.getIdUsuario(), idSeccion, idEntrega, idInscripcion);
            redirect.addFlashAttribute("mensaje", "Nota retirada: el estudiante ya no la ve.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return volver(idSeccion, idEntrega);
    }

    private String volver(Long idSeccion, Long idEntrega) {
        return "redirect:/profesor/entregas/" + idSeccion + "/" + idEntrega;
    }
}
