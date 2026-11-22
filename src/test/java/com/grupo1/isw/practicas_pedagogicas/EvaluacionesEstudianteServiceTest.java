package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository.EntregaDelEstudiante;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository.CriterioEvaluado;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository.EvaluacionEsperada;
import com.grupo1.isw.practicas_pedagogicas.repositories.NotaFinalRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.NotaFinalRepository.NotaFinal;
import com.grupo1.isw.practicas_pedagogicas.services.CalculadoraNota;
import com.grupo1.isw.practicas_pedagogicas.services.EntregaEstudianteService;
import com.grupo1.isw.practicas_pedagogicas.services.EntregaEstudianteService.EntregaVista;
import com.grupo1.isw.practicas_pedagogicas.services.EntregaEstudianteService.GrupoEntregas;
import com.grupo1.isw.practicas_pedagogicas.services.EvaluacionesEstudianteService;
import com.grupo1.isw.practicas_pedagogicas.services.EvaluacionesEstudianteService.FilaEvaluacion;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.ESTUDIANTE;
import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.INSCRIPCION;
import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.bd;
import static com.grupo1.isw.practicas_pedagogicas.EscenarioDesempeno.pauta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EvaluacionesEstudianteServiceTest {
    private static final LocalDate LIMITE = LocalDate.of(2026, 6, 1);

    @Mock
    EntregaEstudianteService entregas;
    @Mock
    EvaluacionesEsperadasRepository pautas;
    @Mock
    NotaFinalRepository notasFinales;

    private EvaluacionesEstudianteService servicio;

    @BeforeEach
    void preparar() {
        servicio = new EvaluacionesEstudianteService(entregas, pautas, notasFinales, new CalculadoraNota(bd("60")));
        when(entregas.listar(ESTUDIANTE)).thenReturn(List.of());
        when(pautas.delEstudiante(ESTUDIANTE)).thenReturn(List.of());
        when(notasFinales.publicadas(ESTUDIANTE)).thenReturn(Map.of());
    }

    private static EntregaVista entrega(long id, String estado, boolean archivo, String nota, boolean publicada,
                                        boolean puedeSubir) {
        var datos = new EntregaDelEstudiante(INSCRIPCION, "EN_CURSO", 1L, "Práctica Pedagógica", 2026, 1, id,
                "Entrega " + id, bd("25"), LIMITE, archivo || nota != null ? 90L + id : null, archivo,
                archivo ? LocalDateTime.of(2026, 5, 20, 10, 0) : null, nota == null ? null : bd(nota),
                nota == null ? null : "Buen trabajo", false, publicada);
        return new EntregaVista(datos, estado, puedeSubir, "NO_ENTREGADA".equals(estado));
    }

    private void entregas(EntregaVista... vistas) {
        when(entregas.listar(ESTUDIANTE)).thenReturn(List.of(
                new GrupoEntregas(INSCRIPCION, "Práctica Pedagógica", "Semestre 1 de 2026", List.of(vistas))));
    }

    @Test
    @DisplayName("1. Una sola tabla por asignatura con las entregas y las pautas de colaborador y tutor")
    void unaTablaConTodo() {
        entregas(entrega(1, "ENTREGADA", true, null, false, true));
        when(pautas.delEstudiante(ESTUDIANTE)).thenReturn(List.of(
                pauta(1, "COLABORADOR", "30", "20", "18", null, false),
                pauta(2, "TUTOR", "20", "20", null, null, false)));

        var grupos = servicio.tabla(ESTUDIANTE);

        assertEquals(1, grupos.size());
        assertEquals("Práctica Pedagógica", grupos.get(0).asignatura());
        List<FilaEvaluacion> filas = grupos.get(0).filas();
        assertEquals(3, filas.size());
        assertEquals(EvaluacionesEstudianteService.ENTREGA, filas.get(0).tipo());
        assertEquals(EvaluacionesEstudianteService.PAUTA, filas.get(1).tipo());
        assertEquals(bd("30"), filas.get(1).ponderacion());
    }

    @Test
    @DisplayName("2. Las notas no se ven mientras el profesor no las publique")
    void notasOcultas() {
        entregas(entrega(1, "ENTREGADA", true, "6.0", false, false));
        when(pautas.delEstudiante(ESTUDIANTE)).thenReturn(List.of(
                pauta(1, "COLABORADOR", "30", "20", "18", null, false)));

        var filas = servicio.tabla(ESTUDIANTE).get(0).filas();

        assertNull(filas.get(0).nota());
        assertFalse(filas.get(0).conDetalle());
        assertNull(filas.get(1).nota());
        assertFalse(filas.get(1).conDetalle());
        assertNull(filas.get(1).idEvaluacion());
    }

    @Test
    @DisplayName("3. Publicadas, se ve la nota de cada entrega y de cada pauta, con enlace al detalle")
    void notasPublicadas() {
        entregas(entrega(1, "ENTREGADA", true, "6.0", true, false));
        when(pautas.delEstudiante(ESTUDIANTE)).thenReturn(List.of(
                pauta(1, "COLABORADOR", "30", "20", "18", null, true)));

        var filas = servicio.tabla(ESTUDIANTE).get(0).filas();

        assertEquals(0, bd("6.0").compareTo(filas.get(0).nota()));
        assertTrue(filas.get(0).conDetalle());
        assertEquals(0, bd("6.3").compareTo(filas.get(1).nota()));
        assertTrue(filas.get(1).conDetalle());
        assertEquals(Long.valueOf(501), filas.get(1).idEvaluacion());
    }

    @Test
    @DisplayName("4. Estados: entregada, pendiente y no entregada en las entregas; aplicada y pendiente en las pautas")
    void estados() {
        entregas(entrega(1, "ENTREGADA", true, null, false, true),
                entrega(2, "PENDIENTE", false, null, false, true),
                entrega(3, "NO_ENTREGADA", false, null, false, false));
        when(pautas.delEstudiante(ESTUDIANTE)).thenReturn(List.of(
                pauta(1, "COLABORADOR", "30", "20", "18", null, false),
                pauta(2, "TUTOR", "20", "20", null, null, false)));

        var filas = servicio.tabla(ESTUDIANTE).get(0).filas();

        assertEquals("ENTREGADA", filas.get(0).estado());
        assertEquals("PENDIENTE", filas.get(1).estado());
        assertEquals("NO_ENTREGADA", filas.get(2).estado());
        assertEquals("APLICADA", filas.get(3).estado());
        assertEquals("PENDIENTE", filas.get(4).estado());
    }

    @Test
    @DisplayName("5. Solo las entregas con plazo abierto se pueden entregar desde la tabla; las pautas nunca")
    void accionEntregar() {
        entregas(entrega(1, "ENTREGADA", true, null, false, true),
                entrega(2, "PENDIENTE", false, null, false, true),
                entrega(3, "NO_ENTREGADA", false, null, false, false));
        when(pautas.delEstudiante(ESTUDIANTE)).thenReturn(List.of(
                pauta(2, "TUTOR", "20", "20", null, null, false)));

        var filas = servicio.tabla(ESTUDIANTE).get(0).filas();

        assertTrue(filas.get(0).puedeEntregar());
        assertTrue(filas.get(0).tieneArchivo());
        assertTrue(filas.get(1).puedeEntregar());
        assertFalse(filas.get(1).tieneArchivo());
        assertFalse(filas.get(2).puedeEntregar());
        assertFalse(filas.get(3).puedeEntregar());
    }

    @Test
    @DisplayName("6. La nota final aparece solo cuando el profesor publicó el informe de desempeño")
    void notaFinal() {
        entregas(entrega(1, "ENTREGADA", true, null, false, true));

        assertFalse(servicio.tabla(ESTUDIANTE).get(0).tieneNotaFinal());

        when(notasFinales.publicadas(ESTUDIANTE)).thenReturn(Map.of(INSCRIPCION,
                new NotaFinal(bd("5.8"), LocalDateTime.of(2026, 7, 1, 9, 0))));

        var grupo = servicio.tabla(ESTUDIANTE).get(0);
        assertTrue(grupo.tieneNotaFinal());
        assertEquals(0, bd("5.8").compareTo(grupo.notaFinal()));
    }

    @Test
    @DisplayName("7. Un semestre con solo pautas también aparece, y sin evaluaciones no hay tabla")
    void soloPautasYVacio() {
        assertTrue(servicio.tabla(ESTUDIANTE).isEmpty());

        when(pautas.delEstudiante(ESTUDIANTE)).thenReturn(List.of(
                pauta(1, "COLABORADOR", "100", "20", null, null, false)));

        var grupos = servicio.tabla(ESTUDIANTE);
        assertEquals(1, grupos.size());
        assertEquals("Semestre 1 de 2026", grupos.get(0).periodo());
    }

    @Test
    @DisplayName("8. N-22: el detalle de una entrega trae nota y comentario si está publicada")
    void detalleDeEntrega() {
        entregas(entrega(1, "ENTREGADA", true, "6.0", true, false),
                entrega(2, "ENTREGADA", true, "5.0", false, false));

        var detalle = servicio.detalleEntrega(ESTUDIANTE, INSCRIPCION, 1L);

        assertEquals(0, bd("6.0").compareTo(detalle.nota()));
        assertEquals("Buen trabajo", detalle.comentario());
        assertThrows(ResponseStatusException.class, () -> servicio.detalleEntrega(ESTUDIANTE, INSCRIPCION, 2L));
        assertThrows(ResponseStatusException.class, () -> servicio.detalleEntrega(ESTUDIANTE, 999L, 1L));
        assertThrows(ResponseStatusException.class, () -> servicio.detalleEntrega(ESTUDIANTE, INSCRIPCION, 77L));
    }

    @Test
    @DisplayName("9. N-22: el detalle de una pauta publicada trae los criterios agrupados por dimensión con sus totales")
    void detalleDePauta() {
        when(pautas.delEstudiante(ESTUDIANTE)).thenReturn(List.of(
                pauta(1, "COLABORADOR", "30", "20", "18", null, true)));
        when(pautas.criterios(501L)).thenReturn(List.of(
                new CriterioEvaluado("Planificación", "Objetivos claros", bd("5"), bd("4"), "Bien"),
                new CriterioEvaluado("Planificación", "Secuencia", bd("5"), bd("5"), null),
                new CriterioEvaluado("Clase", "Gestión del aula", bd("10"), bd("9"), "Muy bien")));

        var detalle = servicio.detallePauta(ESTUDIANTE, 501L);

        assertEquals(0, bd("6.3").compareTo(detalle.nota()));
        assertEquals(2, detalle.dimensiones().size());
        assertEquals("Planificación", detalle.dimensiones().get(0).nombre());
        assertEquals(0, bd("9").compareTo(detalle.dimensiones().get(0).obtenido()));
        assertEquals(0, bd("10").compareTo(detalle.dimensiones().get(0).maximo()));
        assertEquals(2, detalle.dimensiones().get(0).criterios().size());
        assertEquals("Carla Colaboradora", detalle.nombreEvaluador());
    }

    @Test
    @DisplayName("10. N-22: una pauta sin publicar, sin aplicar o de otro estudiante da 404")
    void detalleDePautaNoDisponible() {
        when(pautas.delEstudiante(ESTUDIANTE)).thenReturn(List.of(
                pauta(1, "COLABORADOR", "30", "20", "18", null, false),
                pauta(2, "TUTOR", "20", "20", null, null, true)));

        assertThrows(ResponseStatusException.class, () -> servicio.detallePauta(ESTUDIANTE, 501L));
        assertThrows(ResponseStatusException.class, () -> servicio.detallePauta(ESTUDIANTE, 502L));
        assertThrows(ResponseStatusException.class, () -> servicio.detallePauta(ESTUDIANTE, 99999L));
    }
}
