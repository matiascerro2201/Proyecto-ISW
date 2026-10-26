package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;
import com.grupo1.isw.practicas_pedagogicas.models.Criterio;
import com.grupo1.isw.practicas_pedagogicas.models.Dimension;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.services.GestionPautaService;
import com.grupo1.isw.practicas_pedagogicas.services.GestionPautaService.PautaDetalle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionPautaServiceTest {

    private static final Long PROFESOR = 4L;
    private static final Long OTRO_PROFESOR = 99L;
    private static final Long ID_ASIGNATURA = 10L;
    private static final Long ID_PAUTA = 100L;

    @Mock
    private PautaEvaluacionRepository pautaRepository;

    @Mock
    private SeccionAsignaturaRepository seccionRepository;

    @InjectMocks
    private GestionPautaService service;

    private Asignatura asignatura;

    @BeforeEach
    void preparar() {
        asignatura = new Asignatura("Práctica Pedagógica", Ciclo.FINAL, 2);
        asignatura.setIdAsignatura(ID_ASIGNATURA);
    }

    // ---------- helpers ----------

    private PautaEvaluacion pauta(Long id, TipoLinea tipo, int numero) {
        PautaEvaluacion pauta = new PautaEvaluacion(asignatura, tipo, numero, new BigDecimal("30.00"));
        pauta.setIdPautaEvaluacion(id);
        asignatura.getPautasEvaluacion().add(pauta);
        return pauta;
    }

    private Dimension dimension(PautaEvaluacion pauta, Long id, String nombre, int orden) {
        Dimension dimension = new Dimension(pauta, nombre, orden);
        dimension.setIdDimension(id);
        pauta.getDimensiones().add(dimension);
        return dimension;
    }

    private Criterio criterio(Dimension dimension, Long id, String texto, String puntaje, int orden) {
        Criterio criterio = new Criterio(dimension, texto, new BigDecimal(puntaje), orden);
        criterio.setIdCriterio(id);
        dimension.getCriterios().add(criterio);
        return criterio;
    }

    /** La pauta 100 existe y el profesor imparte su asignatura. */
    private PautaEvaluacion dadaUnaPautaDelProfesor() {
        PautaEvaluacion pauta = pauta(ID_PAUTA, TipoLinea.UNICA, 1);
        when(pautaRepository.findById(ID_PAUTA)).thenReturn(Optional.of(pauta));
        when(seccionRepository.profesorImparte(PROFESOR, ID_ASIGNATURA)).thenReturn(true);
        return pauta;
    }

    private void dadoQueElProfesorImparteLaAsignatura() {
        when(seccionRepository.findAsignaturasDelProfesor(PROFESOR)).thenReturn(List.of(asignatura));
    }

    // ---------- consulta ----------

    @Test
    @DisplayName("1. Lista las asignaturas del profesor con sus pautas y si están en uso")
    void listar_asignaturasConPautas() {
        dadoQueElProfesorImparteLaAsignatura();
        PautaEvaluacion libre = pauta(1L, TipoLinea.UNICA, 1);
        Dimension d = dimension(libre, 1L, "Planificación", 1);
        criterio(d, 1L, "Planifica con claridad", "5", 1);
        criterio(d, 2L, "Usa recursos", "5", 2);
        PautaEvaluacion usada = pauta(2L, TipoLinea.UNICA, 2);
        usada.getEvaluacionesAplicadas().add(new EvaluacionAplicada());
        when(pautaRepository.findByAsignatura(ID_ASIGNATURA)).thenReturn(List.of(libre, usada));

        var resultado = service.listarAsignaturas(PROFESOR);

        assertEquals(1, resultado.size());
        assertEquals("Práctica Pedagógica", resultado.get(0).nombre());
        var pautas = resultado.get(0).pautas();
        assertEquals(2, pautas.get(0).cantidadCriterios());
        assertFalse(pautas.get(0).enUso());
        assertTrue(pautas.get(1).enUso());
    }

    @Test
    @DisplayName("2. El detalle ordena dimensiones y criterios, y suma el puntaje total")
    void obtenerPauta_ordenaYSuma() {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();
        Dimension segunda = dimension(pauta, 2L, "Reflexión", 2);
        Dimension primera = dimension(pauta, 1L, "Planificación", 1);
        criterio(primera, 12L, "Segundo criterio", "3.5", 2);
        criterio(primera, 11L, "Primer criterio", "4", 1);
        criterio(segunda, 21L, "Reflexiona", "2.5", 1);

        PautaDetalle detalle = service.obtenerPauta(PROFESOR, ID_PAUTA);

        assertEquals("Planificación", detalle.dimensiones().get(0).nombre());
        assertEquals("Reflexión", detalle.dimensiones().get(1).nombre());
        assertEquals("Primer criterio", detalle.dimensiones().get(0).criterios().get(0).descripcion());
        assertEquals(0, new BigDecimal("10.00").compareTo(detalle.puntajeTotal()));
        assertTrue(detalle.completa());
        assertTrue(detalle.editable());
    }

    @Test
    @DisplayName("3. Una pauta sin dimensiones, o con una dimensión vacía, está incompleta")
    void obtenerPauta_incompleta() {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();

        assertFalse(service.obtenerPauta(PROFESOR, ID_PAUTA).completa());

        dimension(pauta, 1L, "Planificación", 1);
        assertFalse(service.obtenerPauta(PROFESOR, ID_PAUTA).completa());
    }

    @Test
    @DisplayName("4. Lanza 404 si la pauta no existe")
    void obtenerPauta_noExiste() {
        when(pautaRepository.findById(ID_PAUTA)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerPauta(PROFESOR, ID_PAUTA));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("5. Lanza 403 si la pauta es de una asignatura que el profesor no imparte")
    void obtenerPauta_deOtroProfesor() {
        PautaEvaluacion pauta = pauta(ID_PAUTA, TipoLinea.UNICA, 1);
        when(pautaRepository.findById(ID_PAUTA)).thenReturn(Optional.of(pauta));
        when(seccionRepository.profesorImparte(OTRO_PROFESOR, ID_ASIGNATURA)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerPauta(OTRO_PROFESOR, ID_PAUTA));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("6. Lanza 403 al consultar una asignatura que el profesor no imparte")
    void obtenerAsignatura_ajena() {
        when(seccionRepository.findAsignaturasDelProfesor(OTRO_PROFESOR)).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerAsignatura(OTRO_PROFESOR, ID_ASIGNATURA));

        assertEquals(403, ex.getStatusCode().value());
    }

    // ---------- crear pauta ----------

    @Test
    @DisplayName("7. Crea la pauta con los datos ingresados y devuelve su id")
    void crearPauta_ok() {
        dadoQueElProfesorImparteLaAsignatura();
        when(pautaRepository.findByAsignatura(ID_ASIGNATURA)).thenReturn(List.of());
        when(pautaRepository.save(any(PautaEvaluacion.class))).thenAnswer(inv -> {
            PautaEvaluacion p = inv.getArgument(0);
            p.setIdPautaEvaluacion(ID_PAUTA);
            return p;
        });

        Long id = service.crearPauta(PROFESOR, ID_ASIGNATURA, "ESPECIALIDAD", "2", "25,5");

        assertEquals(ID_PAUTA, id);
        ArgumentCaptor<PautaEvaluacion> captor = ArgumentCaptor.forClass(PautaEvaluacion.class);
        verify(pautaRepository).save(captor.capture());
        PautaEvaluacion guardada = captor.getValue();
        assertEquals(TipoLinea.ESPECIALIDAD, guardada.getTipoLinea());
        assertEquals(2, guardada.getNumeroAplicacion());
        assertEquals(new BigDecimal("25.50"), guardada.getPonderacion());
        assertEquals(asignatura, guardada.getAsignatura());
    }

    @Test
    @DisplayName("8. No crea pautas en asignaturas que el profesor no imparte")
    void crearPauta_asignaturaAjena() {
        when(seccionRepository.findAsignaturasDelProfesor(OTRO_PROFESOR)).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.crearPauta(OTRO_PROFESOR, ID_ASIGNATURA, "UNICA", "1", "30"));

        assertEquals(403, ex.getStatusCode().value());
        verify(pautaRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "abc", "unica", "TODAS"})
    @DisplayName("9. Rechaza un tipo de línea vacío o que no existe")
    void crearPauta_tipoLineaInvalido(String tipo) {
        dadoQueElProfesorImparteLaAsignatura();

        assertThrows(IllegalArgumentException.class,
                () -> service.crearPauta(PROFESOR, ID_ASIGNATURA, tipo, "1", "30"));

        verify(pautaRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"0", "11", "-1", "abc", "1,5", "1.5"})
    @DisplayName("10. Rechaza un número de aplicación vacío, no entero o fuera de 1 a 10")
    void crearPauta_numeroAplicacionInvalido(String numero) {
        dadoQueElProfesorImparteLaAsignatura();

        assertThrows(IllegalArgumentException.class,
                () -> service.crearPauta(PROFESOR, ID_ASIGNATURA, "UNICA", numero, "30"));

        verify(pautaRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"0", "-5", "100,01", "101", "abc", "12,345", "NaN", "12345678901"})
    @DisplayName("11. Rechaza una ponderación vacía, no numérica, fuera de 0-100 o con más de dos decimales")
    void crearPauta_ponderacionInvalida(String ponderacion) {
        dadoQueElProfesorImparteLaAsignatura();

        assertThrows(IllegalArgumentException.class,
                () -> service.crearPauta(PROFESOR, ID_ASIGNATURA, "UNICA", "1", ponderacion));

        verify(pautaRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"100", "100,00", "0,01", " 30 "})
    @DisplayName("12. Acepta ponderaciones válidas en los límites")
    void crearPauta_ponderacionValida(String ponderacion) {
        dadoQueElProfesorImparteLaAsignatura();
        when(pautaRepository.findByAsignatura(ID_ASIGNATURA)).thenReturn(List.of());
        when(pautaRepository.save(any(PautaEvaluacion.class))).thenAnswer(inv -> inv.getArgument(0));

        service.crearPauta(PROFESOR, ID_ASIGNATURA, "UNICA", "1", ponderacion);

        verify(pautaRepository).save(any(PautaEvaluacion.class));
    }

    @Test
    @DisplayName("13. No permite repetir tipo de línea y aplicación en la misma asignatura")
    void crearPauta_duplicada() {
        dadoQueElProfesorImparteLaAsignatura();
        PautaEvaluacion existente = pauta(1L, TipoLinea.UNICA, 1);
        when(pautaRepository.findByAsignatura(ID_ASIGNATURA)).thenReturn(List.of(existente));

        assertThrows(IllegalArgumentException.class,
                () -> service.crearPauta(PROFESOR, ID_ASIGNATURA, "UNICA", "1", "30"));

        verify(pautaRepository, never()).save(any());
    }

    // ---------- actualizar y eliminar pauta ----------

    @Test
    @DisplayName("14. Actualiza los datos de la pauta")
    void actualizarDatos_ok() {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();
        when(pautaRepository.findByAsignatura(ID_ASIGNATURA)).thenReturn(List.of(pauta));

        service.actualizarDatos(PROFESOR, ID_PAUTA, "ORIENTACION", "3", "40");

        assertEquals(TipoLinea.ORIENTACION, pauta.getTipoLinea());
        assertEquals(3, pauta.getNumeroAplicacion());
        assertEquals(new BigDecimal("40.00"), pauta.getPonderacion());
        verify(pautaRepository).save(pauta);
    }

    @Test
    @DisplayName("15. Al actualizar, la pauta no choca consigo misma pero sí con otra existente")
    void actualizarDatos_duplicados() {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();
        PautaEvaluacion otra = pauta(2L, TipoLinea.UNICA, 2);
        when(pautaRepository.findByAsignatura(ID_ASIGNATURA)).thenReturn(List.of(pauta, otra));

        // mismos datos que ella misma: permitido
        service.actualizarDatos(PROFESOR, ID_PAUTA, "UNICA", "1", "50");
        verify(pautaRepository).save(pauta);

        // datos de la otra pauta: rechazado
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarDatos(PROFESOR, ID_PAUTA, "UNICA", "2", "50"));
    }

    @Test
    @DisplayName("16. Elimina la pauta y la quita de la colección de la asignatura")
    void eliminarPauta_ok() {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();

        service.eliminarPauta(PROFESOR, ID_PAUTA);

        assertFalse(asignatura.getPautasEvaluacion().contains(pauta));
        verify(pautaRepository).delete(pauta);
    }

    // ---------- dimensiones ----------

    @Test
    @DisplayName("17. Agrega dimensiones con orden correlativo y sin espacios sobrantes")
    void agregarDimension_ok() {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();

        service.agregarDimension(PROFESOR, ID_PAUTA, "  Planificación  ");
        service.agregarDimension(PROFESOR, ID_PAUTA, "Reflexión");

        assertEquals(2, pauta.getDimensiones().size());
        assertEquals("Planificación", pauta.getDimensiones().get(0).getNombre());
        assertEquals(1, pauta.getDimensiones().get(0).getOrden());
        assertEquals(2, pauta.getDimensiones().get(1).getOrden());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("18. Rechaza una dimensión sin nombre")
    void agregarDimension_vacia(String nombre) {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();

        assertThrows(IllegalArgumentException.class,
                () -> service.agregarDimension(PROFESOR, ID_PAUTA, nombre));

        assertTrue(pauta.getDimensiones().isEmpty());
    }

    @Test
    @DisplayName("19. Rechaza un nombre de dimensión de más de 200 caracteres")
    void agregarDimension_muyLarga() {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();

        assertThrows(IllegalArgumentException.class,
                () -> service.agregarDimension(PROFESOR, ID_PAUTA, "x".repeat(201)));

        assertTrue(pauta.getDimensiones().isEmpty());
    }

    @Test
    @DisplayName("20. Elimina una dimensión con sus criterios")
    void eliminarDimension_ok() {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();
        Dimension d = dimension(pauta, 1L, "Planificación", 1);
        criterio(d, 1L, "Criterio", "5", 1);

        service.eliminarDimension(PROFESOR, ID_PAUTA, 1L);

        assertTrue(pauta.getDimensiones().isEmpty());
        verify(pautaRepository).save(pauta);
    }

    @Test
    @DisplayName("21. Eliminar una dimensión que no es de la pauta lanza 404")
    void eliminarDimension_noExiste() {
        dadaUnaPautaDelProfesor();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.eliminarDimension(PROFESOR, ID_PAUTA, 77L));

        assertEquals(404, ex.getStatusCode().value());
    }

    // ---------- criterios ----------

    @Test
    @DisplayName("22. Agrega criterios con orden correlativo dentro de su dimensión")
    void agregarCriterio_ok() {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();
        Dimension d = dimension(pauta, 1L, "Planificación", 1);

        service.agregarCriterio(PROFESOR, ID_PAUTA, 1L, "  Planifica con claridad ", "5,5");
        service.agregarCriterio(PROFESOR, ID_PAUTA, 1L, "Usa recursos", "4");

        assertEquals(2, d.getCriterios().size());
        assertEquals("Planifica con claridad", d.getCriterios().get(0).getDescripcion());
        assertEquals(new BigDecimal("5.50"), d.getCriterios().get(0).getPuntajeMax());
        assertEquals(1, d.getCriterios().get(0).getOrden());
        assertEquals(2, d.getCriterios().get(1).getOrden());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"0", "-1", "1000", "abc", "5,555", "NaN"})
    @DisplayName("23. Rechaza un puntaje máximo vacío, no numérico, no positivo o con más de dos decimales")
    void agregarCriterio_puntajeInvalido(String puntaje) {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();
        Dimension d = dimension(pauta, 1L, "Planificación", 1);

        assertThrows(IllegalArgumentException.class,
                () -> service.agregarCriterio(PROFESOR, ID_PAUTA, 1L, "Criterio", puntaje));

        assertTrue(d.getCriterios().isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("24. Rechaza un criterio sin descripción")
    void agregarCriterio_sinDescripcion(String descripcion) {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();
        Dimension d = dimension(pauta, 1L, "Planificación", 1);

        assertThrows(IllegalArgumentException.class,
                () -> service.agregarCriterio(PROFESOR, ID_PAUTA, 1L, descripcion, "5"));

        assertTrue(d.getCriterios().isEmpty());
    }

    @Test
    @DisplayName("25. Agregar un criterio a una dimensión que no es de la pauta lanza 404")
    void agregarCriterio_dimensionAjena() {
        dadaUnaPautaDelProfesor();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.agregarCriterio(PROFESOR, ID_PAUTA, 77L, "Criterio", "5"));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("26. Elimina un criterio de cualquier dimensión de la pauta")
    void eliminarCriterio_ok() {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();
        Dimension d = dimension(pauta, 1L, "Planificación", 1);
        criterio(d, 1L, "Primero", "5", 1);
        criterio(d, 2L, "Segundo", "5", 2);

        service.eliminarCriterio(PROFESOR, ID_PAUTA, 1L);

        assertEquals(1, d.getCriterios().size());
        assertEquals("Segundo", d.getCriterios().get(0).getDescripcion());
        verify(pautaRepository).save(pauta);
    }

    @Test
    @DisplayName("27. Eliminar un criterio que no es de la pauta lanza 404")
    void eliminarCriterio_noExiste() {
        dadaUnaPautaDelProfesor();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.eliminarCriterio(PROFESOR, ID_PAUTA, 77L));

        assertEquals(404, ex.getStatusCode().value());
    }

    // ---------- pauta en uso y permisos ----------

    @Test
    @DisplayName("28. Una pauta con evaluaciones aplicadas no se puede modificar ni eliminar")
    void pautaEnUso_soloLectura() {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();
        Dimension d = dimension(pauta, 1L, "Planificación", 1);
        criterio(d, 1L, "Criterio", "5", 1);
        pauta.getEvaluacionesAplicadas().add(new EvaluacionAplicada());

        assertThrows(IllegalStateException.class,
                () -> service.actualizarDatos(PROFESOR, ID_PAUTA, "UNICA", "1", "30"));
        assertThrows(IllegalStateException.class, () -> service.eliminarPauta(PROFESOR, ID_PAUTA));
        assertThrows(IllegalStateException.class, () -> service.agregarDimension(PROFESOR, ID_PAUTA, "Nueva"));
        assertThrows(IllegalStateException.class, () -> service.eliminarDimension(PROFESOR, ID_PAUTA, 1L));
        assertThrows(IllegalStateException.class,
                () -> service.agregarCriterio(PROFESOR, ID_PAUTA, 1L, "Nuevo", "5"));
        assertThrows(IllegalStateException.class, () -> service.eliminarCriterio(PROFESOR, ID_PAUTA, 1L));

        verify(pautaRepository, never()).save(any());
        verify(pautaRepository, never()).delete(any());
        assertEquals(1, d.getCriterios().size());
        assertTrue(service.obtenerPauta(PROFESOR, ID_PAUTA).enUso(), "se puede consultar aunque esté en uso");
    }

    @Test
    @DisplayName("29. Otro profesor no puede modificar la pauta (403) y no se guarda nada")
    void modificar_deOtroProfesor() {
        PautaEvaluacion pauta = pauta(ID_PAUTA, TipoLinea.UNICA, 1);
        when(pautaRepository.findById(ID_PAUTA)).thenReturn(Optional.of(pauta));
        when(seccionRepository.profesorImparte(OTRO_PROFESOR, ID_ASIGNATURA)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.agregarDimension(OTRO_PROFESOR, ID_PAUTA, "Nueva"));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(pauta.getDimensiones().isEmpty());
        verify(pautaRepository, never()).save(any());
    }
}
