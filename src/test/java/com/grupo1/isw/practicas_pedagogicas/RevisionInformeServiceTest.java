package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.InformeRevisionResponse;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeFinalRepository;
import com.grupo1.isw.practicas_pedagogicas.services.NotificacionService;
import com.grupo1.isw.practicas_pedagogicas.services.RevisionInformeService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevisionInformeServiceTest {

    private static final Long ID_INFORME = 1L;
    private static final Long ID_PROFESOR = 3L;
    private static final Long ID_OTRO_PROFESOR = 99L;
    private static final BigDecimal SUGERIDA = new BigDecimal("6.2");

    @Mock
    private InformeFinalRepository informeFinalRepository;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private RevisionInformeService service;

    // ---------- helpers ----------

    private static ProfesorAsignatura profesor(Long id) {
        ProfesorAsignatura profesor = new ProfesorAsignatura();
        profesor.setIdUsuario(id);
        return profesor;
    }

    /** Informe recién generado: con nota sugerida, sin validar ni publicar. */
    private static InformeFinal informe(Long id, Long idProfesorSeccion, Long idCorrector) {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(4L);
        estudiante.setNombre("Camila Soto");

        Asignatura asignatura = new Asignatura("Práctica Pedagógica", Ciclo.FINAL, 2);
        SeccionAsignatura seccion = new SeccionAsignatura(asignatura, profesor(idProfesorSeccion), 2026, 2);
        InscripcionAsignatura inscripcion = new InscripcionAsignatura(estudiante, seccion, null);

        InformeFinal informe = new InformeFinal(inscripcion, profesor(idCorrector));
        informe.setIdInforme(id);
        informe.setCalificacionSugerida(SUGERIDA);
        informe.setFechaGeneracion(LocalDateTime.of(2026, 10, 20, 9, 0));
        return informe;
    }

    private InformeFinal dadoUnInformeDelProfesor() {
        InformeFinal informe = informe(ID_INFORME, ID_PROFESOR, ID_PROFESOR);
        when(informeFinalRepository.findByIdParaRevision(ID_INFORME)).thenReturn(Optional.of(informe));
        return informe;
    }

    private InformeFinal dadoUnInformeValidado() {
        InformeFinal informe = dadoUnInformeDelProfesor();
        informe.setCalificacionFinal(new BigDecimal("6.5"));
        informe.setFechaValidacion(LocalDateTime.of(2026, 10, 21, 15, 30));
        return informe;
    }

    private InformeFinal dadoUnInformePublicado() {
        InformeFinal informe = dadoUnInformeValidado();
        informe.setFechaPublicacion(LocalDateTime.of(2026, 10, 22, 9, 0));
        return informe;
    }

    // ---------- listar / obtener ----------

    @Test
    @DisplayName("1. Lista primero los informes sin publicar y luego los publicados")
    void listar_pendientesPrimero() {
        InformeFinal publicado = informe(1L, ID_PROFESOR, ID_PROFESOR);
        publicado.setFechaPublicacion(LocalDateTime.of(2026, 10, 22, 9, 0));
        InformeFinal pendiente = informe(2L, ID_PROFESOR, ID_PROFESOR);

        // la consulta los entrega por fecha: el publicado viene primero
        when(informeFinalRepository.findParaRevisionDelProfesor(ID_PROFESOR))
                .thenReturn(List.of(publicado, pendiente));

        List<InformeRevisionResponse> resultado = service.listarInformes(ID_PROFESOR);

        assertEquals(2L, resultado.get(0).idInforme());
        assertEquals(InformeRevisionResponse.Estado.PENDIENTE, resultado.get(0).estado());
        assertEquals(1L, resultado.get(1).idInforme());
        assertEquals(InformeRevisionResponse.Estado.PUBLICADO, resultado.get(1).estado());
    }

    @Test
    @DisplayName("2. Obtener devuelve la nota sugerida, el estudiante y el estado PENDIENTE")
    void obtener_devuelveDatosDelInforme() {
        dadoUnInformeDelProfesor();

        InformeRevisionResponse respuesta = service.obtenerInforme(ID_PROFESOR, ID_INFORME);

        assertEquals("Camila Soto", respuesta.nombreEstudiante());
        assertEquals("Práctica Pedagógica", respuesta.nombreAsignatura());
        assertEquals(0, SUGERIDA.compareTo(respuesta.calificacionSugerida()));
        assertEquals(InformeRevisionResponse.Estado.PENDIENTE, respuesta.estado());
        assertTrue(respuesta.puedeAjustar());
    }

    @Test
    @DisplayName("3. Lanza 404 si el informe no existe")
    void obtener_noExiste_lanza404() {
        when(informeFinalRepository.findByIdParaRevision(ID_INFORME)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerInforme(ID_PROFESOR, ID_INFORME));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("4. Lanza 403 si el informe es de otro profesor")
    void obtener_deOtroProfesor_lanza403() {
        dadoUnInformeDelProfesor();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerInforme(ID_OTRO_PROFESOR, ID_INFORME));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("5. El corrector asignado también puede revisar aunque no sea el profesor de la sección")
    void obtener_comoCorrector_permitido() {
        InformeFinal informe = informe(ID_INFORME, ID_OTRO_PROFESOR, ID_PROFESOR);
        when(informeFinalRepository.findByIdParaRevision(ID_INFORME)).thenReturn(Optional.of(informe));

        InformeRevisionResponse respuesta = service.obtenerInforme(ID_PROFESOR, ID_INFORME);

        assertEquals(ID_INFORME, respuesta.idInforme());
    }

    // ---------- guardar calificación final ----------

    @Test
    @DisplayName("6. Ajusta la nota: guarda la final, marca la validación y conserva la sugerida")
    void guardar_ajustaLaNota() {
        InformeFinal informe = dadoUnInformeDelProfesor();

        InformeRevisionResponse respuesta = service.guardarCalificacionFinal(ID_PROFESOR, ID_INFORME, "5,5");

        assertEquals(new BigDecimal("5.5"), informe.getCalificacionFinal());
        assertNotNull(informe.getFechaValidacion());
        assertNull(informe.getFechaPublicacion());
        assertEquals(SUGERIDA, informe.getCalificacionSugerida(), "la sugerida no se toca");
        assertTrue(respuesta.ajustada());
        assertEquals(InformeRevisionResponse.Estado.VALIDADO, respuesta.estado());
        verify(informeFinalRepository).save(informe);
    }

    @Test
    @DisplayName("7. Aceptar la nota sugerida tal cual también valida el informe")
    void guardar_aceptaLaSugerida() {
        InformeFinal informe = dadoUnInformeDelProfesor();

        InformeRevisionResponse respuesta = service.guardarCalificacionFinal(ID_PROFESOR, ID_INFORME, "6.2");

        assertEquals(0, SUGERIDA.compareTo(informe.getCalificacionFinal()));
        assertNotNull(informe.getFechaValidacion());
        assertFalse(respuesta.ajustada());
        verify(informeFinalRepository).save(informe);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "1,0", "7", "7.0", "5,50", " 4,5 "})
    @DisplayName("8. Acepta notas válidas: límites, coma o punto, ceros sobrantes y espacios")
    void guardar_notasValidas(String texto) {
        InformeFinal informe = dadoUnInformeDelProfesor();

        service.guardarCalificacionFinal(ID_PROFESOR, ID_INFORME, texto);

        assertNotNull(informe.getCalificacionFinal());
        assertEquals(1, informe.getCalificacionFinal().scale(), "se guarda con un decimal");
        verify(informeFinalRepository).save(informe);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "abc", "5,5,5", "0,9", "7,1", "8", "-3", "5,55", "6,25", "NaN", "12345678901"})
    @DisplayName("9. Rechaza notas vacías, no numéricas, fuera de 1,0-7,0 o con más de un decimal")
    void guardar_notasInvalidas_lanzaIllegalArgument(String texto) {
        InformeFinal informe = dadoUnInformeDelProfesor();

        assertThrows(IllegalArgumentException.class,
                () -> service.guardarCalificacionFinal(ID_PROFESOR, ID_INFORME, texto));

        verify(informeFinalRepository, never()).save(any());
        assertNull(informe.getCalificacionFinal());
        assertNull(informe.getFechaValidacion());
    }

    @Test
    @DisplayName("10. No permite ajustar un informe ya publicado")
    void guardar_yaPublicado_lanzaIllegalState() {
        InformeFinal informe = dadoUnInformePublicado();

        assertThrows(IllegalStateException.class,
                () -> service.guardarCalificacionFinal(ID_PROFESOR, ID_INFORME, "4,0"));

        verify(informeFinalRepository, never()).save(any());
        assertEquals(new BigDecimal("6.5"), informe.getCalificacionFinal());
    }

    @Test
    @DisplayName("11. Otro profesor no puede ajustar la nota (403) y no se guarda nada")
    void guardar_deOtroProfesor_lanza403() {
        InformeFinal informe = dadoUnInformeDelProfesor();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.guardarCalificacionFinal(ID_OTRO_PROFESOR, ID_INFORME, "5,5"));

        assertEquals(403, ex.getStatusCode().value());
        verify(informeFinalRepository, never()).save(any());
        assertNull(informe.getCalificacionFinal());
    }

    @Test
    @DisplayName("12. Puede volver a ajustar la nota mientras no se publique")
    void guardar_reajusteAntesDePublicar() {
        InformeFinal informe = dadoUnInformeValidado();

        service.guardarCalificacionFinal(ID_PROFESOR, ID_INFORME, "6,8");

        assertEquals(new BigDecimal("6.8"), informe.getCalificacionFinal());
        verify(informeFinalRepository).save(informe);
    }

    // ---------- publicar ----------

    @Test
    @DisplayName("13. Publica un informe cuya nota ya fue fijada")
    void publicar_ok() {
        InformeFinal informe = dadoUnInformeValidado();

        InformeRevisionResponse respuesta = service.publicar(ID_PROFESOR, ID_INFORME);

        assertNotNull(informe.getFechaPublicacion());
        assertEquals(InformeRevisionResponse.Estado.PUBLICADO, respuesta.estado());
        assertFalse(respuesta.puedeAjustar());
        verify(informeFinalRepository).save(informe);
    }

    @Test
    @DisplayName("14. No publica si el profesor aún no fijó la nota final")
    void publicar_sinNotaFinal_lanzaIllegalState() {
        InformeFinal informe = dadoUnInformeDelProfesor();

        assertThrows(IllegalStateException.class, () -> service.publicar(ID_PROFESOR, ID_INFORME));

        verify(informeFinalRepository, never()).save(any());
        assertNull(informe.getFechaPublicacion());
    }

    @Test
    @DisplayName("15. No publica dos veces el mismo informe y conserva la fecha original")
    void publicar_yaPublicado_lanzaIllegalState() {
        InformeFinal informe = dadoUnInformePublicado();
        LocalDateTime fechaOriginal = informe.getFechaPublicacion();

        assertThrows(IllegalStateException.class, () -> service.publicar(ID_PROFESOR, ID_INFORME));

        verify(informeFinalRepository, never()).save(any());
        assertEquals(fechaOriginal, informe.getFechaPublicacion());
    }

    @Test
    @DisplayName("16. Otro profesor no puede publicar (403) y no se guarda nada")
    void publicar_deOtroProfesor_lanza403() {
        InformeFinal informe = dadoUnInformeValidado();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.publicar(ID_OTRO_PROFESOR, ID_INFORME));

        assertEquals(403, ex.getStatusCode().value());
        verify(informeFinalRepository, never()).save(any());
        assertNull(informe.getFechaPublicacion());
    }

    @Test
    @DisplayName("17. Publicar un informe que no existe lanza 404")
    void publicar_noExiste_lanza404() {
        when(informeFinalRepository.findByIdParaRevision(ID_INFORME)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.publicar(ID_PROFESOR, ID_INFORME));

        assertEquals(404, ex.getStatusCode().value());
    }

    // ---------- HU-17: notificación al estudiante ----------

    @Test
    @DisplayName("HU-17. Al publicar se notifica al estudiante con el enlace a su informe, sin la nota")
    void publicar_notificaAlEstudiante() {
        dadoUnInformeValidado();

        service.publicar(ID_PROFESOR, ID_INFORME);

        verify(notificacionService).crear(4L, NotificacionService.TIPO_INFORME_FINAL,
                "Tu informe final de desempeño de Práctica Pedagógica (semestre 2 de 2026) ya está disponible.",
                "/mis-informes/1");
    }

    @Test
    @DisplayName("HU-17. Si no se puede publicar, el estudiante no recibe notificación")
    void publicar_fallido_noNotifica() {
        dadoUnInformeDelProfesor(); // sin nota final: no se puede publicar

        assertThrows(IllegalStateException.class, () -> service.publicar(ID_PROFESOR, ID_INFORME));

        verify(notificacionService, never()).crear(anyLong(), anyString(), anyString(), anyString());
    }
}
