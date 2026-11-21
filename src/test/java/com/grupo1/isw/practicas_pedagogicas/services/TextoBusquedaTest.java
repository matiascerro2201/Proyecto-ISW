package com.grupo1.isw.practicas_pedagogicas.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextoBusquedaTest {
    @Test
    void ignoraMayusculasYTildes() {
        assertEquals("matematica", TextoBusqueda.normalizar("  Matemática "));
        assertTrue(TextoBusqueda.contiene("Práctica Pedagógica", TextoBusqueda.normalizar("PEDAGOGICA")));
    }

    @Test
    void sinTextoCoincideTodo() {
        assertTrue(TextoBusqueda.contiene("cualquier cosa", TextoBusqueda.normalizar(null)));
        assertTrue(TextoBusqueda.contiene("cualquier cosa", TextoBusqueda.normalizar("   ")));
    }

    @Test
    void textoNuloNoCoincideConUnaBusqueda() {
        assertFalse(TextoBusqueda.contiene(null, TextoBusqueda.normalizar("ana")));
    }
}
