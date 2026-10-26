package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.DocumentoService;
import com.grupo1.isw.practicas_pedagogicas.services.DocumentoService.DescargaDocumento;
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

@Controller
@RequestMapping("/estudiante/documentos")
@PreAuthorize("hasRole('ESTUDIANTE')")
public class DocumentoEstudianteController {
    private static final String BASE = "/estudiante/documentos";
    private static final String VISTA_LISTA = "estudiante/documentos/lista";

    private final DocumentoService documentoService;

    public DocumentoEstudianteController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("asignaturas",
                documentoService.listarEntregasDelEstudiante(usuario.getIdUsuario()));
        return VISTA_LISTA;
    }

    /** Único camino al archivo: la carpeta de subidas no se publica como recurso estático. */
    @GetMapping("/{id}/descargar")
    public ResponseEntity<Resource> descargar(@PathVariable Long id,
                                              @AuthenticationPrincipal UsuarioDetails usuario) {
        DescargaDocumento descarga = documentoService.obtenerParaDescarga(usuario.getIdUsuario(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(descarga.nombreSugerido()).build().toString())
                .body(new PathResource(descarga.ruta()));
    }

    @PostMapping
    public String subir(@RequestParam Long idInscripcion,
                        @RequestParam Long idPautaDocumento,
                        @RequestParam(required = false) MultipartFile archivo,
                        @AuthenticationPrincipal UsuarioDetails usuario,
                        RedirectAttributes redirectAttributes) {
        try {
            boolean reemplazo = documentoService.subirDocumento(
                    usuario.getIdUsuario(), idInscripcion, idPautaDocumento, archivo);
            redirectAttributes.addFlashAttribute("mensaje",
                    reemplazo ? "Documento reemplazado correctamente." : "Documento subido correctamente.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE;
    }
}
