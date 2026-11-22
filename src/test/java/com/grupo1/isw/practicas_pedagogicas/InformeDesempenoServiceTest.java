package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository.EvaluacionEsperada;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeDesempenoRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeDesempenoRepository.DatosInscripcion;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeDesempenoRepository.EntregaNota;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeDesempenoRepository.InformeFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository;
import com.grupo1.isw.practicas_pedagogicas.services.CalculadoraNota;
import com.grupo1.isw.practicas_pedagogicas.services.EstructuraInvalidaException;
import com.grupo1.isw.practicas_pedagogicas.services.InformeDesempenoService;
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

import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.ESTUDIANTE;
import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.INSCRIPCION;
import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.SECCION;
import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.bd;
import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.pauta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InformeDesempenoServiceTest {
    private static final long PROFESOR = 5L;
    private static final LocalDate HOY = LocalDate.of(2026, 5, 15);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 5, 15, 12, 0);

    @Mock
    SeccionEstructuraRepository secciones;
    @Mock
    InformeDesempenoRepository repositorio;
    @Mock
    EvaluacionesEsperadasRepository evaluaciones;
    @Mock
    NotaAutomaticaService notaAutomatica;
    @Mock
    NotificacionService notificaciones;

    private InformeDesempenoService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-05-15T16:00:00Z"), ZoneId.of("America/Santiago"));
        servicio = new InformeDesempenoService(secciones, repositorio, evaluaciones, new CalculadoraNota(bd("60")),
                notaAutomatica, notificaciones, reloj);
        usarInscripcion(true);
        completo();
        when(repositorio.informe(INSCRIPCION)).thenReturn(Optional.empty());
    }

    private void usarInscripcion(boolean iniciada) {
        when(repositorio.inscripcion(INSCRIPCION)).thenReturn(Optional.of(new DatosInscripcion(INSCRIPCION,
                ESTUDIANTE, "Camila Soto", "camila@ubb.cl", SECCION, "Práctica Pedagógica", 2026, 1, PROFESOR,
                "EN_CURSO", iniciada)));
    }

    private static EntregaNota entrega(long id, String ponderacion, String nota, boolean archivo, boolean automatica,
                                       LocalDate limite) {
        return new EntregaNota(id, "Entrega " + id, bd(ponderacion), limite, nota != null || archivo ? 9L : null,
                archivo, nota == null ? null : bd(nota), automatica, false);
    }

    /** Dos entregas calificadas (30 % con 6,0 y 20 % con 5,0) y dos pautas aplicadas (30 % con 6,25 y 20 % con 5,125). */
    private void completo() {
        when(repositorio.entregas(INSCRIPCION)).thenReturn(List.of(
                entrega(1, "30", "6.0", true, false, HOY.minusDays(10)),
                entrega(2, "20", "5.0", true, false, HOY.minusDays(5))));
        when(evaluaciones.deLaInscripcion(INSCRIPCION)).thenReturn(List.of(
                pauta(1, "COLABORADOR", "30", "20", "18", null, false),
                pauta(2, "TUTOR", "20", "20", "15", null, false)));
    }

    @Test
    @DisplayName("1. La nota sugerida es el promedio ponderado de entregas, pauta del colaborador y pauta del tutor")
    void calculaLaSugerida() {
        var detalle = servicio.detalle(PROFESOR, INSCRIPCION);

        // (6,0x30 + 5,0x20 + 6,25x30 + 5,125x20) / 100 = 5,70
        assertEquals(0, bd("5.7").compareTo(detalle.sugerida()));
        assertTrue(detalle.completo());
        assertTrue(detalle.ponderacionesSuman100());
        assertEquals(4, detalle.componentes().size());
        verify(notaAutomatica).aplicar(SECCION);
    }

    @Test
    @DisplayName("2. Generar guarda la nota sugerida y la deja a nombre del profesor")
    void generaElInforme() {
        BigDecimal sugerida = servicio.generar(PROFESOR, INSCRIPCION);

        assertEquals(0, bd("5.7").compareTo(sugerida));
        verify(repositorio).guardarSugerida(INSCRIPCION, PROFESOR, bd("5.7"), AHORA);
    }

    @Test
    @DisplayName("3. Si falta la pauta del tutor no genera y dice qué falta")
    void faltaLaPautaDelTutor() {
        when(evaluaciones.deLaInscripcion(INSCRIPCION)).thenReturn(List.of(
                pauta(1, "COLABORADOR", "30", "20", "18", null, false),
                pauta(2, "TUTOR", "20", "20", null, null, false)));

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> servicio.generar(PROFESOR, INSCRIPCION));

        assertEquals(1, ex.getErrores().size());
        assertTrue(ex.getErrores().get(0).contains("Pauta 2"));
        verify(repositorio, never()).guardarSugerida(anyLong(), anyLong(),
                org.mockito.ArgumentMatchers.<BigDecimal>any(), org.mockito.ArgumentMatchers.<LocalDateTime>any());
    }

    @Test
    @DisplayName("4. Una entrega sin calificar o con el plazo abierto también impide generar")
    void entregasPendientes() {
        when(repositorio.entregas(INSCRIPCION)).thenReturn(List.of(
                entrega(1, "30", null, true, false, HOY.minusDays(10)),
                entrega(2, "20", null, false, false, HOY.plusDays(5))));

        var detalle = servicio.detalle(PROFESOR, INSCRIPCION);

        assertEquals(2, detalle.pendientes().size());
        assertNull(detalle.sugerida());
        assertFalse(detalle.puedeGenerar());
    }

    @Test
    @DisplayName("5. El 1,0 automático de quien no entregó cuenta en el promedio")
    void notaAutomaticaCuenta() {
        when(repositorio.entregas(INSCRIPCION)).thenReturn(List.of(
                entrega(1, "50", "1.0", false, true, HOY.minusDays(1)),
                entrega(2, "50", "7.0", true, false, HOY.minusDays(1))));
        when(evaluaciones.deLaInscripcion(INSCRIPCION)).thenReturn(List.of());

        var detalle = servicio.detalle(PROFESOR, INSCRIPCION);

        assertEquals(0, bd("4.0").compareTo(detalle.sugerida()));
    }

    @Test
    @DisplayName("6. Si las ponderaciones no suman 100 la nota se normaliza y se avisa")
    void ponderacionesNoSuman100() {
        when(repositorio.entregas(INSCRIPCION)).thenReturn(List.of(
                entrega(1, "30", "6.0", true, false, null),
                entrega(2, "30", "4.0", true, false, null)));
        when(evaluaciones.deLaInscripcion(INSCRIPCION)).thenReturn(List.of());

        var detalle = servicio.detalle(PROFESOR, INSCRIPCION);

        assertFalse(detalle.ponderacionesSuman100());
        assertEquals(0, bd("5.0").compareTo(detalle.sugerida()));
    }

    @Test
    @DisplayName("7. Sin entregas ni pautas no hay informe que generar")
    void semestreVacio() {
        when(repositorio.entregas(INSCRIPCION)).thenReturn(List.of());
        when(evaluaciones.deLaInscripcion(INSCRIPCION)).thenReturn(List.of());

        assertThrows(EstructuraInvalidaException.class, () -> servicio.generar(PROFESOR, INSCRIPCION));
    }

    @Test
    @DisplayName("8. Un informe con la nota final fijada ya no se vuelve a generar")
    void informeCerrado() {
        when(repositorio.informe(INSCRIPCION)).thenReturn(Optional.of(new InformeFila(1L, INSCRIPCION, bd("5.7"),
                bd("6.0"), AHORA, AHORA, null)));

        assertThrows(IllegalStateException.class, () -> servicio.generar(PROFESOR, INSCRIPCION));
    }

    @Test
    @DisplayName("9. Se detecta que el informe quedó desactualizado si cambió una nota")
    void desactualizado() {
        when(repositorio.informe(INSCRIPCION)).thenReturn(Optional.of(new InformeFila(1L, INSCRIPCION, bd("5.0"),
                null, AHORA, null, null)));

        assertTrue(servicio.detalle(PROFESOR, INSCRIPCION).desactualizado());
    }

    @Test
    @DisplayName("10. La nota final se valida de 1,0 a 7,0 y requiere el informe generado")
    void notaFinal() {
        when(repositorio.informe(INSCRIPCION)).thenReturn(Optional.of(new InformeFila(1L, INSCRIPCION, bd("5.7"),
                null, AHORA, null, null)));

        servicio.fijarNotaFinal(PROFESOR, INSCRIPCION, "6,0");
        verify(repositorio).guardarFinal(INSCRIPCION, bd("6.0"), AHORA);

        assertThrows(EstructuraInvalidaException.class, () -> servicio.fijarNotaFinal(PROFESOR, INSCRIPCION, "7.5"));

        when(repositorio.informe(INSCRIPCION)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> servicio.fijarNotaFinal(PROFESOR, INSCRIPCION, "6.0"));
    }

    @Test
    @DisplayName("11. Publicar exige la nota final, avisa al estudiante y no se repite")
    void publicar() {
        when(repositorio.informe(INSCRIPCION)).thenReturn(Optional.of(new InformeFila(1L, INSCRIPCION, bd("5.7"),
                bd("6.0"), AHORA, AHORA, null)));

        servicio.publicar(PROFESOR, INSCRIPCION);

        verify(repositorio).publicar(INSCRIPCION, AHORA);
        verify(notificaciones).crear(ESTUDIANTE, NotificacionService.TIPO_INFORME_FINAL,
                "Tu informe final de desempeño de Práctica Pedagógica (semestre 1 de 2026) ya está disponible.",
                "/estudiante/evaluaciones");

        when(repositorio.informe(INSCRIPCION)).thenReturn(Optional.of(new InformeFila(1L, INSCRIPCION, bd("5.7"),
                null, AHORA, null, null)));
        assertThrows(IllegalStateException.class, () -> servicio.publicar(PROFESOR, INSCRIPCION));

        when(repositorio.informe(INSCRIPCION)).thenReturn(Optional.of(new InformeFila(1L, INSCRIPCION, bd("5.7"),
                bd("6.0"), AHORA, AHORA, AHORA)));
        assertThrows(IllegalStateException.class, () -> servicio.publicar(PROFESOR, INSCRIPCION));
    }

    @Test
    @DisplayName("12. Otro profesor no puede ver (403), un semestre sin iniciar se rechaza y una inscripción ajena da 404")
    void controlDeAcceso() {
        assertThrows(ResponseStatusException.class, () -> servicio.detalle(999L, INSCRIPCION));
        assertThrows(ResponseStatusException.class, () -> servicio.publicar(999L, INSCRIPCION));
        assertThrows(ResponseStatusException.class, () -> servicio.detalle(PROFESOR, 12345L));

        usarInscripcion(false);
        assertThrows(IllegalStateException.class, () -> servicio.detalle(PROFESOR, INSCRIPCION));
        verify(notificaciones, never()).crear(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("13. La lista de la sección indica cuánto le falta a cada estudiante")
    void listaDeEstudiantes() {
        when(secciones.buscar(SECCION)).thenReturn(Optional.of(
                new com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository.SeccionDatos(
                        SECCION, 10L, "Práctica", 2, 2026, 1, PROFESOR, "Profesor", true, null)));
        usarInscripcion(true);
        when(repositorio.inscripcionesDeLaSeccion(SECCION)).thenReturn(List.of(
                repositorio.inscripcion(INSCRIPCION).get()));
        when(repositorio.informesDeLaSeccion(SECCION)).thenReturn(List.of());
        List<EvaluacionEsperada> pautas = List.of(pauta(1, "COLABORADOR", "30", "20", "18", null, false),
                pauta(2, "TUTOR", "20", "20", null, null, false));
        when(evaluaciones.deLaSeccion(SECCION)).thenReturn(pautas);

        var filas = servicio.estudiantes(PROFESOR, SECCION);

        assertEquals(1, filas.size());
        assertEquals(1, filas.get(0).pendientes());
        assertEquals("PENDIENTE", filas.get(0).estado());
    }
}
