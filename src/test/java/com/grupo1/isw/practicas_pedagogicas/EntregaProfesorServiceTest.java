package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository.ContextoEntrega;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository.ResumenEntrega;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository.SeccionDatos;
import com.grupo1.isw.practicas_pedagogicas.services.EntregaEstudianteService;
import com.grupo1.isw.practicas_pedagogicas.services.EntregaProfesorService;
import com.grupo1.isw.practicas_pedagogicas.services.EstructuraInvalidaException;
import com.grupo1.isw.practicas_pedagogicas.services.NotaAutomaticaService;
import com.grupo1.isw.practicas_pedagogicas.services.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntregaProfesorServiceTest {
    private static final long SECCION = 1L;
    private static final long PROFESOR = 5L;
    private static final long ENTREGA = 20L;
    private static final long INSCRIPCION = 10L;
    private static final long ESTUDIANTE = 7L;
    private static final long REGISTRO = 55L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 5, 15, 12, 0);

    @Mock
    SeccionEstructuraRepository secciones;
    @Mock
    EntregaEstudianteRepository repositorio;
    @Mock
    EntregaEstudianteService entregaEstudianteService;
    @Mock
    NotaAutomaticaService notaAutomatica;
    @Mock
    NotificacionService notificaciones;

    private EntregaProfesorService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-05-15T16:00:00Z"), ZoneId.of("America/Santiago"));
        servicio = new EntregaProfesorService(secciones, repositorio, entregaEstudianteService, notaAutomatica,
                notificaciones, reloj);
        when(secciones.buscar(SECCION)).thenReturn(Optional.of(new SeccionDatos(SECCION, 10L, "Práctica", 2, 2026, 1,
                PROFESOR, "Profesor", true, null)));
        usar(contexto(REGISTRO, "informe.pdf", new BigDecimal("5.5"), false, false));
    }

    private void usar(ContextoEntrega contexto) {
        when(repositorio.contexto(INSCRIPCION, ENTREGA)).thenReturn(Optional.of(contexto));
    }

    private static ContextoEntrega contexto(Long idRegistro, String archivo, BigDecimal nota, boolean automatica,
                                            boolean publicada) {
        return new ContextoEntrega(INSCRIPCION, ESTUDIANTE, "EN_CURSO", SECCION, true, SECCION, "Informe 1", true,
                null, idRegistro, archivo, null, nota, null, automatica, publicada);
    }

    @Test
    @DisplayName("1. N-13: pone la nota (con coma) y el comentario a un trabajo entregado")
    void calificaTrabajoEntregado() {
        usar(contexto(REGISTRO, "informe.pdf", null, false, false));

        servicio.calificar(PROFESOR, SECCION, ENTREGA, INSCRIPCION, "6,3", "  Muy buen trabajo ");

        verify(repositorio).guardarNota(REGISTRO, new BigDecimal("6.3"), "Muy buen trabajo", AHORA);
    }

    @Test
    @DisplayName("2. N-14: cambia el 1,0 automático de quien no entregó")
    void modificaNotaAutomatica() {
        usar(contexto(REGISTRO, null, new BigDecimal("1.0"), true, false));

        servicio.calificar(PROFESOR, SECCION, ENTREGA, INSCRIPCION, "4.0", "");

        verify(repositorio).guardarNota(REGISTRO, new BigDecimal("4.0"), null, AHORA);
    }

    @Test
    @DisplayName("3. No se califica a quien no ha entregado y tiene el plazo abierto")
    void sinRegistro() {
        usar(contexto(null, null, null, false, false));

        assertThrows(IllegalStateException.class,
                () -> servicio.calificar(PROFESOR, SECCION, ENTREGA, INSCRIPCION, "5.0", null));
        verify(repositorio, never()).guardarNota(anyLong(), org.mockito.ArgumentMatchers.<BigDecimal>any(),
                anyString(), org.mockito.ArgumentMatchers.<LocalDateTime>any());
    }

    @Test
    @DisplayName("4. La nota debe ser de 1,0 a 7,0 y el comentario no puede ser larguísimo")
    void validaNotaYComentario() {
        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> servicio.calificar(PROFESOR, SECCION, ENTREGA, INSCRIPCION, "7.5", "x".repeat(2001)));

        assertEquals(2, ex.getErrores().size());
    }

    @Test
    @DisplayName("5. N-15: publicar deja la nota visible y avisa al estudiante")
    void publica() {
        servicio.publicar(PROFESOR, SECCION, ENTREGA, INSCRIPCION);

        verify(repositorio).cambiarPublicada(REGISTRO, true);
        verify(notificaciones).crear(ESTUDIANTE, EntregaProfesorService.TIPO_NOTIFICACION,
                "Se publicó tu nota de «Informe 1»: 5,5", "/estudiante/entregas");
    }

    @Test
    @DisplayName("6. No se publica sin nota ni dos veces")
    void noPublicaSinNotaNiDosVeces() {
        usar(contexto(REGISTRO, "informe.pdf", null, false, false));
        assertThrows(IllegalStateException.class, () -> servicio.publicar(PROFESOR, SECCION, ENTREGA, INSCRIPCION));

        usar(contexto(REGISTRO, "informe.pdf", new BigDecimal("5.5"), false, true));
        assertThrows(IllegalStateException.class, () -> servicio.publicar(PROFESOR, SECCION, ENTREGA, INSCRIPCION));

        verify(repositorio, never()).cambiarPublicada(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("7. Se puede retirar la publicación de una nota publicada")
    void retira() {
        usar(contexto(REGISTRO, "informe.pdf", new BigDecimal("5.5"), false, true));

        servicio.retirarPublicacion(PROFESOR, SECCION, ENTREGA, INSCRIPCION);

        verify(repositorio).cambiarPublicada(REGISTRO, false);
    }

    @Test
    @DisplayName("8. Retirar algo que no está publicado da error")
    void retiraNoPublicada() {
        assertThrows(IllegalStateException.class,
                () -> servicio.retirarPublicacion(PROFESOR, SECCION, ENTREGA, INSCRIPCION));
    }

    @Test
    @DisplayName("9. Otro profesor no puede ver ni calificar (403)")
    void profesorAjeno() {
        assertThrows(ResponseStatusException.class, () -> servicio.resumen(777L, SECCION));
        assertThrows(ResponseStatusException.class,
                () -> servicio.calificar(777L, SECCION, ENTREGA, INSCRIPCION, "5.0", null));
        assertThrows(ResponseStatusException.class, () -> servicio.publicar(777L, SECCION, ENTREGA, INSCRIPCION));
    }

    @Test
    @DisplayName("10. Una inscripción o entrega de otra sección da 404")
    void deOtraSeccion() {
        usar(new ContextoEntrega(INSCRIPCION, ESTUDIANTE, "EN_CURSO", 2L, true, SECCION, "Informe 1", true, null,
                REGISTRO, "informe.pdf", null, null, null, false, false));

        assertThrows(ResponseStatusException.class,
                () -> servicio.calificar(PROFESOR, SECCION, ENTREGA, INSCRIPCION, "5.0", null));
    }

    @Test
    @DisplayName("11. N-12: el resumen revisa primero las notas automáticas y lista las entregas del semestre")
    void resumen() {
        when(repositorio.resumenDeLaSeccion(SECCION)).thenReturn(List.of(
                new ResumenEntrega(ENTREGA, "Informe 1", BigDecimal.TEN, LocalDate.of(2026, 5, 1), 5, 3, 0)));

        List<ResumenEntrega> resumen = servicio.resumen(PROFESOR, SECCION);

        verify(notaAutomatica).aplicar(SECCION);
        assertEquals(2, resumen.get(0).faltan());
    }

    @Test
    @DisplayName("12. El detalle marca si el plazo ya venció y trae a los estudiantes")
    void detalle() {
        when(repositorio.resumenDeLaSeccion(SECCION)).thenReturn(List.of(
                new ResumenEntrega(ENTREGA, "Informe 1", BigDecimal.TEN, LocalDate.of(2026, 5, 1), 5, 3, 0)));
        when(repositorio.estudiantesDeLaEntrega(SECCION, ENTREGA)).thenReturn(List.of());

        var detalle = servicio.detalle(PROFESOR, SECCION, ENTREGA);

        assertTrue(detalle.plazoVencido());
        assertEquals("Informe 1", detalle.entrega().nombre());
        assertThrows(ResponseStatusException.class, () -> servicio.detalle(PROFESOR, SECCION, 999L));
    }
}
