package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.DesgloseInforme;
import com.grupo1.isw.practicas_pedagogicas.dto.DesgloseInforme.PautaDesglose;
import com.grupo1.isw.practicas_pedagogicas.models.TipoEvaluador;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HU-10: el desglose explica la nota y señala con precisión lo que falta. */
class ConstructorInformeTest {

    private static void assertNumero(String esperado, BigDecimal real) {
        assertNotNull(real, "esperaba " + esperado + " pero no hay valor");
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperaba " + esperado + " pero fue " + real);
    }

    @Test
    @DisplayName("Con todo evaluado calcula la calificación sugerida como promedio ponderado")
    void calculaLaCalificacionSugerida() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();

        DesgloseInforme desglose = e.constructor().construir(e.camila);

        assertTrue(desglose.completo());
        // (5,5x50 + 6,25x50 + 4,75x30 + 5,875x30 + 6,25x40) / 200 = 5,78125
        assertNumero("5.8", desglose.calificacionCalculada());
    }

    @Test
    @DisplayName("Cada pauta muestra puntaje, porcentaje de logro y nota")
    void desglosePorPauta() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();

        DesgloseInforme desglose = e.constructor().construir(e.camila);

        assertEquals(2, desglose.evaluaciones().size());
        PautaDesglose especialidad = desglose.evaluaciones().get(0);
        assertEquals("Especialidad · aplicación 1", especialidad.titulo());
        assertTrue(especialidad.evaluada());
        assertNumero("24", especialidad.puntajeObtenido());
        assertNumero("30", especialidad.puntajeMaximo());
        assertNumero("80.0", especialidad.porcentaje());
        assertNumero("5.5", especialidad.nota());

        PautaDesglose orientacion = desglose.evaluaciones().get(1);
        assertNumero("18", orientacion.puntajeObtenido());
        assertNumero("6.3", orientacion.nota());
    }

    @Test
    @DisplayName("El comentario del evaluador de cada criterio llega al desglose: es el 'por qué'")
    void incluyeLosComentariosPorCriterio() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();

        PautaDesglose especialidad = e.constructor().construir(e.camila).evaluaciones().get(0);

        assertEquals(2, especialidad.dimensiones().size());
        var preparacion = especialidad.dimensiones().get(0);
        assertEquals("Preparación de la enseñanza", preparacion.nombre());
        assertNumero("17", preparacion.puntajeObtenido());
        assertNumero("20", preparacion.puntajeMaximo());
        assertEquals("Objetivos bien secuenciados", preparacion.criterios().get(0).comentario());
        assertNull(preparacion.criterios().get(1).comentario(), "sin comentario debe quedar null");
        assertNull(e.constructor().construir(e.camila).evaluaciones().get(1)
                .dimensiones().get(0).criterios().get(1).comentario(), "un comentario vacío debe quedar null");
    }

    @Test
    @DisplayName("Cada entrega muestra su puntaje, su nota y los comentarios del evaluador")
    void desgloseDeEntregas() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();

        DesgloseInforme desglose = e.constructor().construir(e.camila);

        assertEquals(3, desglose.documentos().size());
        var informe = desglose.documentos().get(0);
        assertEquals("Informe 1", informe.etiqueta());
        assertNumero("70", informe.puntaje());
        assertNumero("4.8", informe.nota());
        assertEquals("Falta profundizar", informe.comentarios());
        assertNumero("5.9", desglose.documentos().get(1).nota());
        assertNumero("6.3", desglose.documentos().get(2).nota());
    }

    @Test
    @DisplayName("Informa la suma de ponderaciones y si no es 100")
    void sumaDePonderaciones() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();

        DesgloseInforme desglose = e.constructor().construir(e.camila);

        assertNumero("200", desglose.sumaPonderaciones());
        assertFalse(desglose.ponderacionesSuman100());
    }

    @Test
    @DisplayName("La última modificación es la fecha más reciente entre evaluaciones y entregas")
    void ultimaModificacion() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();

        assertEquals(LocalDateTime.of(2026, 10, 7, 9, 0), e.constructor().construir(e.camila).ultimaModificacion());
    }

    @Test
    @DisplayName("Si falta algo no hay calificación y se lista exactamente qué falta")
    void sinCalificacionSiFaltanCosas() {
        EscenarioInforme e = new EscenarioInforme();
        e.diegoSoloEspecialidad();

        DesgloseInforme desglose = e.constructor().construir(e.diego);

        assertFalse(desglose.completo());
        assertNull(desglose.calificacionCalculada());
        assertEquals(4, desglose.pendientes().size());
        assertEquals("El estudiante no tiene asignada una línea de práctica para evaluar: Orientación · aplicación 1",
                desglose.pendientes().get(0));
        assertEquals("El estudiante aún no entrega: Informe 1", desglose.pendientes().get(1));
        assertEquals("El estudiante aún no entrega: Producto 1", desglose.pendientes().get(2));
        assertEquals("El estudiante aún no entrega: Proyecto de intervención", desglose.pendientes().get(3));
    }

    @Test
    @DisplayName("Aun incompleto, lo ya evaluado conserva su nota y lo pendiente queda marcado")
    void loEvaluadoSeMuestraAunqueFalteOtraCosa() {
        EscenarioInforme e = new EscenarioInforme();
        e.diegoSoloEspecialidad();

        DesgloseInforme desglose = e.constructor().construir(e.diego);

        PautaDesglose especialidad = desglose.evaluaciones().get(0);
        assertTrue(especialidad.evaluada());
        assertNumero("3.5", especialidad.nota()); // 15 de 30 = 50 % de logro
        PautaDesglose orientacion = desglose.evaluaciones().get(1);
        assertFalse(orientacion.evaluada());
        assertNull(orientacion.nota());
        assertFalse(desglose.documentos().get(0).entregado());
    }

    @Test
    @DisplayName("Una entrega subida pero sin puntaje se reporta como 'falta evaluar', no como 'no entregada'")
    void entregaSinEvaluar() {
        EscenarioInforme e = new EscenarioInforme();
        e.diegoSoloEspecialidad();
        e.entregar(e.diego, e.pautaInforme, 9L, null, null, LocalDateTime.of(2026, 10, 5, 10, 0));

        DesgloseInforme desglose = e.constructor().construir(e.diego);

        assertTrue(desglose.pendientes().contains("Falta evaluar la entrega: Informe 1"));
        assertFalse(desglose.pendientes().contains("El estudiante aún no entrega: Informe 1"));
        assertTrue(desglose.documentos().get(0).entregado());
        assertFalse(desglose.documentos().get(0).evaluado());
    }

    @Test
    @DisplayName("Una evaluación a la que le falta un criterio se considera incompleta")
    void evaluacionIncompleta() {
        EscenarioInforme e = new EscenarioInforme();
        e.evaluar(e.diego, TipoLinea.ESPECIALIDAD, e.pauta1, 7L,
                LocalDateTime.of(2026, 10, 3, 9, 0), new Object[][]{{e.c1, "5", null}, {e.c2, "5", null}});

        DesgloseInforme desglose = e.constructor().construir(e.diego);

        assertTrue(desglose.pendientes().stream().anyMatch(p -> p.contains("está incompleta")));
        assertFalse(desglose.evaluaciones().get(0).evaluada());
    }

    @Test
    @DisplayName("Si la línea tiene tutor y este aún no evalúa, la pauta queda pendiente aunque el colaborador ya evaluó")
    void exigeLaEvaluacionDelTutorCuandoLaLineaTieneTutor() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();
        e.asignarTutor(e.camila, TipoLinea.ESPECIALIDAD);

        DesgloseInforme desglose = e.constructor().construir(e.camila);

        assertFalse(desglose.completo());
        assertNull(desglose.calificacionCalculada());
        assertEquals(1, desglose.pendientes().size());
        assertEquals("Falta la evaluación del profesor supervisor: Especialidad · aplicación 1",
                desglose.pendientes().get(0));
        assertFalse(desglose.evaluaciones().get(0).evaluada());
    }

    @Test
    @DisplayName("Con tutor asignado también se exige al colaborador: la evaluación del tutor sola no basta")
    void laEvaluacionDelTutorSolaNoBasta() {
        EscenarioInforme e = new EscenarioInforme();
        e.asignarTutor(e.diego, TipoLinea.ESPECIALIDAD);
        e.evaluar(e.diego, TipoLinea.ESPECIALIDAD, e.pauta1, TipoEvaluador.TUTOR, 30L,
                LocalDateTime.of(2026, 10, 3, 9, 0), new Object[][]{{e.c1, "5", null}, {e.c2, "5", null}, {e.c3, "5", null}});

        DesgloseInforme desglose = e.constructor().construir(e.diego);

        assertTrue(desglose.pendientes().contains("Falta la evaluación del profesor colaborador: Especialidad · aplicación 1"));
        assertFalse(desglose.pendientes().contains("Falta la evaluación del profesor supervisor: Especialidad · aplicación 1"));
    }

    @Test
    @DisplayName("Con colaborador y tutor, la pauta promedia los puntajes por criterio y junta ambos comentarios")
    void promediaColaboradorYTutor() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();   // colaborador en especialidad: 8 + 9 + 7 = 24 de 30
        e.asignarTutor(e.camila, TipoLinea.ESPECIALIDAD);
        e.evaluar(e.camila, TipoLinea.ESPECIALIDAD, e.pauta1, TipoEvaluador.TUTOR, 40L,
                LocalDateTime.of(2026, 10, 4, 9, 0),
                new Object[][]{{e.c1, "6", "Del supervisor"}, {e.c2, "7", null}, {e.c3, "9", null}});

        DesgloseInforme desglose = e.constructor().construir(e.camila);

        assertTrue(desglose.completo());
        PautaDesglose especialidad = desglose.evaluaciones().get(0);
        assertTrue(especialidad.evaluada());
        // criterio 1: (8+6)/2 = 7; criterio 2: (9+7)/2 = 8; criterio 3: (7+9)/2 = 8  ->  23 de 30
        assertNumero("23", especialidad.puntajeObtenido());
        assertNumero("76.7", especialidad.porcentaje());
        assertNumero("5.3", especialidad.nota());
        assertEquals("Objetivos bien secuenciados · Del supervisor",
                especialidad.dimensiones().get(0).criterios().get(0).comentario());
    }

    @Test
    @DisplayName("La evaluación de la pauta de orientación no exige tutor si esa línea no tiene uno")
    void elTutorSoloSeExigeEnSuLinea() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();
        e.asignarTutor(e.camila, TipoLinea.ESPECIALIDAD);   // solo la línea de especialidad tiene tutor

        DesgloseInforme desglose = e.constructor().construir(e.camila);

        assertEquals(1, desglose.pendientes().size());
        assertTrue(desglose.evaluaciones().get(1).evaluada(), "orientación está completa con solo el colaborador");
    }
}
