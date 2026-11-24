package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.repositories.AvisoNotificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository;
import com.grupo1.isw.practicas_pedagogicas.services.AvisoPlazoService;
import com.grupo1.isw.practicas_pedagogicas.services.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.COLABORADOR;
import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.TUTOR;
import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.pauta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AvisoPlazoServiceTest {
    private static final LocalDate HOY = LocalDate.of(2026, 5, 15);

    @Mock
    EvaluacionesEsperadasRepository repositorio;
    @Mock
    AvisoNotificacionRepository avisos;
    @Mock
    NotificacionService notificaciones;

    private AvisoPlazoService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-05-15T16:00:00Z"), ZoneId.of("America/Santiago"));
        servicio = new AvisoPlazoService(repositorio, avisos, notificaciones, reloj);
    }

    @Test
    @DisplayName("1. Avisa al evaluador cuando el plazo de una pauta pendiente está a 3 días o menos")
    void avisoPrevio() {
        when(repositorio.conPlazo()).thenReturn(List.of(
                pauta(1, "COLABORADOR", "30", "20", null, HOY.plusDays(3), false)));

        assertEquals(1, servicio.revisar());

        verify(notificaciones).crear(COLABORADOR, AvisoPlazoService.TIPO_PROXIMO,
                "Se acerca el plazo: la pauta «Pauta 1» de Camila Soto (Práctica Pedagógica) vence el 18/05/2026.",
                "/evaluador/pautas");
    }

    @Test
    @DisplayName("2. El mismo día del plazo también avisa")
    void avisoElMismoDia() {
        when(repositorio.conPlazo()).thenReturn(List.of(
                pauta(1, "TUTOR", "30", "20", null, HOY, false)));

        assertEquals(1, servicio.revisar());

        verify(notificaciones).crear(org.mockito.ArgumentMatchers.eq(TUTOR), org.mockito.ArgumentMatchers.eq(
                AvisoPlazoService.TIPO_PROXIMO), anyString(), anyString());
    }

    @Test
    @DisplayName("3. Después del plazo avisa que venció, aclarando que se puede aplicar igual")
    void avisoVencido() {
        when(repositorio.conPlazo()).thenReturn(List.of(
                pauta(1, "COLABORADOR", "30", "20", null, HOY.minusDays(2), false)));

        assertEquals(1, servicio.revisar());

        verify(notificaciones).crear(COLABORADOR, AvisoPlazoService.TIPO_VENCIDO,
                "Venció el plazo (13/05/2026) de la pauta «Pauta 1» de Camila Soto (Práctica Pedagógica). "
                        + "Sigue pendiente y puedes aplicarla igual.", "/evaluador/pautas");
    }

    @Test
    @DisplayName("4. No avisa si la pauta ya fue aplicada, si el plazo está lejos o si venció hace mucho")
    void sinAviso() {
        when(repositorio.conPlazo()).thenReturn(List.of(
                pauta(1, "COLABORADOR", "30", "20", "15", HOY.plusDays(1), false),
                pauta(2, "COLABORADOR", "30", "20", null, HOY.plusDays(4), false),
                pauta(3, "COLABORADOR", "30", "20", null,
                        HOY.minusDays(AvisoPlazoService.DIAS_MAXIMOS_ATRASO + 1), false)));

        assertEquals(0, servicio.revisar());

        verify(notificaciones, never()).crear(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("5. Un aviso que ya existe no se repite")
    void noRepite() {
        when(repositorio.conPlazo()).thenReturn(List.of(
                pauta(1, "COLABORADOR", "30", "20", null, HOY.plusDays(1), false)));
        when(avisos.existe(anyLong(), anyString(), anyString())).thenReturn(true);

        assertEquals(0, servicio.revisar());

        verify(notificaciones, never()).crear(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("6. La tarea programada no propaga errores")
    void programadaNoFalla() {
        doThrow(new RuntimeException("base caída")).when(repositorio).conPlazo();

        servicio.revisarProgramado();
    }
}
