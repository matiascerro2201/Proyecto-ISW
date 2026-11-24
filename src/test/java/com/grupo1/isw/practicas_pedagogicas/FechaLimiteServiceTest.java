package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaSemestreRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaSemestreRepository.EntregaFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaSemestreRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaSemestreRepository.PautaFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository.SeccionDatos;
import com.grupo1.isw.practicas_pedagogicas.services.FechaLimiteService;
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
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FechaLimiteServiceTest {
    private static final long SECCION = 1L;
    private static final long PROFESOR = 5L;
    private static final long COORDINADOR = 99L;

    @Mock
    SeccionEstructuraRepository secciones;
    @Mock
    EntregaSemestreRepository entregas;
    @Mock
    PautaSemestreRepository pautas;
    @Mock
    EntregaEstudianteRepository entregasEstudiantes;

    private FechaLimiteService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-05-15T16:00:00Z"), ZoneId.of("America/Santiago"));
        servicio = new FechaLimiteService(secciones, entregas, pautas, entregasEstudiantes, reloj);
        when(secciones.buscar(SECCION)).thenReturn(Optional.of(new SeccionDatos(SECCION, 10L, "Práctica", 2, 2026, 1,
                PROFESOR, "Profesor", true, null)));
        when(entregas.buscar(20L)).thenReturn(Optional.of(new EntregaFila(20L, SECCION, "Informe 1",
                new BigDecimal("30"), true, null, 1)));
        when(pautas.buscar(30L)).thenReturn(Optional.of(new PautaFila(30L, SECCION, "Pauta", "TUTOR", "UNICA", 1,
                new BigDecimal("20"), true, null, 2, BigDecimal.TEN)));
    }

    @Test
    @DisplayName("1. El profesor de la sección fija la fecha límite de una entrega")
    void profesorFijaEntrega() {
        servicio.fijarFechaEntrega(PROFESOR, false, SECCION, 20L, "2026-06-30");

        verify(entregas).cambiarFechaLimite(20L, LocalDate.of(2026, 6, 30));
    }

    @Test
    @DisplayName("2. El coordinador puede fijar fechas en cualquier sección")
    void coordinadorFija() {
        servicio.fijarFechaPauta(COORDINADOR, true, SECCION, 30L, "2026-07-01");

        verify(pautas).cambiarFechaLimite(30L, LocalDate.of(2026, 7, 1));
    }

    @Test
    @DisplayName("3. Un profesor que no es el de la sección no puede (403)")
    void profesorAjeno() {
        assertThrows(ResponseStatusException.class,
                () -> servicio.fijarFechaEntrega(777L, false, SECCION, 20L, "2026-06-30"));
        assertThrows(ResponseStatusException.class, () -> servicio.vista(777L, false, SECCION));

        verify(entregas, never()).cambiarFechaLimite(anyLong(), org.mockito.ArgumentMatchers.<LocalDate>any());
    }

    @Test
    @DisplayName("4. No se acepta una fecha anterior a hoy ni una fecha mal escrita")
    void fechaInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.fijarFechaEntrega(PROFESOR, false, SECCION, 20L, "2026-05-14"));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.fijarFechaEntrega(PROFESOR, false, SECCION, 20L, "15/06/2026"));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.fijarFechaPauta(PROFESOR, false, SECCION, 30L, "2026-01-01"));
    }

    @Test
    @DisplayName("5. Hoy es una fecha válida")
    void hoyEsValido() {
        servicio.fijarFechaEntrega(PROFESOR, false, SECCION, 20L, "2026-05-15");

        verify(entregas).cambiarFechaLimite(20L, LocalDate.of(2026, 5, 15));
    }

    @Test
    @DisplayName("6. Al mover el plazo de una entrega se quitan las notas 1,0 automáticas anteriores")
    void extenderQuitaNotasAutomaticas() {
        servicio.fijarFechaEntrega(PROFESOR, false, SECCION, 20L, "2026-06-30");

        verify(entregasEstudiantes).eliminarNotasAutomaticas(20L);
    }

    @Test
    @DisplayName("7. Dejar la fecha en blanco quita el plazo")
    void quitaElPlazo() {
        servicio.fijarFechaEntrega(PROFESOR, false, SECCION, 20L, "");

        verify(entregas).cambiarFechaLimite(20L, null);
    }

    @Test
    @DisplayName("8. Una entrega o pauta de otra sección da 404")
    void itemAjeno() {
        when(entregas.buscar(21L)).thenReturn(Optional.of(new EntregaFila(21L, 2L, "Otra", BigDecimal.TEN, true, null, 1)));

        assertThrows(ResponseStatusException.class,
                () -> servicio.fijarFechaEntrega(PROFESOR, false, SECCION, 21L, "2026-06-30"));
        assertThrows(ResponseStatusException.class,
                () -> servicio.fijarFechaPauta(PROFESOR, false, SECCION, 555L, "2026-06-30"));
    }

    @Test
    @DisplayName("9. La vista muestra solo lo habilitado")
    void vistaHabilitadas() {
        when(entregas.listar(SECCION)).thenReturn(List.of(
                new EntregaFila(20L, SECCION, "A", BigDecimal.TEN, true, null, 1),
                new EntregaFila(21L, SECCION, "B", BigDecimal.TEN, false, null, 2)));
        when(pautas.listar(SECCION)).thenReturn(List.of());

        assertEquals(1, servicio.vista(PROFESOR, false, SECCION).entregas().size());
    }
}
