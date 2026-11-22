package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository;
import com.grupo1.isw.practicas_pedagogicas.services.NotaAutomaticaService;
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
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotaAutomaticaServiceTest {
    private static final LocalDate HOY = LocalDate.of(2026, 5, 15);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 5, 15, 12, 0);

    @Mock
    EntregaEstudianteRepository repositorio;

    private NotaAutomaticaService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-05-15T16:00:00Z"), ZoneId.of("America/Santiago"));
        servicio = new NotaAutomaticaService(repositorio, reloj);
    }

    @Test
    @DisplayName("1. Aplica las notas 1,0 a todos los semestres usando la fecha y hora de hoy")
    void aplicaATodos() {
        when(repositorio.crearNotasAutomaticas(HOY, AHORA, null)).thenReturn(4);

        assertEquals(4, servicio.aplicar(null));
    }

    @Test
    @DisplayName("2. También puede aplicarse a un solo semestre")
    void aplicaAUnaSeccion() {
        when(repositorio.crearNotasAutomaticas(HOY, AHORA, 5L)).thenReturn(1);

        assertEquals(1, servicio.aplicar(5L));
        verify(repositorio).crearNotasAutomaticas(HOY, AHORA, 5L);
    }

    @Test
    @DisplayName("3. La tarea programada no propaga errores de la base de datos")
    void tareaProgramadaNoFalla() {
        doThrow(new RuntimeException("base caída")).when(repositorio).crearNotasAutomaticas(HOY, AHORA, null);

        servicio.aplicarProgramado();
    }
}
