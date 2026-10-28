package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.services.informe.ResumenInforme;
import com.grupo1.isw.practicas_pedagogicas.services.informe.ResumenInforme.Aprobacion;
import com.grupo1.isw.practicas_pedagogicas.services.informe.ResumenInforme.Aspecto;
import com.grupo1.isw.practicas_pedagogicas.services.informe.ResumenInforme.AspectoVista;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Lo que la pantalla muestra a partir de las cifras: porcentajes, decimales y cuánto cambió cada aspecto. */
class ResumenInformeTest {

    private ResumenInforme resumen(int total, int conAsistente, int revisadas, int consultas, int enviadas,
                                   int enviadasConAsistente, int valoradas, List<Aspecto> aspectos,
                                   Aprobacion aprobacion) {
        return new ResumenInforme(total, conAsistente, revisadas, consultas, enviadas, enviadasConAsistente,
                valoradas, aspectos, aprobacion);
    }

    private ResumenInforme conAspectos(List<Aspecto> aspectos) {
        return resumen(30, 24, 61, 187, 58, 41, 61, aspectos, new Aprobacion(41, 31, 17, 8));
    }

    @Test
    @DisplayName("HU-26. Calcula los porcentajes de las tarjetas")
    void porcentajesDeLasTarjetas() {
        ResumenInforme r = conAspectos(List.of());

        assertEquals(80, r.pctEstudiantesConAsistente());
        assertEquals(71, r.pctEnviadasConAsistente());
    }

    @Test
    @DisplayName("HU-26. Los porcentajes no fallan cuando no hay base")
    void porcentajesSinBase() {
        ResumenInforme vacio = resumen(0, 0, 0, 0, 0, 0, 0, List.of(), new Aprobacion(0, 0, 0, 0));

        assertEquals(0, vacio.pctEstudiantesConAsistente());
        assertEquals(0, vacio.pctEnviadasConAsistente());
        assertEquals(0, vacio.aprobacion().pctConAsistente());
        assertEquals(0, vacio.aprobacion().pctSinAsistente());
        assertEquals("0", vacio.consultasPorPlanificacion());
    }

    @Test
    @DisplayName("HU-26. Consultas por planificación: una decimal y coma, sin depender del idioma del equipo")
    void consultasPorPlanificacion() {
        assertEquals("3,1", resumen(0, 0, 61, 187, 0, 0, 0, List.of(), new Aprobacion(0, 0, 0, 0)).consultasPorPlanificacion());
        assertEquals("2,5", resumen(0, 0, 4, 10, 0, 0, 0, List.of(), new Aprobacion(0, 0, 0, 0)).consultasPorPlanificacion());
        assertEquals("6,7", resumen(0, 0, 3, 20, 0, 0, 0, List.of(), new Aprobacion(0, 0, 0, 0)).consultasPorPlanificacion());
        assertEquals("1,0", resumen(0, 0, 61, 61, 0, 0, 0, List.of(), new Aprobacion(0, 0, 0, 0)).consultasPorPlanificacion());
    }

    @Test
    @DisplayName("HU-26. La aprobación en la primera evaluación se expresa en porcentajes")
    void porcentajesDeAprobacion() {
        Aprobacion aprobacion = new Aprobacion(41, 31, 17, 8);

        assertEquals(76, aprobacion.pctConAsistente());
        assertEquals(47, aprobacion.pctSinAsistente());
    }

    @Test
    @DisplayName("HU-26. Cada aspecto muestra su porcentaje y su detalle en la primera y en la última revisión")
    void aspectosVista() {
        ResumenInforme r = conAspectos(List.of(new Aspecto("EVALUACION", "Evaluación del aprendizaje", 52, 31, List.of())));

        AspectoVista vista = r.aspectosVista().get(0);
        assertEquals("Evaluación del aprendizaje", vista.nombre());
        assertEquals(85, vista.pctPrimera());
        assertEquals("52 de 61", vista.detallePrimera());
        assertEquals(51, vista.pctUltima());
        assertEquals("31 de 61", vista.detalleUltima());
    }

    @Test
    @DisplayName("HU-26. El cambio se escribe con signo: baja con el signo menos, sube con más y sin cambio es 0")
    void cambioDeCadaAspecto() {
        ResumenInforme r = conAspectos(List.of(
                new Aspecto("A", "Baja", 52, 31, List.of()),     // 85% -> 51%
                new Aspecto("B", "Sube", 10, 16, List.of()),     // 16% -> 26%
                new Aspecto("C", "Igual", 20, 20, List.of())));  // 33% -> 33%

        List<AspectoVista> vistas = r.aspectosVista();
        assertEquals("\u221234 pts", vistas.get(0).cambio());
        assertEquals("+10 pts", vistas.get(1).cambio());
        assertEquals("0 pts", vistas.get(2).cambio());
    }

    @Test
    @DisplayName("HU-26. Los aspectos conservan el orden que trae el resumen")
    void ordenDeLosAspectosEnLaVista() {
        ResumenInforme r = conAspectos(List.of(
                new Aspecto("A", "Primero", 5, 1, List.of()),
                new Aspecto("B", "Segundo", 3, 1, List.of())));

        assertEquals(List.of("Primero", "Segundo"), r.aspectosVista().stream().map(AspectoVista::nombre).toList());
    }
}
