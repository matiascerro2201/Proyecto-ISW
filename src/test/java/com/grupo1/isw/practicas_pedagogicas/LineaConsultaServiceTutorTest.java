package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.LineaEvaluacionesResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.LineaResumenResponse;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorColaborador;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorTutor;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.TipoEvaluador;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionAplicadaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import com.grupo1.isw.practicas_pedagogicas.services.LineaConsultaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/** HU-03: acceso y estado de las pautas desde el punto de vista del profesor supervisor (tutor). */
@ExtendWith(MockitoExtension.class)
class LineaConsultaServiceTutorTest {

    private static final Long ID_TUTOR = 6L;
    private static final Long ID_OTRO_TUTOR = 7L;
    private static final Long ID_LINEA = 1L;
    private static final Long ID_PAUTA_1 = 100L;
    private static final Long ID_PAUTA_2 = 101L;

    @Mock private LineaPracticaRepository lineaRepository;
    @Mock private EvaluacionAplicadaRepository evaluacionRepository;
    @Mock private PautaEvaluacionRepository pautaRepository;

    private LineaConsultaService service;

    private Asignatura asignatura;
    private LineaPractica linea;

    @BeforeEach
    void preparar() {
        service = new LineaConsultaService(lineaRepository, evaluacionRepository, pautaRepository);

        asignatura = new Asignatura("Práctica Pedagógica", Ciclo.FINAL, 2);
        asignatura.setIdAsignatura(1L);

        Estudiante estudiante = new Estudiante();
        estudiante.setNombre("Camila Soto");
        SeccionAsignatura seccion = new SeccionAsignatura(asignatura, null, 2026, 2);
        InscripcionAsignatura inscripcion = new InscripcionAsignatura(estudiante, seccion, null);

        linea = new LineaPractica(inscripcion, null, tutor(ID_TUTOR), TipoLinea.ESPECIALIDAD);
        linea.setIdLinea(ID_LINEA);
    }

    private static ProfesorTutor tutor(Long id) {
        ProfesorTutor tutor = new ProfesorTutor();
        tutor.setIdUsuario(id);
        return tutor;
    }

    private PautaEvaluacion pauta(Long id, int aplicacion) {
        PautaEvaluacion pauta = new PautaEvaluacion(asignatura, TipoLinea.ESPECIALIDAD, aplicacion, new BigDecimal("50.00"));
        pauta.setIdPautaEvaluacion(id);
        return pauta;
    }

    // ------------------------------------------------------- acceso a la línea

    @Test
    @DisplayName("1. La línea no existe: 404")
    void obtenerLineaDelTutor_inexistente_lanza404() {
        when(lineaRepository.findByIdConContexto(ID_LINEA)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerLineaDelTutor(ID_TUTOR, ID_LINEA));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("2. La línea es de otro tutor: 403")
    void obtenerLineaDelTutor_deOtroTutor_lanza403() {
        when(lineaRepository.findByIdConContexto(ID_LINEA)).thenReturn(Optional.of(linea));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerLineaDelTutor(ID_OTRO_TUTOR, ID_LINEA));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("3. La línea no tiene tutor asignado: 403 (sin NullPointerException)")
    void obtenerLineaDelTutor_sinTutor_lanza403() {
        linea.setTutor(null);
        when(lineaRepository.findByIdConContexto(ID_LINEA)).thenReturn(Optional.of(linea));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerLineaDelTutor(ID_TUTOR, ID_LINEA));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("4. La línea es del tutor: la devuelve")
    void obtenerLineaDelTutor_propia_laDevuelve() {
        when(lineaRepository.findByIdConContexto(ID_LINEA)).thenReturn(Optional.of(linea));

        assertSame(linea, service.obtenerLineaDelTutor(ID_TUTOR, ID_LINEA));
    }

    // ---------------------------------------------------------------- listado

    @Test
    @DisplayName("5. El listado trae las líneas que supervisa el tutor, con el contexto del estudiante")
    void listarLineasDelTutor_devuelveResumen() {
        when(lineaRepository.findByTutor(ID_TUTOR)).thenReturn(List.of(linea));

        List<LineaResumenResponse> lineas = service.listarLineasDelTutor(ID_TUTOR);

        assertEquals(1, lineas.size());
        LineaResumenResponse resumen = lineas.get(0);
        assertEquals(ID_LINEA, resumen.idLinea());
        assertEquals("Camila Soto", resumen.nombreEstudiante());
        assertEquals("Práctica Pedagógica", resumen.nombreAsignatura());
        assertEquals(2026, resumen.anio());
        assertEquals(2, resumen.semestre());
        assertEquals("ESPECIALIDAD", resumen.tipoLinea());
    }

    @Test
    @DisplayName("6. Un tutor sin líneas asignadas recibe una lista vacía")
    void listarLineasDelTutor_sinLineas_listaVacia() {
        when(lineaRepository.findByTutor(ID_OTRO_TUTOR)).thenReturn(List.of());

        assertEquals(List.of(), service.listarLineasDelTutor(ID_OTRO_TUTOR));
    }

    // ---------------------------------------------------------------- detalle

    @Test
    @DisplayName("7. El detalle marca como evaluadas solo las pautas que completó el tutor")
    void detalleLineaDelTutor_marcaSoloLasEvaluadasPorElTutor() {
        PautaEvaluacion pauta1 = pauta(ID_PAUTA_1, 1);
        PautaEvaluacion pauta2 = pauta(ID_PAUTA_2, 2);
        EvaluacionAplicada delTutor = new EvaluacionAplicada(
                linea, pauta1, TipoEvaluador.TUTOR, LocalDateTime.of(2026, 10, 20, 9, 30));
        delTutor.setIdEvaluacion(500L);
        delTutor.setPuntajeTotal(new BigDecimal("6.50"));

        when(lineaRepository.findByIdConContexto(ID_LINEA)).thenReturn(Optional.of(linea));
        when(evaluacionRepository.findByLineaYEvaluador(ID_LINEA, TipoEvaluador.TUTOR)).thenReturn(List.of(delTutor));
        when(pautaRepository.findByAsignatura(1L)).thenReturn(List.of(pauta1, pauta2));

        LineaEvaluacionesResponse detalle = service.detalleLineaDelTutor(ID_TUTOR, ID_LINEA);

        assertEquals("Camila Soto", detalle.nombreEstudiante());
        assertEquals(2, detalle.pautas().size());

        var primera = detalle.pautas().get(0);
        assertEquals(ID_PAUTA_1, primera.idPautaEvaluacion());
        assertEquals(Long.valueOf(500L), primera.idEvaluacion());
        assertEquals(0, new BigDecimal("6.50").compareTo(primera.puntajeTotal()));
        assertNotNull(primera.fecha());

        var segunda = detalle.pautas().get(1);
        assertEquals(ID_PAUTA_2, segunda.idPautaEvaluacion());
        assertNull(segunda.idEvaluacion());
        assertNull(segunda.puntajeTotal());
    }

    @Test
    @DisplayName("8. El detalle no se entrega a un tutor que no supervisa la línea")
    void detalleLineaDelTutor_deOtroTutor_lanza403() {
        when(lineaRepository.findByIdConContexto(ID_LINEA)).thenReturn(Optional.of(linea));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.detalleLineaDelTutor(ID_OTRO_TUTOR, ID_LINEA));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("9. El detalle del colaborador sigue consultando solo las evaluaciones del colaborador (HU-02 intacta)")
    void detalleLinea_delColaborador_usaEvaluadorColaborador() {
        ProfesorColaborador colaborador = new ProfesorColaborador();
        colaborador.setIdUsuario(1L);
        linea.setColaborador(colaborador);
        PautaEvaluacion pauta1 = pauta(ID_PAUTA_1, 1);

        when(lineaRepository.findByIdConContexto(ID_LINEA)).thenReturn(Optional.of(linea));
        when(evaluacionRepository.findByLineaYEvaluador(ID_LINEA, TipoEvaluador.COLABORADOR)).thenReturn(List.of());
        when(pautaRepository.findByAsignatura(1L)).thenReturn(List.of(pauta1));

        LineaEvaluacionesResponse detalle = service.detalleLinea(1L, ID_LINEA);

        assertEquals(1, detalle.pautas().size());
        assertNull(detalle.pautas().get(0).idEvaluacion());
    }
}
