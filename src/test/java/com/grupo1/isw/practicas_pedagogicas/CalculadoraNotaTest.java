package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.services.CalculadoraNota;
import com.grupo1.isw.practicas_pedagogicas.services.CalculadoraNota.ItemNota;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** HU-10: reglas numéricas del informe final (escala 1,0 a 7,0 con exigencia y promedio ponderado). */
class CalculadoraNotaTest {
    private final CalculadoraNota calculadora = new CalculadoraNota(bd("60"));

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    /** Compara por valor, sin importar la escala (6.0 == 6.00). */
    private static void assertNota(String esperada, BigDecimal real) {
        assertEquals(0, bd(esperada).compareTo(real), "esperaba " + esperada + " pero fue " + real);
    }

    private BigDecimal notaRedondeada(String logro) {
        return calculadora.redondear(calculadora.notaDesdeLogro(bd(logro)));
    }

    @Test
    @DisplayName("Sin logro la nota es 1,0 y con logro total es 7,0")
    void extremosDeLaEscala() {
        assertNota("1.0", notaRedondeada("0"));
        assertNota("7.0", notaRedondeada("1"));
    }

    @Test
    @DisplayName("Con la exigencia exacta (60 %) la nota es 4,0")
    void laExigenciaEquivaleAlCuatro() {
        assertNota("4.0", notaRedondeada("0.6"));
    }

    @Test
    @DisplayName("Bajo la exigencia la nota sube linealmente de 1,0 a 4,0")
    void tramoBajoLaExigencia() {
        assertNota("2.5", notaRedondeada("0.3"));
    }

    @Test
    @DisplayName("Sobre la exigencia la nota sube linealmente de 4,0 a 7,0")
    void tramoSobreLaExigencia() {
        assertNota("5.5", notaRedondeada("0.8"));
    }

    @Test
    @DisplayName("El redondeo es a un decimal hacia arriba en el punto medio (6,25 pasa a 6,3)")
    void redondeoHalfUp() {
        assertNota("6.3", notaRedondeada("0.9"));
    }

    @Test
    @DisplayName("Un logro fuera de rango se acota: la nota nunca pasa de 7,0 ni baja de 1,0")
    void logroFueraDeRango() {
        assertNota("7.0", notaRedondeada("1.7"));
        assertNota("1.0", notaRedondeada("-0.4"));
        assertNota("1", calculadora.logro(bd("120"), bd("100")));
    }

    @Test
    @DisplayName("La exigencia es configurable: con 50 % el 4,0 se alcanza con la mitad del puntaje")
    void exigenciaConfigurable() {
        CalculadoraNota con50 = new CalculadoraNota(bd("50"));
        assertNota("4.0", con50.redondear(con50.notaDesdeLogro(bd("0.5"))));
        assertNota("50", con50.getExigenciaPorcentaje());
    }

    @Test
    @DisplayName("El logro es puntaje obtenido sobre puntaje máximo")
    void logroDesdePuntajes() {
        assertNota("0.8", calculadora.logro(bd("24"), bd("30")));
        assertThrows(IllegalArgumentException.class, () -> calculadora.logro(bd("5"), bd("0")));
        assertThrows(IllegalArgumentException.class, () -> calculadora.logro(null, bd("10")));
    }

    @Test
    @DisplayName("El promedio pondera cada nota por su peso")
    void promedioPonderado() {
        BigDecimal promedio = calculadora.promedioPonderado(List.of(
                new ItemNota(bd("7.0"), bd("30")),
                new ItemNota(bd("4.0"), bd("70"))));
        assertNota("4.9", promedio);
    }

    @Test
    @DisplayName("El promedio se normaliza: da lo mismo aunque las ponderaciones no sumen 100")
    void promedioNormalizado() {
        BigDecimal sumando100 = calculadora.promedioPonderado(List.of(
                new ItemNota(bd("7.0"), bd("30")), new ItemNota(bd("4.0"), bd("70"))));
        BigDecimal sumando200 = calculadora.promedioPonderado(List.of(
                new ItemNota(bd("7.0"), bd("60")), new ItemNota(bd("4.0"), bd("140"))));
        assertEquals(0, sumando100.compareTo(sumando200));
    }

    @Test
    @DisplayName("No se puede promediar sin notas, con ponderaciones negativas ni con suma cero")
    void promedioInvalido() {
        assertThrows(IllegalArgumentException.class, () -> calculadora.promedioPonderado(List.of()));
        assertThrows(IllegalArgumentException.class, () -> calculadora.promedioPonderado(
                List.of(new ItemNota(bd("5"), bd("-10")))));
        assertThrows(IllegalArgumentException.class, () -> calculadora.promedioPonderado(
                List.of(new ItemNota(bd("5"), bd("0")))));
    }

    @Test
    @DisplayName("La exigencia debe estar entre 0 y 100 (sin incluirlos)")
    void exigenciaInvalida() {
        for (String invalida : List.of("0", "100", "-5", "150")) {
            assertThrows(IllegalArgumentException.class, () -> new CalculadoraNota(bd(invalida)),
                    "la exigencia " + invalida + " debería rechazarse");
        }
    }
}
