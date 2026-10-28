package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.PautaFormulario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PautaFormularioTest {

    /** Dos dimensiones; la primera con dos criterios y la segunda con uno. */
    private static PautaFormulario formularioConFilas() {
        PautaFormulario formulario = PautaFormulario.vacio();   // dimensión 0 con 1 criterio
        formulario.agregarCriterio(0);                          // dimensión 0 con 2 criterios
        formulario.agregarDimension();                          // dimensión 1 con 1 criterio
        formulario.getDimensiones().get(0).setNombre("Primera");
        formulario.getDimensiones().get(1).setNombre("Segunda");
        return formulario;
    }

    @Test
    @DisplayName("1. Una pauta nueva parte con una dimensión que ya trae un criterio vacío")
    void vacio_traeUnaFila() {
        PautaFormulario formulario = PautaFormulario.vacio();

        assertEquals(1, formulario.getDimensiones().size());
        assertEquals(1, formulario.getDimensiones().get(0).getCriterios().size());
    }

    @Test
    @DisplayName("2. Guardar (o ninguna acción) pide guardar la pauta")
    void guardar_devuelveTrue() {
        assertTrue(PautaFormulario.vacio().aplicarAccion("guardar"));
        assertTrue(PautaFormulario.vacio().aplicarAccion(null));
    }

    @Test
    @DisplayName("3. Agregar una dimensión añade una fila con un criterio y no guarda")
    void agregarDimension() {
        PautaFormulario formulario = formularioConFilas();

        boolean guardar = formulario.aplicarAccion("agregarDimension");

        assertFalse(guardar);
        assertEquals(3, formulario.getDimensiones().size());
        assertEquals(1, formulario.getDimensiones().get(2).getCriterios().size());
    }

    @Test
    @DisplayName("4. Quitar una dimensión elimina la fila indicada")
    void quitarDimension() {
        PautaFormulario formulario = formularioConFilas();

        boolean guardar = formulario.aplicarAccion("quitarDimension-0");

        assertFalse(guardar);
        assertEquals(1, formulario.getDimensiones().size());
        assertEquals("Segunda", formulario.getDimensiones().get(0).getNombre());
    }

    @Test
    @DisplayName("5. Agregar un criterio añade una fila solo a la dimensión indicada")
    void agregarCriterio() {
        PautaFormulario formulario = formularioConFilas();

        boolean guardar = formulario.aplicarAccion("agregarCriterio-1");

        assertFalse(guardar);
        assertEquals(2, formulario.getDimensiones().get(0).getCriterios().size());
        assertEquals(2, formulario.getDimensiones().get(1).getCriterios().size());
    }

    @Test
    @DisplayName("6. Quitar un criterio elimina solo ese criterio")
    void quitarCriterio() {
        PautaFormulario formulario = formularioConFilas();
        formulario.getDimensiones().get(0).getCriterios().get(0).setDescripcion("Se queda");
        formulario.getDimensiones().get(0).getCriterios().get(1).setDescripcion("Se va");

        boolean guardar = formulario.aplicarAccion("quitarCriterio-0-1");

        assertFalse(guardar);
        assertEquals(1, formulario.getDimensiones().get(0).getCriterios().size());
        assertEquals("Se queda", formulario.getDimensiones().get(0).getCriterios().get(0).getDescripcion());
        assertEquals(1, formulario.getDimensiones().get(1).getCriterios().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"quitarDimension-7", "quitarDimension--1", "agregarCriterio-9",
            "quitarCriterio-0-9", "quitarCriterio-9-0", "quitarCriterio-0--1"})
    @DisplayName("7. Una acción con índices fuera de rango no cambia nada ni falla")
    void indicesFueraDeRango(String accion) {
        PautaFormulario formulario = formularioConFilas();

        boolean guardar = formulario.aplicarAccion(accion);

        assertFalse(guardar);
        assertEquals(2, formulario.getDimensiones().size());
        assertEquals(2, formulario.getDimensiones().get(0).getCriterios().size());
        assertEquals(1, formulario.getDimensiones().get(1).getCriterios().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"borrarTodo", "quitarDimension", "quitarDimension-x", "quitarCriterio-0",
            "quitarCriterio-a-b", ""})
    @DisplayName("8. Una acción desconocida o mal formada no cambia nada, no falla y no guarda")
    void accionInvalida(String accion) {
        PautaFormulario formulario = formularioConFilas();

        boolean guardar = formulario.aplicarAccion(accion);

        assertFalse(guardar);
        assertEquals(2, formulario.getDimensiones().size());
        assertEquals(2, formulario.getDimensiones().get(0).getCriterios().size());
    }
}
