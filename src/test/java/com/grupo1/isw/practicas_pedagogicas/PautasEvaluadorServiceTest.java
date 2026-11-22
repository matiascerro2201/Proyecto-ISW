package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository;
import com.grupo1.isw.practicas_pedagogicas.services.PautasEvaluadorService;
import com.grupo1.isw.practicas_pedagogicas.services.PautasEvaluadorService.EstudianteVista;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PautasEvaluadorServiceTest {
    private static final LocalDate HOY = LocalDate.of(2026, 5, 15);

    @Mock
    EvaluacionesEsperadasRepository repositorio;

    private PautasEvaluadorService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-05-15T16:00:00Z"), ZoneId.of("America/Santiago"));
        servicio = new PautasEvaluadorService(repositorio, reloj);
    }

    @Test
    @DisplayName("1. Marca cada pauta como aplicada, pendiente o atrasada según su fecha límite")
    void estados() {
        when(repositorio.delEvaluador(COLABORADOR, "COLABORADOR")).thenReturn(List.of(
                pauta(1, "COLABORADOR", "30", "20", "18", HOY.minusDays(5), false),
                pauta(2, "COLABORADOR", "30", "20", null, HOY.plusDays(2), false),
                pauta(3, "COLABORADOR", "40", "20", null, HOY.minusDays(1), false),
                pauta(4, "COLABORADOR", "40", "20", null, null, false)));

        List<EstudianteVista> estudiantes = servicio.listar(COLABORADOR, "COLABORADOR");

        assertEquals(1, estudiantes.size());
        var pautas = estudiantes.get(0).pautas();
        assertEquals(PautasEvaluadorService.APLICADA, pautas.get(0).estado());
        assertEquals(PautasEvaluadorService.PENDIENTE, pautas.get(1).estado());
        assertEquals(Long.valueOf(2), pautas.get(1).diasRestantes());
        assertEquals(PautasEvaluadorService.ATRASADA, pautas.get(2).estado());
        assertEquals(Long.valueOf(-1), pautas.get(2).diasRestantes());
        assertEquals(PautasEvaluadorService.PENDIENTE, pautas.get(3).estado());
        assertNull(pautas.get(3).diasRestantes());
        assertEquals(3, estudiantes.get(0).pendientes());
    }

    @Test
    @DisplayName("2. Una pauta atrasada se puede aplicar igual: el enlace sigue disponible")
    void atrasadaNoBloquea() {
        when(repositorio.delEvaluador(COLABORADOR, "COLABORADOR")).thenReturn(List.of(
                pauta(3, "COLABORADOR", "40", "20", null, HOY.minusDays(30), false)));

        var vista = servicio.listar(COLABORADOR, "COLABORADOR").get(0).pautas().get(0);

        assertEquals(PautasEvaluadorService.ATRASADA, vista.estado());
        assertEquals("/colaborador/lineas/100/pautas/3", vista.enlace());
    }

    @Test
    @DisplayName("3. El tutor ve sus pautas y enlaza a su propia pantalla de evaluación")
    void tutor() {
        when(repositorio.delEvaluador(TUTOR, "TUTOR")).thenReturn(List.of(
                pauta(7, "TUTOR", "20", "20", null, HOY.plusDays(10), false)));

        var vista = servicio.listar(TUTOR, "TUTOR").get(0).pautas().get(0);

        assertEquals("/tutor/lineas/100/pautas/7", vista.enlace());
        assertFalse(vista.aplicada());
    }

    @Test
    @DisplayName("4. Cualquier otro valor de evaluador se trata como colaborador")
    void evaluadorDesconocido() {
        when(repositorio.delEvaluador(COLABORADOR, "COLABORADOR")).thenReturn(List.of());

        assertTrue(servicio.listar(COLABORADOR, "OTRO").isEmpty());
    }

    @Test
    @DisplayName("5. Agrupa las pautas por estudiante y línea")
    void agrupa() {
        when(repositorio.delEvaluador(COLABORADOR, "COLABORADOR")).thenReturn(List.of(
                pauta(1, "COLABORADOR", "30", "20", "10", null, false),
                pauta(2, "COLABORADOR", "30", "20", null, null, false)));

        var estudiantes = servicio.listar(COLABORADOR, "COLABORADOR");

        assertEquals(1, estudiantes.size());
        assertEquals(2, estudiantes.get(0).pautas().size());
        assertEquals("Camila Soto", estudiantes.get(0).estudiante());
    }
}
