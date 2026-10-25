package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.EstudianteInformeResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.EstudianteInformeResponse.Estado;
import com.grupo1.isw.practicas_pedagogicas.dto.InformeProfesorResponse;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoInscripcion;
import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.services.GeneracionInformeService;
import com.grupo1.isw.practicas_pedagogicas.services.InformeNoGenerableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.grupo1.isw.practicas_pedagogicas.EscenarioInforme.ID_INSCRIPCION_CAMILA;
import static com.grupo1.isw.practicas_pedagogicas.EscenarioInforme.ID_INSCRIPCION_DIEGO;
import static com.grupo1.isw.practicas_pedagogicas.EscenarioInforme.ID_PROFESOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HU-10: generar el informe final: reglas de acceso, de completitud y de regeneración. */
class GeneracionInformeServiceTest {
    private static final long OTRO_PROFESOR = 99L;

    private static void assertNumero(String esperado, BigDecimal real) {
        assertNotNull(real);
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperaba " + esperado + " pero fue " + real);
    }

    // ------------------------------------------------------------------ generar

    @Test
    @DisplayName("Genera el informe con la calificación sugerida y la fecha de generación")
    void generaElInforme() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();

        Long idInforme = e.servicio().generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA);

        InformeFinal guardado = e.informes.get(ID_INSCRIPCION_CAMILA);
        assertNotNull(idInforme);
        assertEquals(idInforme, guardado.getIdInforme());
        assertNumero("5.8", guardado.getCalificacionSugerida());
        assertNotNull(guardado.getFechaGeneracion());
        assertSame(e.profesor, guardado.getCorrector());
    }

    @Test
    @DisplayName("Generar no toca lo que corresponde a HU-11: ni nota final, ni validación, ni publicación")
    void noInvadeHU11() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();

        e.servicio().generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA);

        InformeFinal guardado = e.informes.get(ID_INSCRIPCION_CAMILA);
        assertNull(guardado.getCalificacionFinal());
        assertNull(guardado.getFechaValidacion());
        assertNull(guardado.getFechaPublicacion());
    }

    @Test
    @DisplayName("No genera el informe si faltan evaluaciones o entregas, y dice todo lo que falta")
    void noGeneraSiFaltanCosas() {
        EscenarioInforme e = new EscenarioInforme();
        e.diegoSoloEspecialidad();

        InformeNoGenerableException ex = assertThrows(InformeNoGenerableException.class,
                () -> e.servicio().generar(ID_PROFESOR, ID_INSCRIPCION_DIEGO));

        assertEquals(4, ex.getMotivos().size());
        assertNull(e.informes.get(ID_INSCRIPCION_DIEGO), "no debe quedar guardado un informe a medias");
    }

    @Test
    @DisplayName("No genera el informe mientras el profesor supervisor no haya evaluado, si la línea tiene uno")
    void noGeneraSiFaltaLaEvaluacionDelTutor() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();
        e.asignarTutor(e.camila, TipoLinea.ESPECIALIDAD);

        InformeNoGenerableException ex = assertThrows(InformeNoGenerableException.class,
                () -> e.servicio().generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA));

        assertEquals(List.of("Falta la evaluación del profesor supervisor: Especialidad · aplicación 1"), ex.getMotivos());
        assertTrue(e.informes.isEmpty());
    }

    @Test
    @DisplayName("Volver a generar actualiza el mismo informe en vez de crear otro")
    void regenerarReutilizaElInforme() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();
        GeneracionInformeService servicio = e.servicio();

        Long primero = servicio.generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA);
        Long segundo = servicio.generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA);

        assertEquals(primero, segundo);
        assertEquals(1, e.informes.size());
    }

    @Test
    @DisplayName("Al volver a generar toma los datos actuales")
    void regenerarRecalcula() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();
        GeneracionInformeService servicio = e.servicio();
        servicio.generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA);

        // el colaborador sube el criterio 3 de 7 a 10 puntos: pauta 1 pasa de 24 a 27 de 30
        e.cambiarPuntaje(e.evaluaciones.get(0), e.c3, "10");
        servicio.generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA);

        // pauta 1: 27/30 = 90 % -> 6,25; promedio = (6,25x50 + 6,25x50 + 4,75x30 + 5,875x30 + 6,25x40) / 200
        // = 1193,75 / 200 = 5,96875 -> 6,0
        assertNumero("6.0", e.informes.get(ID_INSCRIPCION_CAMILA).getCalificacionSugerida());
    }

    @Test
    @DisplayName("Un informe ya validado o publicado no se puede volver a generar")
    void noRegeneraUnInformeCerrado() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();
        GeneracionInformeService servicio = e.servicio();
        servicio.generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA);
        InformeFinal informe = e.informes.get(ID_INSCRIPCION_CAMILA);
        BigDecimal sugeridaOriginal = informe.getCalificacionSugerida();

        informe.setFechaValidacion(LocalDateTime.now());
        assertThrows(InformeNoGenerableException.class, () -> servicio.generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA));

        informe.setFechaValidacion(null);
        informe.setFechaPublicacion(LocalDateTime.now());
        assertThrows(InformeNoGenerableException.class, () -> servicio.generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA));

        assertSame(sugeridaOriginal, informe.getCalificacionSugerida(), "no debe modificarse un informe cerrado");
    }

    @Test
    @DisplayName("No genera el informe de una inscripción retirada")
    void noGeneraSiEstaRetirada() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();
        e.camila.setEstado(EstadoInscripcion.RETIRADO);

        InformeNoGenerableException ex = assertThrows(InformeNoGenerableException.class,
                () -> e.servicio().generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA));

        assertTrue(ex.getMotivos().get(0).contains("retirada"));
        assertTrue(e.informes.isEmpty());
    }

    @Test
    @DisplayName("Un profesor que no dicta la sección recibe 403 y no se genera nada")
    void profesorAjenoNoPuedeGenerar() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> e.servicio().generar(OTRO_PROFESOR, ID_INSCRIPCION_CAMILA));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(e.informes.isEmpty());
    }

    @Test
    @DisplayName("Una inscripción que no existe da 404")
    void inscripcionInexistente() {
        EscenarioInforme e = new EscenarioInforme();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> e.servicio().generar(ID_PROFESOR, 777L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ------------------------------------------------------------------ lista del profesor

    @Test
    @DisplayName("La lista indica quién está listo para generar y quién tiene datos pendientes")
    void listaConEstados() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();
        e.diegoSoloEspecialidad();

        List<EstudianteInformeResponse> filas = e.servicio().listarEstudiantes(ID_PROFESOR);

        assertEquals(2, filas.size());
        EstudianteInformeResponse camila = filas.get(0);
        assertEquals("Camila Soto", camila.nombreEstudiante());
        assertEquals(Estado.LISTO, camila.estado());
        assertTrue(camila.puedeGenerar());
        assertFalse(camila.tieneInforme());

        EstudianteInformeResponse diego = filas.get(1);
        assertEquals(Estado.PENDIENTE, diego.estado());
        assertFalse(diego.puedeGenerar());
        assertEquals(4, diego.pendientes().size());
    }

    @Test
    @DisplayName("La lista sigue el ciclo del informe: generado, validado, publicado")
    void listaSigueElCicloDelInforme() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();
        GeneracionInformeService servicio = e.servicio();
        servicio.generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA);
        InformeFinal informe = e.informes.get(ID_INSCRIPCION_CAMILA);

        EstudianteInformeResponse generado = servicio.listarEstudiantes(ID_PROFESOR).get(0);
        assertEquals(Estado.GENERADO, generado.estado());
        assertNumero("5.8", generado.calificacionSugerida());
        assertTrue(generado.tieneInforme());
        assertFalse(generado.puedeGenerar());

        informe.setFechaValidacion(LocalDateTime.now());
        assertEquals(Estado.VALIDADO, servicio.listarEstudiantes(ID_PROFESOR).get(0).estado());

        informe.setFechaPublicacion(LocalDateTime.now());
        assertEquals(Estado.PUBLICADO, servicio.listarEstudiantes(ID_PROFESOR).get(0).estado());
    }

    // ------------------------------------------------------------------ detalle del profesor

    @Test
    @DisplayName("El detalle trae la nota sugerida y el desglose que la explica")
    void detalleConDesglose() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();
        GeneracionInformeService servicio = e.servicio();
        Long idInforme = servicio.generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA);

        InformeProfesorResponse detalle = servicio.obtenerParaProfesor(ID_PROFESOR, idInforme);

        assertEquals("Camila Soto", detalle.nombreEstudiante());
        assertNumero("5.8", detalle.calificacionSugerida());
        assertTrue(detalle.desglose().completo());
        assertFalse(detalle.desactualizado());
        assertTrue(detalle.puedeRegenerar());
    }

    @Test
    @DisplayName("Otro profesor no puede ver el informe (403) y uno inexistente da 404")
    void detalleControlDeAcceso() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();
        GeneracionInformeService servicio = e.servicio();
        Long idInforme = servicio.generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA);

        ResponseStatusException ajeno = assertThrows(ResponseStatusException.class,
                () -> servicio.obtenerParaProfesor(OTRO_PROFESOR, idInforme));
        ResponseStatusException inexistente = assertThrows(ResponseStatusException.class,
                () -> servicio.obtenerParaProfesor(ID_PROFESOR, 12345L));

        assertEquals(HttpStatus.FORBIDDEN, ajeno.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, inexistente.getStatusCode());
    }

    @Test
    @DisplayName("Si una evaluación cambia después de generar, el informe se marca desactualizado")
    void detectaInformeDesactualizado() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();
        GeneracionInformeService servicio = e.servicio();
        Long idInforme = servicio.generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA);

        e.cambiarPuntaje(e.evaluaciones.get(0), e.c3, "10");

        assertTrue(servicio.obtenerParaProfesor(ID_PROFESOR, idInforme).desactualizado());

        servicio.generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA);
        assertFalse(servicio.obtenerParaProfesor(ID_PROFESOR, idInforme).desactualizado());
    }

    @Test
    @DisplayName("Un cambio posterior a la generación se detecta aunque la nota no cambie")
    void detectaCambioPorFecha() {
        EscenarioInforme e = new EscenarioInforme();
        e.camilaCompleta();
        GeneracionInformeService servicio = e.servicio();
        Long idInforme = servicio.generar(ID_PROFESOR, ID_INSCRIPCION_CAMILA);

        // el colaborador re-guarda la pauta (cambia su fecha) con los mismos puntajes
        e.evaluaciones.get(0).setFecha(LocalDateTime.now().plusMinutes(5));

        assertTrue(servicio.obtenerParaProfesor(ID_PROFESOR, idInforme).desactualizado());
    }
}
