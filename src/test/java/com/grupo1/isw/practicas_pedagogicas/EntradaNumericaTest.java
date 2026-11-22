package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.services.EntradaNumerica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntradaNumericaTest {

    @Test
    @DisplayName("1. Acepta coma o punto decimal en la ponderación")
    void comaOPunto() {
        List<String> errores = new ArrayList<>();

        assertEquals(new BigDecimal("12.5"), EntradaNumerica.ponderacion("12,5", errores));
        assertEquals(new BigDecimal("12.5"), EntradaNumerica.ponderacion(" 12.5 ", errores));
        assertTrue(errores.isEmpty());
    }

    @Test
    @DisplayName("2. Rechaza ponderaciones fuera de rango, con muchos decimales o que no son números")
    void rechaza() {
        for (String invalida : new String[]{null, "", "  ", "abc", "0", "-1", "100.01", "5.555"}) {
            List<String> errores = new ArrayList<>();

            assertNull(EntradaNumerica.ponderacion(invalida, errores), "debería rechazar '" + invalida + "'");
            assertEquals(1, errores.size());
        }
    }

    @Test
    @DisplayName("3. El 100 exacto es una ponderación válida")
    void cien() {
        assertEquals(new BigDecimal("100"), EntradaNumerica.ponderacion("100", new ArrayList<>()));
    }

    @Test
    @DisplayName("4. La fecha se lee en formato AAAA-MM-DD y vacía es null")
    void fechas() {
        assertEquals(LocalDate.of(2026, 5, 30), EntradaNumerica.fecha("2026-05-30"));
        assertNull(EntradaNumerica.fecha(""));
        assertNull(EntradaNumerica.fecha(null));
        assertThrows(IllegalArgumentException.class, () -> EntradaNumerica.fecha("30/05/2026"));
    }
}
