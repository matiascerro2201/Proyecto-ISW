package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.PautaFormulario;
import com.grupo1.isw.practicas_pedagogicas.dto.PautaFormulario.CriterioFormulario;
import com.grupo1.isw.practicas_pedagogicas.dto.PautaFormulario.DimensionFormulario;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;
import com.grupo1.isw.practicas_pedagogicas.models.Criterio;
import com.grupo1.isw.practicas_pedagogicas.models.Dimension;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionAplicadaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.services.PautaDefinicionService;
import com.grupo1.isw.practicas_pedagogicas.services.PautaInvalidaException;

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
import java.util.ArrayList;
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
class PautaDefinicionServiceTest {

    private static final Long PROFESOR = 4L;
    private static final Long OTRO_PROFESOR = 99L;
    private static final Long ID_ASIGNATURA = 10L;
    private static final Long ID_PAUTA = 100L;

    @Mock
    private PautaEvaluacionRepository pautaRepository;

    @Mock
    private SeccionAsignaturaRepository seccionRepository;

    @Mock
    private EvaluacionAplicadaRepository evaluacionRepository;

    @InjectMocks
    private PautaDefinicionService service;

    private Asignatura asignatura;

    @BeforeEach
    void preparar() {
        asignatura = new Asignatura("Práctica Pedagógica", Ciclo.FINAL, 2);
        asignatura.setIdAsignatura(ID_ASIGNATURA);
    }

    // ---------- helpers ----------

    private static CriterioFormulario criterio(String descripcion, String puntaje) {
        CriterioFormulario criterio = new CriterioFormulario();
        criterio.setDescripcion(descripcion);
        criterio.setPuntajeMax(puntaje);
        return criterio;
    }

    private static DimensionFormulario dimension(String nombre, CriterioFormulario... criterios) {
        DimensionFormulario dimension = new DimensionFormulario();
        dimension.setNombre(nombre);
        dimension.setCriterios(new ArrayList<>(List.of(criterios)));
        return dimension;
    }

    /** Pauta correcta: dos dimensiones, tres criterios. */
    private static PautaFormulario formularioValido() {
        PautaFormulario formulario = new PautaFormulario();
        formulario.setTipoLinea("UNICA");
        formulario.setNumeroAplicacion("1");
        formulario.setPonderacion("30");
        formulario.setDimensiones(new ArrayList<>(List.of(
                dimension("Planificación",
                        criterio("Define objetivos claros", "5"),
                        criterio("Usa recursos pertinentes", "4,5")),
                dimension("Reflexión",
                        criterio("Reflexiona sobre su práctica", "2")))));
        return formulario;
    }

    private PautaEvaluacion pautaExistente(Long id, TipoLinea tipo, int numero) {
        PautaEvaluacion pauta = new PautaEvaluacion(asignatura, tipo, numero, new BigDecimal("30.00"));
        pauta.setIdPautaEvaluacion(id);
        asignatura.getPautasEvaluacion().add(pauta);
        return pauta;
    }

    private Dimension agregarDimension(PautaEvaluacion pauta, Long id, String nombre, int orden) {
        Dimension dimension = new Dimension(pauta, nombre, orden);
        dimension.setIdDimension(id);
        pauta.getDimensiones().add(dimension);
        return dimension;
    }

    private void agregarCriterio(Dimension dimension, Long id, String texto, String puntaje, int orden) {
        Criterio criterio = new Criterio(dimension, texto, new BigDecimal(puntaje), orden);
        criterio.setIdCriterio(id);
        dimension.getCriterios().add(criterio);
    }

    private void dadoQueElProfesorImparteLaAsignatura() {
        when(seccionRepository.findAsignaturasDelProfesor(PROFESOR)).thenReturn(List.of(asignatura));
    }

    /** La pauta 100 existe, es de una asignatura del profesor y trae una dimensión con un criterio. */
    private PautaEvaluacion dadaUnaPautaDelProfesor() {
        PautaEvaluacion pauta = pautaExistente(ID_PAUTA, TipoLinea.UNICA, 1);
        Dimension d = agregarDimension(pauta, 1L, "Antigua", 1);
        agregarCriterio(d, 1L, "Criterio antiguo", "5", 1);
        when(pautaRepository.findById(ID_PAUTA)).thenReturn(Optional.of(pauta));
        when(seccionRepository.profesorImparte(PROFESOR, ID_ASIGNATURA)).thenReturn(true);
        return pauta;
    }

    private List<String> erroresDe(PautaFormulario formulario) {
        PautaInvalidaException ex = assertThrows(PautaInvalidaException.class,
                () -> service.crear(PROFESOR, ID_ASIGNATURA, formulario));
        return ex.getErrores();
    }

    // ---------- consulta ----------

    @Test
    @DisplayName("1. Lista las asignaturas del profesor con sus pautas y si están en uso")
    void listar_conPautas() {
        dadoQueElProfesorImparteLaAsignatura();
        PautaEvaluacion libre = pautaExistente(1L, TipoLinea.UNICA, 1);
        Dimension d = agregarDimension(libre, 1L, "Planificación", 1);
        agregarCriterio(d, 1L, "Primero", "5", 1);
        agregarCriterio(d, 2L, "Segundo", "5", 2);
        PautaEvaluacion usada = pautaExistente(2L, TipoLinea.UNICA, 2);
        when(pautaRepository.findByAsignatura(ID_ASIGNATURA)).thenReturn(List.of(libre, usada));
        when(evaluacionRepository.existsByPauta(1L)).thenReturn(false);
        when(evaluacionRepository.existsByPauta(2L)).thenReturn(true);

        var resultado = service.listarAsignaturas(PROFESOR);

        assertEquals(1, resultado.size());
        var pautas = resultado.get(0).pautas();
        assertEquals(2, pautas.get(0).cantidadCriterios());
        assertFalse(pautas.get(0).enUso());
        assertTrue(pautas.get(1).enUso());
    }

    @Test
    @DisplayName("2. Carga la pauta en el formulario, ordenada y con los números como se escriben")
    void obtenerParaEditar_cargaFormulario() {
        PautaEvaluacion pauta = pautaExistente(ID_PAUTA, TipoLinea.ESPECIALIDAD, 2);
        pauta.setPonderacion(new BigDecimal("25.50"));
        Dimension segunda = agregarDimension(pauta, 2L, "Reflexión", 2);
        Dimension primera = agregarDimension(pauta, 1L, "Planificación", 1);
        agregarCriterio(primera, 12L, "Segundo criterio", "3.50", 2);
        agregarCriterio(primera, 11L, "Primer criterio", "4.00", 1);
        agregarCriterio(segunda, 21L, "Reflexiona", "10.00", 1);
        when(pautaRepository.findById(ID_PAUTA)).thenReturn(Optional.of(pauta));
        when(seccionRepository.profesorImparte(PROFESOR, ID_ASIGNATURA)).thenReturn(true);

        var editable = service.obtenerParaEditar(PROFESOR, ID_PAUTA);

        PautaFormulario f = editable.formulario();
        assertEquals("ESPECIALIDAD", f.getTipoLinea());
        assertEquals("2", f.getNumeroAplicacion());
        assertEquals("25,5", f.getPonderacion());
        assertEquals("Planificación", f.getDimensiones().get(0).getNombre());
        assertEquals("Primer criterio", f.getDimensiones().get(0).getCriterios().get(0).getDescripcion());
        assertEquals("4", f.getDimensiones().get(0).getCriterios().get(0).getPuntajeMax());
        assertEquals("3,5", f.getDimensiones().get(0).getCriterios().get(1).getPuntajeMax());
        assertEquals("10", f.getDimensiones().get(1).getCriterios().get(0).getPuntajeMax());
        assertFalse(editable.enUso());
    }

    @Test
    @DisplayName("3. Una pauta con evaluaciones se carga marcada como en uso")
    void obtenerParaEditar_enUso() {
        dadaUnaPautaDelProfesor();
        when(evaluacionRepository.existsByPauta(ID_PAUTA)).thenReturn(true);

        assertTrue(service.obtenerParaEditar(PROFESOR, ID_PAUTA).enUso());
    }

    @Test
    @DisplayName("4. Lanza 404 si la pauta no existe")
    void obtenerParaEditar_noExiste() {
        when(pautaRepository.findById(ID_PAUTA)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerParaEditar(PROFESOR, ID_PAUTA));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("5. Lanza 403 si la pauta es de una asignatura que el profesor no imparte")
    void obtenerParaEditar_deOtroProfesor() {
        PautaEvaluacion pauta = pautaExistente(ID_PAUTA, TipoLinea.UNICA, 1);
        when(pautaRepository.findById(ID_PAUTA)).thenReturn(Optional.of(pauta));
        when(seccionRepository.profesorImparte(OTRO_PROFESOR, ID_ASIGNATURA)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerParaEditar(OTRO_PROFESOR, ID_PAUTA));

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

    // ---------- crear ----------

    @Test
    @DisplayName("7. Crea la pauta completa con dimensiones y criterios en su orden")
    void crear_ok() {
        dadoQueElProfesorImparteLaAsignatura();
        when(pautaRepository.findByAsignatura(ID_ASIGNATURA)).thenReturn(List.of());
        when(pautaRepository.save(any(PautaEvaluacion.class))).thenAnswer(inv -> {
            PautaEvaluacion p = inv.getArgument(0);
            p.setIdPautaEvaluacion(ID_PAUTA);
            return p;
        });

        Long id = service.crear(PROFESOR, ID_ASIGNATURA, formularioValido());

        assertEquals(ID_PAUTA, id);
        ArgumentCaptor<PautaEvaluacion> captor = ArgumentCaptor.forClass(PautaEvaluacion.class);
        verify(pautaRepository).save(captor.capture());
        PautaEvaluacion guardada = captor.getValue();
        assertEquals(TipoLinea.UNICA, guardada.getTipoLinea());
        assertEquals(1, guardada.getNumeroAplicacion());
        assertEquals(new BigDecimal("30.00"), guardada.getPonderacion());
        assertEquals(asignatura, guardada.getAsignatura());

        assertEquals(2, guardada.getDimensiones().size());
        Dimension primera = guardada.getDimensiones().get(0);
        Dimension segunda = guardada.getDimensiones().get(1);
        assertEquals("Planificación", primera.getNombre());
        assertEquals(1, primera.getOrden());
        assertEquals(2, segunda.getOrden());
        assertEquals(2, primera.getCriterios().size());
        assertEquals(1, primera.getCriterios().get(0).getOrden());
        assertEquals(2, primera.getCriterios().get(1).getOrden());
        assertEquals(new BigDecimal("4.50"), primera.getCriterios().get(1).getPuntajeMax());
        assertEquals(primera, primera.getCriterios().get(0).getDimension());
        assertEquals(guardada, primera.getPautaEvaluacion());
    }

    @Test
    @DisplayName("8. No crea pautas en asignaturas que el profesor no imparte (403)")
    void crear_asignaturaAjena() {
        when(seccionRepository.findAsignaturasDelProfesor(OTRO_PROFESOR)).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.crear(OTRO_PROFESOR, ID_ASIGNATURA, formularioValido()));

        assertEquals(403, ex.getStatusCode().value());
        verify(pautaRepository, never()).save(any());
    }

    @Test
    @DisplayName("9. Rechaza una pauta repetida: misma asignatura, tipo de línea y aplicación")
    void crear_duplicada() {
        dadoQueElProfesorImparteLaAsignatura();
        when(pautaRepository.findByAsignatura(ID_ASIGNATURA))
                .thenReturn(List.of(pautaExistente(1L, TipoLinea.UNICA, 1)));

        List<String> errores = erroresDe(formularioValido());

        assertEquals(1, errores.size());
        assertTrue(errores.get(0).contains("Ya existe una pauta"));
        verify(pautaRepository, never()).save(any());
    }

    @Test
    @DisplayName("10. Informa todos los errores juntos, no solo el primero")
    void crear_erroresJuntos() {
        dadoQueElProfesorImparteLaAsignatura();
        PautaFormulario formulario = new PautaFormulario();
        formulario.setTipoLinea("TODAS");                 // 1: tipo inválido
        formulario.setNumeroAplicacion("0");              // 2: aplicación fuera de rango
        formulario.setPonderacion("150");                 // 3: ponderación sobre 100
        formulario.setDimensiones(new ArrayList<>(List.of(
                dimension(" "),                           // 4: sin nombre  5: sin criterios
                dimension("Reflexión",
                        criterio("", "5"),                // 6: sin descripción
                        criterio("Reflexiona", "-1")))));  // 7: puntaje no positivo

        List<String> errores = erroresDe(formulario);

        assertEquals(7, errores.size(), errores.toString());
        assertTrue(errores.stream().anyMatch(e -> e.contains("tipo de línea")));
        assertTrue(errores.stream().anyMatch(e -> e.contains("número de aplicación")));
        assertTrue(errores.stream().anyMatch(e -> e.contains("ponderación")));
        assertTrue(errores.stream().anyMatch(e -> e.contains("nombre de la dimensión 1")));
        assertTrue(errores.stream().anyMatch(e -> e.contains("La dimensión 1 necesita al menos un criterio")));
        assertTrue(errores.stream().anyMatch(e -> e.contains("descripción del criterio 1 de la dimensión 2")));
        assertTrue(errores.stream().anyMatch(e -> e.contains("puntaje máximo del criterio 2 de la dimensión 2")));
        verify(pautaRepository, never()).save(any());
    }

    @Test
    @DisplayName("11. Exige al menos una dimensión")
    void crear_sinDimensiones() {
        dadoQueElProfesorImparteLaAsignatura();
        when(pautaRepository.findByAsignatura(ID_ASIGNATURA)).thenReturn(List.of());
        PautaFormulario formulario = formularioValido();
        formulario.setDimensiones(new ArrayList<>());

        List<String> errores = erroresDe(formulario);

        assertEquals(List.of("La pauta necesita al menos una dimensión"), errores);
    }

    @Test
    @DisplayName("12. Exige al menos un criterio en cada dimensión")
    void crear_dimensionSinCriterios() {
        dadoQueElProfesorImparteLaAsignatura();
        when(pautaRepository.findByAsignatura(ID_ASIGNATURA)).thenReturn(List.of());
        PautaFormulario formulario = formularioValido();
        formulario.getDimensiones().add(dimension("Vacía"));

        List<String> errores = erroresDe(formulario);

        assertEquals(List.of("La dimensión 3 necesita al menos un criterio"), errores);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "0", "-5", "100,01", "101", "abc", "12,345", "NaN", "12345678901"})
    @DisplayName("13. Rechaza una ponderación vacía, no numérica, fuera de 0-100 o con más de dos decimales")
    void crear_ponderacionInvalida(String ponderacion) {
        dadoQueElProfesorImparteLaAsignatura();
        PautaFormulario formulario = formularioValido();
        formulario.setPonderacion(ponderacion);

        List<String> errores = erroresDe(formulario);

        assertEquals(1, errores.size(), errores.toString());
        assertTrue(errores.get(0).contains("ponderación"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"100", "100,00", "0,01", " 30 "})
    @DisplayName("14. Acepta ponderaciones válidas, incluidos los límites")
    void crear_ponderacionValida(String ponderacion) {
        dadoQueElProfesorImparteLaAsignatura();
        when(pautaRepository.findByAsignatura(ID_ASIGNATURA)).thenReturn(List.of());
        when(pautaRepository.save(any(PautaEvaluacion.class))).thenAnswer(inv -> inv.getArgument(0));
        PautaFormulario formulario = formularioValido();
        formulario.setPonderacion(ponderacion);

        service.crear(PROFESOR, ID_ASIGNATURA, formulario);

        verify(pautaRepository).save(any(PautaEvaluacion.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"0", "-1", "1000", "abc", "5,555", "NaN"})
    @DisplayName("15. Rechaza un puntaje máximo vacío, no numérico, no positivo o con más de dos decimales")
    void crear_puntajeInvalido(String puntaje) {
        dadoQueElProfesorImparteLaAsignatura();
        PautaFormulario formulario = formularioValido();
        formulario.getDimensiones().get(0).getCriterios().get(0).setPuntajeMax(puntaje);

        List<String> errores = erroresDe(formulario);

        assertEquals(1, errores.size(), errores.toString());
        assertTrue(errores.get(0).contains("puntaje máximo del criterio 1 de la dimensión 1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "11", "-1", "abc", "1,5", ""})
    @DisplayName("16. Rechaza un número de aplicación vacío, no entero o fuera de 1 a 10")
    void crear_numeroAplicacionInvalido(String numero) {
        dadoQueElProfesorImparteLaAsignatura();
        PautaFormulario formulario = formularioValido();
        formulario.setNumeroAplicacion(numero);

        List<String> errores = erroresDe(formulario);

        assertEquals(1, errores.size(), errores.toString());
        assertTrue(errores.get(0).contains("número de aplicación"));
    }

    @Test
    @DisplayName("17. Rechaza nombres y descripciones demasiado largos")
    void crear_textosLargos() {
        dadoQueElProfesorImparteLaAsignatura();
        when(pautaRepository.findByAsignatura(ID_ASIGNATURA)).thenReturn(List.of());
        PautaFormulario formulario = formularioValido();
        formulario.getDimensiones().get(0).setNombre("x".repeat(201));
        formulario.getDimensiones().get(0).getCriterios().get(0).setDescripcion("y".repeat(501));

        List<String> errores = erroresDe(formulario);

        assertEquals(2, errores.size(), errores.toString());
    }

    // ---------- actualizar ----------

    @Test
    @DisplayName("18. Actualiza la pauta y reemplaza sus dimensiones y criterios")
    void actualizar_ok() {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();
        when(pautaRepository.findByAsignatura(ID_ASIGNATURA)).thenReturn(List.of(pauta));
        PautaFormulario formulario = formularioValido();
        formulario.setTipoLinea("ORIENTACION");
        formulario.setNumeroAplicacion("3");
        formulario.setPonderacion("40");

        service.actualizar(PROFESOR, ID_PAUTA, formulario);

        assertEquals(TipoLinea.ORIENTACION, pauta.getTipoLinea());
        assertEquals(3, pauta.getNumeroAplicacion());
        assertEquals(new BigDecimal("40.00"), pauta.getPonderacion());
        assertEquals(2, pauta.getDimensiones().size());
        assertEquals("Planificación", pauta.getDimensiones().get(0).getNombre());
        assertEquals(2, pauta.getDimensiones().get(0).getCriterios().size());
        verify(pautaRepository).save(pauta);
    }

    @Test
    @DisplayName("19. Al actualizar, la pauta no choca consigo misma pero sí con otra")
    void actualizar_duplicados() {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();
        PautaEvaluacion otra = pautaExistente(2L, TipoLinea.UNICA, 2);
        when(pautaRepository.findByAsignatura(ID_ASIGNATURA)).thenReturn(List.of(pauta, otra));

        service.actualizar(PROFESOR, ID_PAUTA, formularioValido());   // UNICA / 1: es ella misma
        verify(pautaRepository).save(pauta);

        PautaFormulario choca = formularioValido();
        choca.setNumeroAplicacion("2");                               // UNICA / 2: es la otra pauta
        assertThrows(PautaInvalidaException.class, () -> service.actualizar(PROFESOR, ID_PAUTA, choca));
    }

    @Test
    @DisplayName("20. Si la actualización tiene errores no se cambia nada de la pauta")
    void actualizar_conErrores() {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();
        when(pautaRepository.findByAsignatura(ID_ASIGNATURA)).thenReturn(List.of(pauta));
        PautaFormulario formulario = formularioValido();
        formulario.setPonderacion("abc");

        assertThrows(PautaInvalidaException.class, () -> service.actualizar(PROFESOR, ID_PAUTA, formulario));

        assertEquals(new BigDecimal("30.00"), pauta.getPonderacion());
        assertEquals("Antigua", pauta.getDimensiones().get(0).getNombre());
        verify(pautaRepository, never()).save(any());
    }

    @Test
    @DisplayName("21. Una pauta con evaluaciones no se puede modificar: error y no se guarda nada")
    void actualizar_enUso() {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();
        when(evaluacionRepository.existsByPauta(ID_PAUTA)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.actualizar(PROFESOR, ID_PAUTA, formularioValido()));

        assertEquals("Antigua", pauta.getDimensiones().get(0).getNombre());
        assertEquals("Criterio antiguo", pauta.getDimensiones().get(0).getCriterios().get(0).getDescripcion());
        verify(pautaRepository, never()).save(any());
    }

    @Test
    @DisplayName("22. Otro profesor no puede modificar la pauta (403) y no se guarda nada")
    void actualizar_deOtroProfesor() {
        PautaEvaluacion pauta = pautaExistente(ID_PAUTA, TipoLinea.UNICA, 1);
        when(pautaRepository.findById(ID_PAUTA)).thenReturn(Optional.of(pauta));
        when(seccionRepository.profesorImparte(OTRO_PROFESOR, ID_ASIGNATURA)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.actualizar(OTRO_PROFESOR, ID_PAUTA, formularioValido()));

        assertEquals(403, ex.getStatusCode().value());
        verify(pautaRepository, never()).save(any());
    }

    // ---------- eliminar ----------

    @Test
    @DisplayName("23. Elimina la pauta y la quita de la colección de la asignatura")
    void eliminar_ok() {
        PautaEvaluacion pauta = dadaUnaPautaDelProfesor();

        service.eliminar(PROFESOR, ID_PAUTA);

        assertFalse(asignatura.getPautasEvaluacion().contains(pauta));
        verify(pautaRepository).delete(pauta);
    }

    @Test
    @DisplayName("24. Una pauta con evaluaciones no se puede eliminar")
    void eliminar_enUso() {
        dadaUnaPautaDelProfesor();
        when(evaluacionRepository.existsByPauta(ID_PAUTA)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.eliminar(PROFESOR, ID_PAUTA));

        verify(pautaRepository, never()).delete(any());
    }
}
