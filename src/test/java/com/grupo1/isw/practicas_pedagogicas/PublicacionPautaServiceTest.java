package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository.ContextoEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository.SeccionDatos;
import com.grupo1.isw.practicas_pedagogicas.services.CalculadoraNota;
import com.grupo1.isw.practicas_pedagogicas.services.NotasDePauta;
import com.grupo1.isw.practicas_pedagogicas.services.NotificacionService;
import com.grupo1.isw.practicas_pedagogicas.services.PublicacionPautaService;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.ESTUDIANTE;
import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.INSCRIPCION;
import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.SECCION;
import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.bd;
import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.pauta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicacionPautaServiceTest {
    private static final long PROFESOR = 5L;
    private static final long EVALUACION = 501L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 5, 15, 12, 0);

    @Mock
    SeccionEstructuraRepository secciones;
    @Mock
    EvaluacionesEsperadasRepository repositorio;
    @Mock
    NotificacionService notificaciones;

    private final CalculadoraNota calculadora = new CalculadoraNota(bd("60"));
    private PublicacionPautaService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-05-15T16:00:00Z"), ZoneId.of("America/Santiago"));
        servicio = new PublicacionPautaService(secciones, repositorio, calculadora, notificaciones, reloj);
        when(secciones.buscar(SECCION)).thenReturn(Optional.of(new SeccionDatos(SECCION, 10L, "Práctica", 2, 2026, 1,
                PROFESOR, "Profesor", true, null)));
        usar(contexto(bd("18"), false));
    }

    private void usar(ContextoEvaluacion contexto) {
        when(repositorio.contexto(EVALUACION)).thenReturn(Optional.of(contexto));
    }

    private static ContextoEvaluacion contexto(BigDecimal puntaje, boolean publicada) {
        return new ContextoEvaluacion(EVALUACION, INSCRIPCION, ESTUDIANTE, SECCION, PROFESOR, "Pauta 1", puntaje,
                bd("20"), publicada);
    }

    @Test
    @DisplayName("1. Publicar deja la pauta visible y avisa al estudiante")
    void publica() {
        servicio.publicar(PROFESOR, SECCION, EVALUACION);

        verify(repositorio).cambiarPublicada(EVALUACION, true, AHORA);
        verify(notificaciones).crear(ESTUDIANTE, PublicacionPautaService.TIPO_NOTIFICACION,
                "Se publicó la evaluación «Pauta 1» de tu práctica.", "/estudiante/evaluaciones");
    }

    @Test
    @DisplayName("2. No se publica una pauta que el evaluador todavía no aplicó")
    void sinAplicar() {
        usar(contexto(null, false));

        assertThrows(IllegalStateException.class, () -> servicio.publicar(PROFESOR, SECCION, EVALUACION));

        verify(repositorio, never()).cambiarPublicada(anyLong(), anyBoolean(),
                org.mockito.ArgumentMatchers.<LocalDateTime>any());
    }

    @Test
    @DisplayName("3. No se publica dos veces")
    void yaPublicada() {
        usar(contexto(bd("18"), true));

        assertThrows(IllegalStateException.class, () -> servicio.publicar(PROFESOR, SECCION, EVALUACION));

        verify(notificaciones, never()).crear(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("4. Se puede retirar la publicación, pero solo si estaba publicada")
    void retira() {
        usar(contexto(bd("18"), true));
        servicio.retirar(PROFESOR, SECCION, EVALUACION);
        verify(repositorio).cambiarPublicada(EVALUACION, false, AHORA);

        usar(contexto(bd("18"), false));
        assertThrows(IllegalStateException.class, () -> servicio.retirar(PROFESOR, SECCION, EVALUACION));
    }

    @Test
    @DisplayName("5. Otro profesor no puede publicar (403) y una evaluación de otra sección da 404")
    void controlDeAcceso() {
        assertThrows(ResponseStatusException.class, () -> servicio.publicar(999L, SECCION, EVALUACION));
        assertThrows(ResponseStatusException.class, () -> servicio.panel(999L, SECCION));

        usar(new ContextoEvaluacion(EVALUACION, INSCRIPCION, ESTUDIANTE, 2L, PROFESOR, "Pauta 1", bd("18"), bd("20"),
                false));
        assertThrows(ResponseStatusException.class, () -> servicio.publicar(PROFESOR, SECCION, EVALUACION));
        assertThrows(ResponseStatusException.class, () -> servicio.publicar(PROFESOR, SECCION, 12345L));
    }

    @Test
    @DisplayName("6. Publicar todas solo toca las aplicadas que aún no se publican")
    void publicarTodas() {
        when(repositorio.deLaSeccion(SECCION)).thenReturn(List.of(
                pauta(1, "COLABORADOR", "30", "20", "18", null, false),
                pauta(2, "TUTOR", "30", "20", null, null, false),
                pauta(3, "COLABORADOR", "30", "20", "10", null, true)));
        when(repositorio.contexto(501L)).thenReturn(Optional.of(contexto(bd("18"), false)));

        int publicadas = servicio.publicarTodas(PROFESOR, SECCION);

        assertEquals(1, publicadas);
        verify(repositorio).cambiarPublicada(501L, true, AHORA);
    }

    @Test
    @DisplayName("7. El panel trae la nota de cada pauta aplicada y deja vacía la de las pendientes")
    void panel() {
        when(repositorio.deLaSeccion(SECCION)).thenReturn(List.of(
                pauta(1, "COLABORADOR", "30", "20", "18", null, false),
                pauta(2, "TUTOR", "30", "20", null, null, false)));

        var panel = servicio.panel(PROFESOR, SECCION);

        assertEquals(0, bd("6.3").compareTo(panel.filas().get(0).nota()));
        assertNull(panel.filas().get(1).nota());
        assertEquals(1, panel.aplicadas());
        assertEquals(1, panel.porPublicar());
    }

    @Test
    @DisplayName("8. La nota de una pauta usa la exigencia del sistema: 60 % de logro es un 4,0")
    void notaDePauta() {
        var exigencia = pauta(1, "COLABORADOR", "30", "20", "12", null, false);
        var perfecta = pauta(2, "COLABORADOR", "30", "20", "20", null, false);
        var nula = pauta(3, "COLABORADOR", "30", "0", "0", null, false);

        assertEquals(0, bd("4.0").compareTo(NotasDePauta.redondeada(calculadora, exigencia)));
        assertEquals(0, bd("7.0").compareTo(NotasDePauta.redondeada(calculadora, perfecta)));
        assertNull(NotasDePauta.exacta(calculadora, nula));
    }
}
