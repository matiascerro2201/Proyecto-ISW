package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.repositories.InscripcionAdminRepository.InscripcionFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.InscripcionAdminRepository.LineaFila;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InscripcionFilaTest {

    private static InscripcionFila fila(LineaFila... lineas) {
        return new InscripcionFila(1L, 2L, 3L, "Camila Soto", "camila@correo.cl", 4L, "Liceo A", "EN_CURSO",
                List.of(lineas));
    }

    @Test
    @DisplayName("Está completa solo si todas sus líneas tienen colaborador y tutor")
    void completa() {
        assertTrue(fila(new LineaFila(1L, "UNICA", 5L, "Ana Pérez", 6L, "Luis Soto")).completa());
        assertFalse(fila(new LineaFila(1L, "ESPECIALIDAD", 5L, "Ana Pérez", null, null)).completa());
        assertFalse(fila(new LineaFila(1L, "ESPECIALIDAD", 5L, "Ana Pérez", 6L, "Luis Soto"),
                new LineaFila(2L, "ORIENTACION", null, null, 6L, "Luis Soto")).completa());
        assertFalse(fila().completa());
    }

    @Test
    @DisplayName("El texto de búsqueda reúne correo, centro y profesores, sin 'null'")
    void textoBusqueda() {
        String texto = fila(new LineaFila(1L, "UNICA", 5L, "Ana Pérez", null, null)).textoBusqueda();

        assertTrue(texto.contains("camila@correo.cl"));
        assertTrue(texto.contains("Liceo A"));
        assertTrue(texto.contains("Ana Pérez"));
        assertFalse(texto.contains("null"));
    }
}
