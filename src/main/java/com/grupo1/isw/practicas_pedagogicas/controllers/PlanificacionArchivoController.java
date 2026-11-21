package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionArchivoService;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionArchivoService.ArchivoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionArchivoService.ResultadoSubida;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * PDF de la planificación del estudiante. La subida responde JSON porque la página la hace con
 * XMLHttpRequest (único modo de mostrar el avance de la carga); el formulario debe enviar también el token CSRF.
 */
@RestController
@RequestMapping("/estudiante/planificaciones")
@PreAuthorize("hasRole('ESTUDIANTE')")
public class PlanificacionArchivoController {
    private final PlanificacionArchivoService archivoService;

    public PlanificacionArchivoController(PlanificacionArchivoService archivoService) {
        this.archivoService = archivoService;
    }

    /** Sin idPlanificacion crea un borrador en idLinea; con ella reemplaza el PDF de esa planificación. */
    @PostMapping("/archivo")
    public ResponseEntity<?> subir(@RequestParam(required = false) Long idLinea,
                                   @RequestParam(required = false) Long idPlanificacion,
                                   @RequestParam(required = false) MultipartFile archivo,
                                   @AuthenticationPrincipal UsuarioDetails usuario) {
        try {
            ResultadoSubida resultado =
                    archivoService.subirPdf(usuario.getIdUsuario(), idLinea, idPlanificacion, archivo);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    /** Se muestra "inline" para la vista previa en la página; la carpeta de subidas nunca se publica directamente. */
    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> ver(@PathVariable Long id, @AuthenticationPrincipal UsuarioDetails usuario) {
        return respuestaPdf(archivoService.obtenerParaEstudiante(usuario.getIdUsuario(), id));
    }

    static ResponseEntity<Resource> respuestaPdf(ArchivoPlanificacion archivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(archivo.nombreSugerido()).build().toString())
                .body(new PathResource(archivo.ruta()));
    }
}
