package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaHallazgo;
import com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaInteraccion;
import com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.CategoriaPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.NivelHallazgo;
import com.grupo1.isw.practicas_pedagogicas.services.informe.CalculadorInforme;
import com.grupo1.isw.practicas_pedagogicas.services.informe.ResumenInforme;
import com.grupo1.isw.practicas_pedagogicas.services.informe.ResumenInforme.Aspecto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Las cifras del informe del profesor: lógica pura, calculada a partir de filas ya leídas. */
class CalculadorInformeTest {

    private static final List<CategoriaPlanificacion> CATEGORIAS = List.of(
            new CategoriaPlanificacion("OBJETIVOS", "Objetivos de aprendizaje", "descripción", 1),
            new CategoriaPlanificacion("EVALUACION", "Evaluación del aprendizaje", "descripción", 2),
            new CategoriaPlanificacion("CIERRE", "Cierre de la clase", "descripción", 3));

    private static final NivelHallazgo DEBIL = NivelHallazgo.DEBIL;
    private static final NivelHallazgo ADECUADO = NivelHallazgo.ADECUADO;

    // ------------------------------------------------------------------ ayudas

    private static LocalDateTime dia(int dia) {
        return LocalDateTime.of(2026, 10, dia, 10, 0);
    }

    private ResumenInforme calcular(List<FilaPlanificacion> planificaciones, List<FilaInteraccion> interacciones,
                                    List<FilaHallazgo> hallazgos, List<FilaEvaluacion> evaluaciones) {
        return CalculadorInforme.calcular(30, CATEGORIAS, planificaciones, interacciones, hallazgos, evaluaciones);
    }

    private ResumenInforme soloHallazgos(List<FilaHallazgo> hallazgos) {
        return calcular(List.of(), List.of(), hallazgos, List.of());
    }

    private FilaHallazgo hallazgo(long plan, long interaccion, int dia, String codigo, NivelHallazgo nivel) {
        return new FilaHallazgo(plan, interaccion, dia(dia), codigo, nivel, null);
    }

    private FilaHallazgo hallazgo(long plan, long interaccion, int dia, String codigo, NivelHallazgo nivel,
                                  String observacion) {
        return new FilaHallazgo(plan, interaccion, dia(dia), codigo, nivel, observacion);
    }

    private FilaInteraccion consulta(long plan, long estudiante, long interaccion, int dia) {
        return new FilaInteraccion(plan, estudiante, interaccion, dia(dia));
    }

    private FilaPlanificacion planificacion(long id, long estudiante, EstadoPlanificacion estado) {
        return new FilaPlanificacion(id, estudiante, estado);
    }

    private FilaEvaluacion evaluacion(long id, long plan, EstadoPlanificacion decision, int dia) {
        return new FilaEvaluacion(id, plan, decision, dia(dia));
    }

    private Aspecto aspecto(ResumenInforme resumen, String codigo) {
        return resumen.aspectos().stream().filter(a -> a.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    // -------------------------------------------------------------------- base

    @Test
    @DisplayName("HU-26. Sin datos, todo es cero y los aspectos igual aparecen")
    void sinDatos() {
        ResumenInforme resumen = calcular(List.of(), List.of(), List.of(), List.of());

        assertEquals(30, resumen.totalEstudiantes());
        assertEquals(0, resumen.estudiantesConAsistente());
        assertEquals(0, resumen.planificacionesRevisadas());
        assertEquals(0, resumen.consultas());
        assertEquals(0, resumen.planificacionesEnviadas());
        assertEquals(0, resumen.enviadasConAsistente());
        assertEquals(0, resumen.planificacionesValoradas());
        assertEquals(3, resumen.aspectos().size());
        assertTrue(resumen.aspectos().stream().allMatch(a -> a.debilesPrimera() == 0 && a.debilesUltima() == 0));
        assertEquals(0, resumen.aprobacion().evaluadasConAsistente());
        assertEquals(0, resumen.aprobacion().evaluadasSinAsistente());
    }

    @Test
    @DisplayName("HU-26. Cuenta estudiantes, planificaciones y consultas sin repetir")
    void cuentaEstudiantesPlanificacionesYConsultas() {
        ResumenInforme resumen = calcular(List.of(), List.of(
                consulta(1, 10, 1, 1), consulta(1, 10, 2, 2),
                consulta(2, 11, 3, 3),
                consulta(3, 10, 4, 4)), List.of(), List.of());

        assertEquals(2, resumen.estudiantesConAsistente());
        assertEquals(3, resumen.planificacionesRevisadas());
        assertEquals(4, resumen.consultas());
    }

    @Test
    @DisplayName("HU-26. El porcentaje se redondea y no falla cuando no hay base")
    void porcentaje() {
        assertEquals(85, ResumenInforme.porcentaje(52, 61));
        assertEquals(51, ResumenInforme.porcentaje(31, 61));
        assertEquals(8, ResumenInforme.porcentaje(5, 61));
        assertEquals(100, ResumenInforme.porcentaje(3, 3));
        assertEquals(0, ResumenInforme.porcentaje(0, 10));
        assertEquals(0, ResumenInforme.porcentaje(5, 0));
    }

    // ----------------------------------------------- aspectos: primera y última

    @Test
    @DisplayName("HU-26. Compara la primera valoración con la última de una misma planificación")
    void primeraYUltimaValoracion() {
        ResumenInforme resumen = soloHallazgos(List.of(
                hallazgo(1, 1, 1, "OBJETIVOS", DEBIL), hallazgo(1, 1, 1, "EVALUACION", DEBIL),
                hallazgo(1, 1, 1, "CIERRE", ADECUADO),
                hallazgo(1, 2, 3, "OBJETIVOS", ADECUADO), hallazgo(1, 2, 3, "EVALUACION", DEBIL),
                hallazgo(1, 2, 3, "CIERRE", DEBIL)));

        assertEquals(1, resumen.planificacionesValoradas());
        assertEquals(1, aspecto(resumen, "OBJETIVOS").debilesPrimera());
        assertEquals(0, aspecto(resumen, "OBJETIVOS").debilesUltima());
        assertEquals(1, aspecto(resumen, "EVALUACION").debilesPrimera());
        assertEquals(1, aspecto(resumen, "EVALUACION").debilesUltima());
        assertEquals(0, aspecto(resumen, "CIERRE").debilesPrimera());
        assertEquals(1, aspecto(resumen, "CIERRE").debilesUltima());
    }

    @Test
    @DisplayName("HU-26. Con una sola consulta, la primera y la última valoración coinciden")
    void unaSolaConsulta() {
        ResumenInforme resumen = soloHallazgos(List.of(
                hallazgo(1, 1, 1, "OBJETIVOS", DEBIL), hallazgo(1, 1, 1, "EVALUACION", ADECUADO)));

        assertEquals(1, aspecto(resumen, "OBJETIVOS").debilesPrimera());
        assertEquals(1, aspecto(resumen, "OBJETIVOS").debilesUltima());
        assertEquals(0, aspecto(resumen, "EVALUACION").debilesPrimera());
        assertEquals(0, aspecto(resumen, "EVALUACION").debilesUltima());
    }

    @Test
    @DisplayName("HU-26. Las planificaciones se suman, y cada una cuenta una vez aunque haya hecho muchas consultas")
    void variasPlanificaciones() {
        ResumenInforme resumen = soloHallazgos(List.of(
                hallazgo(1, 1, 1, "OBJETIVOS", DEBIL), hallazgo(1, 1, 1, "EVALUACION", DEBIL),
                hallazgo(1, 1, 1, "CIERRE", ADECUADO),
                hallazgo(1, 2, 3, "OBJETIVOS", ADECUADO), hallazgo(1, 2, 3, "EVALUACION", DEBIL),
                hallazgo(1, 2, 3, "CIERRE", DEBIL),
                hallazgo(2, 3, 2, "OBJETIVOS", DEBIL), hallazgo(2, 3, 2, "EVALUACION", ADECUADO),
                hallazgo(2, 3, 2, "CIERRE", DEBIL)));

        assertEquals(2, resumen.planificacionesValoradas());
        assertEquals(2, aspecto(resumen, "OBJETIVOS").debilesPrimera());
        assertEquals(1, aspecto(resumen, "OBJETIVOS").debilesUltima());
        assertEquals(1, aspecto(resumen, "EVALUACION").debilesPrimera());
        assertEquals(1, aspecto(resumen, "EVALUACION").debilesUltima());
        assertEquals(1, aspecto(resumen, "CIERRE").debilesPrimera());
        assertEquals(2, aspecto(resumen, "CIERRE").debilesUltima());
    }

    @Test
    @DisplayName("HU-26. La primera y la última no dependen del orden en que llegan las filas")
    void ordenDeLasFilas() {
        ResumenInforme resumen = soloHallazgos(List.of(
                hallazgo(1, 2, 3, "OBJETIVOS", ADECUADO),
                hallazgo(1, 1, 1, "OBJETIVOS", DEBIL)));

        assertEquals(1, aspecto(resumen, "OBJETIVOS").debilesPrimera());
        assertEquals(0, aspecto(resumen, "OBJETIVOS").debilesUltima());
    }

    @Test
    @DisplayName("HU-26. Los aspectos salen de más a menos débiles en la primera revisión, y a igualdad, en el orden del catálogo")
    void ordenDeLosAspectos() {
        ResumenInforme resumen = soloHallazgos(List.of(
                hallazgo(1, 1, 1, "EVALUACION", DEBIL), hallazgo(1, 1, 1, "CIERRE", DEBIL),
                hallazgo(2, 2, 1, "EVALUACION", DEBIL), hallazgo(2, 2, 1, "OBJETIVOS", DEBIL)));

        assertEquals(List.of("EVALUACION", "OBJETIVOS", "CIERRE"),
                resumen.aspectos().stream().map(Aspecto::codigo).toList());
    }

    @Test
    @DisplayName("HU-26. Ignora los aspectos que ya no están en el catálogo")
    void codigosDesconocidos() {
        ResumenInforme resumen = soloHallazgos(List.of(
                hallazgo(1, 1, 1, "OBJETIVOS", DEBIL), hallazgo(1, 1, 1, "INVENTADA", DEBIL),
                hallazgo(2, 2, 1, "INVENTADA", DEBIL)));

        assertEquals(1, resumen.planificacionesValoradas());
        assertEquals(3, resumen.aspectos().size());
        assertEquals(1, aspecto(resumen, "OBJETIVOS").debilesPrimera());
    }

    @Test
    @DisplayName("HU-26. Una consulta sin valoraciones cuenta como revisada pero no como valorada")
    void revisadasYValoradas() {
        ResumenInforme resumen = calcular(List.of(),
                List.of(consulta(1, 10, 1, 1), consulta(2, 11, 2, 2)),
                List.of(hallazgo(1, 1, 1, "OBJETIVOS", DEBIL)), List.of());

        assertEquals(2, resumen.planificacionesRevisadas());
        assertEquals(1, resumen.planificacionesValoradas());
    }

    // -------------------------------------------------------- ejemplos de texto

    @Test
    @DisplayName("HU-26. Los ejemplos no se repiten, son como mucho tres y siguen el orden de las planificaciones")
    void ejemplosDistintosYAcotados() {
        ResumenInforme resumen = soloHallazgos(List.of(
                hallazgo(1, 1, 1, "OBJETIVOS", DEBIL, "No son medibles"),
                hallazgo(2, 2, 1, "OBJETIVOS", DEBIL, "No son medibles"),
                hallazgo(3, 3, 1, "OBJETIVOS", DEBIL, "Falta el verbo de acción"),
                hallazgo(4, 4, 1, "OBJETIVOS", DEBIL, "Muy generales"),
                hallazgo(5, 5, 1, "OBJETIVOS", DEBIL, "Un cuarto ejemplo")));

        assertEquals(List.of("No son medibles", "Falta el verbo de acción", "Muy generales"),
                aspecto(resumen, "OBJETIVOS").ejemplos());
    }

    @Test
    @DisplayName("HU-26. Los ejemplos ignoran lo vacío, quitan espacios y se recortan a 200 caracteres")
    void ejemplosLimpios() {
        String largo = "x".repeat(300);
        ResumenInforme resumen = soloHallazgos(List.of(
                hallazgo(1, 1, 1, "OBJETIVOS", DEBIL, null),
                hallazgo(2, 2, 1, "OBJETIVOS", DEBIL, "   "),
                hallazgo(3, 3, 1, "OBJETIVOS", DEBIL, "  Con espacios  "),
                hallazgo(4, 4, 1, "OBJETIVOS", DEBIL, largo)));

        List<String> ejemplos = aspecto(resumen, "OBJETIVOS").ejemplos();
        assertEquals(2, ejemplos.size());
        assertEquals("Con espacios", ejemplos.get(0));
        assertEquals(200, ejemplos.get(1).length());
    }

    @Test
    @DisplayName("HU-26. Los ejemplos salen solo de lo débil en la primera revisión")
    void ejemplosSoloDeLaPrimeraRevision() {
        ResumenInforme resumen = soloHallazgos(List.of(
                hallazgo(1, 1, 1, "OBJETIVOS", ADECUADO, "Está bien"),
                hallazgo(1, 2, 3, "OBJETIVOS", DEBIL, "Solo débil en la última"),
                hallazgo(2, 3, 1, "OBJETIVOS", DEBIL, "Débil desde el principio")));

        assertEquals(List.of("Débil desde el principio"), aspecto(resumen, "OBJETIVOS").ejemplos());
    }

    // ---------------------------------------------------- enviadas y asistente

    @Test
    @DisplayName("HU-26. Una enviada pasó por el asistente si consultó antes de su primera evaluación")
    void enviadasConAsistente() {
        ResumenInforme resumen = calcular(
                List.of(planificacion(1, 10, EstadoPlanificacion.BORRADOR),    // borrador: no es una enviada
                        planificacion(2, 11, EstadoPlanificacion.ENVIADA),     // sin evaluar y con consulta
                        planificacion(3, 12, EstadoPlanificacion.ENVIADA),     // sin consultas
                        planificacion(4, 13, EstadoPlanificacion.APROBADA),    // consultó antes de que la evaluaran
                        planificacion(5, 14, EstadoPlanificacion.RECHAZADA)),  // consultó solo después del rechazo
                List.of(consulta(1, 10, 1, 1), consulta(2, 11, 2, 1), consulta(4, 13, 3, 1), consulta(5, 14, 4, 8)),
                List.of(),
                List.of(evaluacion(1, 4, EstadoPlanificacion.APROBADA, 5),
                        evaluacion(2, 5, EstadoPlanificacion.RECHAZADA, 5)));

        assertEquals(4, resumen.planificacionesEnviadas());
        assertEquals(2, resumen.enviadasConAsistente());
    }

    // ---------------------------------------------------------------- aprobación

    @Test
    @DisplayName("HU-26. La aprobación se mide en la primera evaluación, con y sin consulta previa al asistente")
    void aprobacionEnLaPrimeraEvaluacion() {
        ResumenInforme resumen = calcular(List.of(),
                List.of(consulta(1, 10, 1, 1), consulta(2, 11, 2, 1)),
                List.of(),
                List.of(evaluacion(1, 1, EstadoPlanificacion.APROBADA, 3),     // con asistente, aprobada
                        evaluacion(2, 2, EstadoPlanificacion.RECHAZADA, 3),    // con asistente, rechazada primero...
                        evaluacion(3, 2, EstadoPlanificacion.APROBADA, 6),     // ...y aprobada después: no cuenta
                        evaluacion(4, 3, EstadoPlanificacion.APROBADA, 2),     // sin asistente, aprobada
                        evaluacion(5, 4, EstadoPlanificacion.RECHAZADA, 2),    // sin asistente, rechazada
                        evaluacion(6, 5, EstadoPlanificacion.APROBADA, 2)));   // sin asistente, aprobada

        assertEquals(2, resumen.aprobacion().evaluadasConAsistente());
        assertEquals(1, resumen.aprobacion().aprobadasConAsistente());
        assertEquals(3, resumen.aprobacion().evaluadasSinAsistente());
        assertEquals(2, resumen.aprobacion().aprobadasSinAsistente());
    }

    @Test
    @DisplayName("HU-26. La primera evaluación no depende del orden de las filas, y con igual fecha gana el menor identificador")
    void primeraEvaluacion() {
        ResumenInforme desordenadas = calcular(List.of(), List.of(), List.of(),
                List.of(evaluacion(2, 1, EstadoPlanificacion.APROBADA, 6),
                        evaluacion(1, 1, EstadoPlanificacion.RECHAZADA, 3)));
        assertEquals(0, desordenadas.aprobacion().aprobadasSinAsistente());
        assertEquals(1, desordenadas.aprobacion().evaluadasSinAsistente());

        ResumenInforme empate = calcular(List.of(), List.of(), List.of(),
                List.of(evaluacion(7, 1, EstadoPlanificacion.APROBADA, 3),
                        evaluacion(5, 1, EstadoPlanificacion.RECHAZADA, 3)));
        assertEquals(0, empate.aprobacion().aprobadasSinAsistente());
        assertEquals(1, empate.aprobacion().evaluadasSinAsistente());
    }

    @Test
    @DisplayName("HU-26. Una consulta posterior a la primera evaluación no cuenta como consulta previa")
    void consultaPosteriorNoCuenta() {
        ResumenInforme resumen = calcular(List.of(),
                List.of(consulta(1, 10, 1, 8)),
                List.of(),
                List.of(evaluacion(1, 1, EstadoPlanificacion.RECHAZADA, 5),
                        evaluacion(2, 1, EstadoPlanificacion.APROBADA, 12)));

        assertEquals(0, resumen.aprobacion().evaluadasConAsistente());
        assertEquals(1, resumen.aprobacion().evaluadasSinAsistente());
        assertEquals(0, resumen.aprobacion().aprobadasSinAsistente());
    }
}
