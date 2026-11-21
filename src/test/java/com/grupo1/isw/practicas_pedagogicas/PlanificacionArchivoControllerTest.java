package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.controllers.PlanificacionArchivoColaboradorController;
import com.grupo1.isw.practicas_pedagogicas.controllers.PlanificacionArchivoController;
import com.grupo1.isw.practicas_pedagogicas.controllers.SubidaExcedidaAdvice;
import com.grupo1.isw.practicas_pedagogicas.models.RolUsuario;
import com.grupo1.isw.practicas_pedagogicas.models.Usuario;
import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionArchivoService;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionArchivoService.ArchivoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionArchivoService.ResultadoSubida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/** HU-31/32: respuestas HTTP de la subida y de la vista del PDF. */
@ExtendWith(MockitoExtension.class)
class PlanificacionArchivoControllerTest {

    private static final Long ID_ESTUDIANTE = 4L;
    private static final Long ID_COLABORADOR = 1L;
    private static final Long ID_PLANIFICACION = 10L;

    @Mock
    private PlanificacionArchivoService archivoService;

    private static UsuarioDetails usuario(Long id, RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setRol(rol);
        return new UsuarioDetails(usuario);
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("archivo", "plan.pdf", "application/pdf", new byte[]{1});
    }

    @Test
    @DisplayName("HU-31/32. Una subida correcta responde 200 con el resultado")
    void subir_ok() {
        ResultadoSubida resultado = new ResultadoSubida(ID_PLANIFICACION, "plan.pdf", 1L,
                LocalDateTime.of(2026, 10, 1, 9, 0), false, List.of());
        MockMultipartFile archivo = pdf();
        when(archivoService.subirPdf(ID_ESTUDIANTE, 7L, null, archivo)).thenReturn(resultado);

        ResponseEntity<?> respuesta = new PlanificacionArchivoController(archivoService)
                .subir(7L, null, archivo, usuario(ID_ESTUDIANTE, RolUsuario.ESTUDIANTE));

        assertEquals(200, respuesta.getStatusCode().value());
        assertSame(resultado, respuesta.getBody());
    }

    @Test
    @DisplayName("HU-31/32. Un archivo inválido responde 400 con el mensaje en JSON")
    void subir_archivoInvalido_responde400() {
        when(archivoService.subirPdf(ID_ESTUDIANTE, 7L, null, null))
                .thenThrow(new IllegalArgumentException("Debe seleccionar un archivo"));

        ResponseEntity<?> respuesta = new PlanificacionArchivoController(archivoService)
                .subir(7L, null, null, usuario(ID_ESTUDIANTE, RolUsuario.ESTUDIANTE));

        assertEquals(400, respuesta.getStatusCode().value());
        assertEquals(Map.of("error", "Debe seleccionar un archivo"), respuesta.getBody());
    }

    @Test
    @DisplayName("HU-31/32. Cambiar el PDF de una planificación no editable responde 409")
    void subir_noEditable_responde409() {
        MockMultipartFile archivo = pdf();
        when(archivoService.subirPdf(ID_ESTUDIANTE, null, ID_PLANIFICACION, archivo))
                .thenThrow(new IllegalStateException("Solo se puede cambiar el PDF de una planificación en borrador o rechazada"));

        ResponseEntity<?> respuesta = new PlanificacionArchivoController(archivoService)
                .subir(null, ID_PLANIFICACION, archivo, usuario(ID_ESTUDIANTE, RolUsuario.ESTUDIANTE));

        assertEquals(409, respuesta.getStatusCode().value());
    }

    @Test
    @DisplayName("HU-31/32. Si el servicio da 403/404 la excepción se propaga tal cual")
    void subir_sinPermiso_sePropaga() {
        MockMultipartFile archivo = pdf();
        when(archivoService.subirPdf(ID_ESTUDIANTE, 7L, null, archivo))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class, () -> new PlanificacionArchivoController(archivoService)
                .subir(7L, null, archivo, usuario(ID_ESTUDIANTE, RolUsuario.ESTUDIANTE)));
    }

    @Test
    @DisplayName("HU-31/32. El estudiante recibe su PDF como application/pdf y 'inline' para la vista previa")
    void ver_estudiante_esInline() {
        when(archivoService.obtenerParaEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenReturn(new ArchivoPlanificacion(Path.of("uuid.pdf"), "Planificacion-10.pdf"));

        ResponseEntity<Resource> respuesta = new PlanificacionArchivoController(archivoService)
                .ver(ID_PLANIFICACION, usuario(ID_ESTUDIANTE, RolUsuario.ESTUDIANTE));

        assertEquals(MediaType.APPLICATION_PDF, respuesta.getHeaders().getContentType());
        String disposicion = respuesta.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertTrue(disposicion.startsWith("inline"), disposicion);
        assertTrue(disposicion.contains("Planificacion-10.pdf"), disposicion);
    }

    @Test
    @DisplayName("HU-31/32. El colaborador recibe el PDF por su propia ruta, con su propio id")
    void ver_colaborador_esInline() {
        when(archivoService.obtenerParaColaborador(ID_PLANIFICACION, ID_COLABORADOR))
                .thenReturn(new ArchivoPlanificacion(Path.of("uuid.pdf"), "Planificacion-10.pdf"));

        ResponseEntity<Resource> respuesta = new PlanificacionArchivoColaboradorController(archivoService)
                .ver(ID_PLANIFICACION, usuario(ID_COLABORADOR, RolUsuario.COLABORADOR));

        assertEquals(MediaType.APPLICATION_PDF, respuesta.getHeaders().getContentType());
        assertTrue(respuesta.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("inline"));
    }

    // ---------------------------------------------------- límite de tamaño

    @Test
    @DisplayName("HU-31/32. Superar el límite de subida en la ruta del PDF responde 413 con JSON")
    void limiteExcedido_enLaRutaDelPdf_responde413() {
        MockHttpServletRequest peticion = new MockHttpServletRequest("POST", "/estudiante/planificaciones/archivo");
        peticion.setRequestURI("/estudiante/planificaciones/archivo");

        ResponseEntity<Map<String, String>> respuesta = new SubidaExcedidaAdvice()
                .excedida(new MaxUploadSizeExceededException(21L * 1024 * 1024), peticion);

        assertEquals(413, respuesta.getStatusCode().value());
        assertTrue(respuesta.getBody().get("error").contains("20 MB"));
    }

    @Test
    @DisplayName("HU-31/32. En otras rutas el error de tamaño no se intercepta: el comportamiento actual se conserva")
    void limiteExcedido_enOtraRuta_sePropaga() {
        MockHttpServletRequest peticion = new MockHttpServletRequest("POST", "/estudiante/documentos");
        peticion.setRequestURI("/estudiante/documentos");
        MaxUploadSizeExceededException error = new MaxUploadSizeExceededException(21L * 1024 * 1024);

        MaxUploadSizeExceededException lanzada = assertThrows(MaxUploadSizeExceededException.class,
                () -> new SubidaExcedidaAdvice().excedida(error, peticion));

        assertSame(error, lanzada);
    }
}
