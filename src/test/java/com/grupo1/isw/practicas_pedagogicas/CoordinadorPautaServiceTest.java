package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.PautaFormulario.CriterioFormulario;
import com.grupo1.isw.practicas_pedagogicas.dto.PautaFormulario.DimensionFormulario;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.PautaSemestreFormulario;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaSemestreRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaSemestreRepository.CriterioFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaSemestreRepository.DimensionFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaSemestreRepository.PautaFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository.SeccionDatos;
import com.grupo1.isw.practicas_pedagogicas.services.CoordinadorPautaService;
import com.grupo1.isw.practicas_pedagogicas.services.EstructuraInvalidaException;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoordinadorPautaServiceTest {
    private static final long ID_SECCION = 1L;
    private static final long ID_PAUTA = 8L;

    @Mock
    PautaSemestreRepository pautas;
    @Mock
    SeccionEstructuraRepository secciones;

    private CoordinadorPautaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new CoordinadorPautaService(pautas, secciones);
        when(secciones.buscar(ID_SECCION)).thenReturn(Optional.of(seccion(2, false)));
        when(pautas.buscar(ID_PAUTA)).thenReturn(Optional.of(pauta(ID_SECCION)));
    }

    private static SeccionDatos seccion(int numeroLineas, boolean iniciada) {
        return new SeccionDatos(ID_SECCION, 10L, "Práctica Pedagógica", numeroLineas, 2026, 1, 5L, "Profesor",
                iniciada, null);
    }

    private static PautaFila pauta(long idSeccion) {
        return new PautaFila(ID_PAUTA, idSeccion, "Desempeño", "COLABORADOR", "ESPECIALIDAD", 1,
                new BigDecimal("15.00"), true, null, 2, BigDecimal.TEN);
    }

    private static PautaSemestreFormulario formulario(String evaluador, String linea, String aplicacion, String pond) {
        PautaSemestreFormulario f = new PautaSemestreFormulario();
        f.setNombre("Desempeño");
        f.setEvaluador(evaluador);
        f.setTipoLinea(linea);
        f.setAplicacion(aplicacion);
        f.setPonderacion(pond);
        return f;
    }

    private static DimensionFormulario dimension(String nombre, String... criterios) {
        DimensionFormulario d = new DimensionFormulario();
        d.setNombre(nombre);
        for (int i = 0; i + 1 < criterios.length; i += 2) {
            CriterioFormulario c = new CriterioFormulario();
            c.setDescripcion(criterios[i]);
            c.setPuntajeMax(criterios[i + 1]);
            d.getCriterios().add(c);
        }
        return d;
    }

    private static PautaSemestreFormulario conContenido(DimensionFormulario... dimensiones) {
        PautaSemestreFormulario f = formulario("COLABORADOR", "ESPECIALIDAD", "2", "15");
        f.setDimensiones(new java.util.ArrayList<>(List.of(dimensiones)));
        return f;
    }

    @Test
    @DisplayName("1. Crea una pauta de colaborador para una línea de la asignatura")
    void creaPauta() {
        when(pautas.insertar(10L, ID_SECCION, "Desempeño", "COLABORADOR", "ESPECIALIDAD", 2,
                new BigDecimal("15"), true, null)).thenReturn(30L);

        long id = servicio.crear(ID_SECCION, formulario("COLABORADOR", "ESPECIALIDAD", "2", "15"));

        assertEquals(30L, id);
    }

    @Test
    @DisplayName("2. El evaluador, la línea, la aplicación y la ponderación son obligatorios")
    void camposObligatorios() {
        PautaSemestreFormulario vacio = new PautaSemestreFormulario();
        vacio.setAplicacion(null);

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> servicio.crear(ID_SECCION, vacio));

        assertEquals(5, ex.getErrores().size());
    }

    @Test
    @DisplayName("3. En una asignatura de una línea solo vale la línea única")
    void lineaSegunAsignatura() {
        when(secciones.buscar(ID_SECCION)).thenReturn(Optional.of(seccion(1, false)));

        assertThrows(EstructuraInvalidaException.class,
                () -> servicio.crear(ID_SECCION, formulario("TUTOR", "ESPECIALIDAD", "1", "10")));
        servicio.crear(ID_SECCION, formulario("TUTOR", "UNICA", "1", "10"));

        verify(pautas).insertar(10L, ID_SECCION, "Desempeño", "TUTOR", "UNICA", 1, new BigDecimal("10"), true, null);
    }

    @Test
    @DisplayName("4. No se repite evaluador + línea + aplicación en el mismo semestre")
    void noSeRepite() {
        when(pautas.existe(ID_SECCION, "TUTOR", "ORIENTACION", 1, null)).thenReturn(true);

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> servicio.crear(ID_SECCION, formulario("TUTOR", "ORIENTACION", "1", "10")));

        assertTrue(ex.getErrores().get(0).contains("Ya existe"));
        verify(pautas, never()).insertar(anyLong(), anyLong(), anyString(), anyString(), anyString(), anyInt(),
                any(BigDecimal.class), anyBoolean(), org.mockito.ArgumentMatchers.<java.time.LocalDate>any());
    }

    @Test
    @DisplayName("5. Con el semestre iniciado no se puede cambiar nada de la pauta")
    void congelada() {
        when(secciones.buscar(ID_SECCION)).thenReturn(Optional.of(seccion(2, true)));

        assertThrows(IllegalStateException.class,
                () -> servicio.crear(ID_SECCION, formulario("TUTOR", "ESPECIALIDAD", "1", "10")));
        assertThrows(IllegalStateException.class, () -> servicio.eliminar(ID_SECCION, ID_PAUTA));
        assertThrows(IllegalStateException.class, () -> servicio.agregarDimension(ID_SECCION, ID_PAUTA, "Dim"));
        assertThrows(IllegalStateException.class, () -> servicio.cambiarHabilitada(ID_SECCION, ID_PAUTA, false));
    }

    @Test
    @DisplayName("6. Una pauta con evaluaciones aplicadas no se modifica ni se elimina")
    void conEvaluaciones() {
        when(pautas.tieneEvaluacionesAplicadas(ID_PAUTA)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> servicio.eliminar(ID_SECCION, ID_PAUTA));
        assertThrows(IllegalStateException.class,
                () -> servicio.actualizar(ID_SECCION, ID_PAUTA, formulario("COLABORADOR", "ESPECIALIDAD", "1", "10")));
        verify(pautas, never()).eliminar(anyLong());
    }

    @Test
    @DisplayName("7. Una pauta de otra sección se trata como inexistente")
    void pautaAjena() {
        when(pautas.buscar(ID_PAUTA)).thenReturn(Optional.of(pauta(99L)));

        assertThrows(ResponseStatusException.class, () -> servicio.detalle(ID_SECCION, ID_PAUTA));
    }

    @Test
    @DisplayName("8. Agrega dimensiones con nombre y criterios con puntaje válido")
    void agregaContenido() {
        when(pautas.pautaDeDimension(4L)).thenReturn(Optional.of(ID_PAUTA));

        servicio.agregarDimension(ID_SECCION, ID_PAUTA, "  Manejo de grupo ");
        servicio.agregarCriterio(ID_SECCION, ID_PAUTA, 4L, " Mantiene el orden ", "7,5");

        verify(pautas).insertarDimension(ID_PAUTA, "Manejo de grupo");
        verify(pautas).insertarCriterio(4L, "Mantiene el orden", new BigDecimal("7.5"));
    }

    @Test
    @DisplayName("9. La dimensión, la descripción y el puntaje del criterio se validan")
    void validaContenido() {
        when(pautas.pautaDeDimension(4L)).thenReturn(Optional.of(ID_PAUTA));

        assertThrows(EstructuraInvalidaException.class, () -> servicio.agregarDimension(ID_SECCION, ID_PAUTA, " "));
        assertThrows(EstructuraInvalidaException.class,
                () -> servicio.agregarCriterio(ID_SECCION, ID_PAUTA, 4L, "", "5"));
        assertThrows(EstructuraInvalidaException.class,
                () -> servicio.agregarCriterio(ID_SECCION, ID_PAUTA, 4L, "Criterio", "0"));
        assertThrows(EstructuraInvalidaException.class,
                () -> servicio.agregarCriterio(ID_SECCION, ID_PAUTA, 4L, "Criterio", "1000"));
        assertThrows(EstructuraInvalidaException.class,
                () -> servicio.agregarCriterio(ID_SECCION, ID_PAUTA, 4L, "Criterio", "x"));
    }

    @Test
    @DisplayName("10. Una dimensión de otra pauta no se puede tocar")
    void dimensionAjena() {
        when(pautas.pautaDeDimension(4L)).thenReturn(Optional.of(555L));

        assertThrows(ResponseStatusException.class, () -> servicio.eliminarDimension(ID_SECCION, ID_PAUTA, 4L));
        assertThrows(ResponseStatusException.class,
                () -> servicio.agregarCriterio(ID_SECCION, ID_PAUTA, 4L, "Criterio", "5"));
    }

    @Test
    @DisplayName("11. Solo se borra un criterio que pertenece a la dimensión indicada")
    void criterioAjeno() {
        when(pautas.pautaDeDimension(4L)).thenReturn(Optional.of(ID_PAUTA));
        when(pautas.dimensionDeCriterio(70L)).thenReturn(Optional.of(5L));
        when(pautas.dimensionDeCriterio(71L)).thenReturn(Optional.of(4L));

        assertThrows(ResponseStatusException.class, () -> servicio.eliminarCriterio(ID_SECCION, ID_PAUTA, 4L, 70L));
        servicio.eliminarCriterio(ID_SECCION, ID_PAUTA, 4L, 71L);

        verify(pautas).eliminarCriterio(71L);
        verify(pautas, never()).eliminarCriterio(70L);
    }

    @Test
    @DisplayName("12. El detalle trae las dimensiones de la pauta")
    void detalle() {
        when(pautas.dimensiones(ID_PAUTA)).thenReturn(List.of());

        assertEquals(Long.valueOf(ID_PAUTA), servicio.detalle(ID_SECCION, ID_PAUTA).pauta().id());
    }

    @Test
    @DisplayName("13. Crea la pauta junto con sus dimensiones y criterios")
    void creaPautaConContenido() {
        when(pautas.insertar(10L, ID_SECCION, "Desempeño", "COLABORADOR", "ESPECIALIDAD", 2,
                new BigDecimal("15"), true, null)).thenReturn(30L);
        when(pautas.insertarDimension(30L, "Preparación")).thenReturn(101L);
        when(pautas.insertarDimension(30L, "Ejecución")).thenReturn(102L);

        servicio.crear(ID_SECCION, conContenido(
                dimension("  Preparación ", " Define objetivos ", "7,5", "Organiza el material", "5"),
                dimension("Ejecución", "Mantiene el orden", "10")));

        verify(pautas).insertarCriterio(101L, "Define objetivos", new BigDecimal("7.5"));
        verify(pautas).insertarCriterio(101L, "Organiza el material", new BigDecimal("5"));
        verify(pautas).insertarCriterio(102L, "Mantiene el orden", new BigDecimal("10"));
    }

    @Test
    @DisplayName("14. El contenido se valida entero y los errores dicen dónde están; no se guarda nada")
    void validaContenidoCompleto() {
        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> servicio.crear(ID_SECCION, conContenido(
                        dimension(" ", "Criterio", "5"),
                        dimension("Sin criterios"),
                        dimension("Con errores", "", "5", "Descripción", "0", "Otra", "x"))));

        String errores = String.join(" | ", ex.getErrores());
        assertTrue(errores.contains("nombre de la dimensión 1"));
        assertTrue(errores.contains("dimensión 2 necesita al menos un criterio"));
        assertTrue(errores.contains("descripción del criterio 1 de la dimensión 3"));
        assertTrue(errores.contains("puntaje máximo del criterio 2 de la dimensión 3"));
        assertTrue(errores.contains("puntaje máximo del criterio 3 de la dimensión 3"));
        verify(pautas, never()).insertar(anyLong(), anyLong(), anyString(), anyString(), anyString(), anyInt(),
                any(BigDecimal.class), anyBoolean(), org.mockito.ArgumentMatchers.<java.time.LocalDate>any());
        verify(pautas, never()).insertarDimension(anyLong(), anyString());
    }

    @Test
    @DisplayName("15. Al guardar una pauta con contenido, reemplaza el que tenía")
    void actualizaReemplazandoContenido() {
        when(pautas.insertarDimension(ID_PAUTA, "Nueva")).thenReturn(200L);

        servicio.actualizar(ID_SECCION, ID_PAUTA, conContenido(dimension("Nueva", "Criterio", "4")));

        org.mockito.InOrder orden = org.mockito.Mockito.inOrder(pautas);
        orden.verify(pautas).eliminarDimensiones(ID_PAUTA);
        orden.verify(pautas).insertarDimension(ID_PAUTA, "Nueva");
        orden.verify(pautas).insertarCriterio(200L, "Criterio", new BigDecimal("4"));
    }

    @Test
    @DisplayName("16. Si se guarda sin dimensiones, el contenido que ya tenía la pauta no se toca")
    void actualizaSinTocarContenido() {
        servicio.actualizar(ID_SECCION, ID_PAUTA, formulario("COLABORADOR", "ESPECIALIDAD", "1", "10"));

        verify(pautas).actualizar(ID_PAUTA, "Desempeño", "COLABORADOR", "ESPECIALIDAD", 1, new BigDecimal("10"));
        verify(pautas, never()).eliminarDimensiones(anyLong());
    }

    @Test
    @DisplayName("17. El formulario de edición trae las dimensiones y criterios guardados")
    void formularioConContenido() {
        when(pautas.dimensiones(ID_PAUTA)).thenReturn(List.of(new DimensionFila(4L, "Preparación", 1,
                List.of(new CriterioFila(70L, "Define objetivos", new BigDecimal("7.50"), 1)))));

        PautaSemestreFormulario f = servicio.formularioDe(ID_SECCION, ID_PAUTA);

        assertEquals(1, f.getDimensiones().size());
        assertEquals("Preparación", f.getDimensiones().get(0).getNombre());
        assertEquals("Define objetivos", f.getDimensiones().get(0).getCriterios().get(0).getDescripcion());
        assertEquals("7.5", f.getDimensiones().get(0).getCriterios().get(0).getPuntajeMax());
    }

    @Test
    @DisplayName("18. El formulario de una pauta nueva parte con una dimensión y un criterio en blanco")
    void formularioVacio() {
        PautaSemestreFormulario f = PautaSemestreFormulario.vacio();

        assertEquals(1, f.getDimensiones().size());
        assertEquals(1, f.getDimensiones().get(0).getCriterios().size());
    }
}
