package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.EntregaEstudianteService;
import com.grupo1.isw.practicas_pedagogicas.services.EntregaEstudianteService.DescargaEntrega;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** N-10: el estudiante sube o reemplaza el PDF de cada entrega del semestre hasta su fecha límite. */
@Controller
@RequestMapping("/estudiante/entregas")
@PreAuthorize("hasRole('ESTUDIANTE')")
public class EntregaEstudianteController {
    private static final String BASE = "/estudiante/entregas";

    private final EntregaEstudianteService servicio;

    public EntregaEstudianteController(EntregaEstudianteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("grupos", servicio.listar(usuario.getIdUsuario()));
        return "estudiante/entregas/lista";
    }

    @PostMapping("/{idInscripcion}/{idEntrega}")
    public String subir(@PathVariable Long idInscripcion, @PathVariable Long idEntrega,
                        @RequestParam(required = false) MultipartFile archivo,
                        @AuthenticationPrincipal UsuarioDetails usuario, RedirectAttributes redirect) {
        try {
            boolean reemplazo = servicio.subir(usuario.getIdUsuario(), idInscripcion, idEntrega, archivo);
            redirect.addFlashAttribute("mensaje",
                    reemplazo ? "Entrega reemplazada correctamente." : "Entrega subida correctamente.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE;
    }

    /** Único camino al archivo: la carpeta de subidas no se publica como recurso estático. */
    @GetMapping("/{idInscripcion}/{idEntrega}/descargar")
    public ResponseEntity<Resource> descargar(@PathVariable Long idInscripcion, @PathVariable Long idEntrega,
                                              @AuthenticationPrincipal UsuarioDetails usuario) {
        DescargaEntrega descarga = servicio.descargar(usuario.getIdUsuario(), idInscripcion, idEntrega);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(descarga.nombreSugerido()).build().toString())
                .body(new PathResource(descarga.ruta()));
    }
}
