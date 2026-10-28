package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.DetalleEvaluacionRequest;
import com.grupo1.isw.practicas_pedagogicas.dto.FormularioEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;
import com.grupo1.isw.practicas_pedagogicas.models.Criterio;
import com.grupo1.isw.practicas_pedagogicas.models.DetalleCriterio;
import com.grupo1.isw.practicas_pedagogicas.models.Dimension;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorTutor;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.TipoEvaluador;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.repositories.DetalleCriterioRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionAplicadaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import com.grupo1.isw.practicas_pedagogicas.services.EvaluacionInvalidaException;
import com.grupo1.isw.practicas_pedagogicas.services.EvaluacionMapper;
import com.grupo1.isw.practicas_pedagogicas.services.EvaluacionService;
import com.grupo1.isw.practicas_pedagogicas.services.LineaConsultaService;
import com.grupo1.isw.practicas_pedagogicas.services.PautaCargador;
import com.grupo1.isw.practicas_pedagogicas.services.PautaCargador.DimensionConCriterios;
import com.grupo1.isw.practicas_pedagogicas.services.ValidadorEvaluacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HU-03: el profesor supervisor (tutor) llena la pauta en el sistema.
 * El control de acceso a la línea se prueba en {@link LineaConsultaServiceTutorTest};
 * aquí se prueba qué se guarda y cómo.
 */
@ExtendWith(MockitoExtension.class)
class EvaluacionServiceTutorTest {

    private static final Long ID_TUTOR = 6L;
    private static final Long ID_LINEA = 1L;
    private static final Long ID_PAUTA = 100L;
    private static final Long ID_CRITERIO_1 = 11L;
    private static final Long ID_CRITERIO_2 = 12L;
    private static final Long ID_EVALUACION_EXISTENTE = 500L;

    @Mock private LineaConsultaService lineaConsulta;
    @Mock private PautaEvaluacionRepository pautaRepository;
    @Mock private EvaluacionAplicadaRepository evaluacionRepository;
    @Mock private DetalleCriterioRepository detalleRepository;
    @Mock private PautaCargador cargador;

    @Captor private ArgumentCaptor<List<DetalleCriterio>> detallesCaptor;

    private EvaluacionService service;

    private LineaPractica linea;
    private PautaEvaluacion pauta;
    private Dimension dimension;
    private Criterio criterio1;
    private Criterio criterio2;

    @BeforeEach
    void preparar() {
        service = new EvaluacionService(lineaConsulta, pautaRepository, evaluacionRepository,
                detalleRepository, cargador, new ValidadorEvaluacion(), new EvaluacionMapper());

        Asignatura asignatura = new Asignatura("Práctica Pedagógica", Ciclo.FINAL, 2);
        asignatura.setIdAsignatura(1L);

        Estudiante estudiante = new Estudiante();
        estudiante.setNombre("Camila Soto");

        SeccionAsignatura seccion = new SeccionAsignatura(asignatura, null, 2026, 2);
        InscripcionAsignatura inscripcion = new InscripcionAsignatura(estudiante, seccion, null);

        ProfesorTutor tutor = new ProfesorTutor();
        tutor.setIdUsuario(ID_TUTOR);

        linea = new LineaPractica(inscripcion, null, tutor, TipoLinea.ESPECIALIDAD);
        linea.setIdLinea(ID_LINEA);

        pauta = new PautaEvaluacion(asignatura, TipoLinea.ESPECIALIDAD, 1, new BigDecimal("50.00"));
        pauta.setIdPautaEvaluacion(ID_PAUTA);

        dimension = new Dimension(pauta, "Desempeño en aula", 1);
        criterio1 = new Criterio(dimension, "Dominio del contenido", new BigDecimal("4.00"), 1);
        criterio1.setIdCriterio(ID_CRITERIO_1);
        criterio2 = new Criterio(dimension, "Gestión del tiempo", new BigDecimal("3.00"), 2);
        criterio2.setIdCriterio(ID_CRITERIO_2);
    }

    private void dadoUnaLineaDelTutorYUnaPautaConDosCriterios() {
        when(lineaConsulta.obtenerLineaDelTutor(ID_TUTOR, ID_LINEA)).thenReturn(linea);
        when(pautaRepository.findById(ID_PAUTA)).thenReturn(Optional.of(pauta));
    }

    private void dadoQueLaPautaTieneLosDosCriterios() {
        when(cargador.criteriosDePauta(ID_PAUTA)).thenReturn(List.of(criterio1, criterio2));
    }

    private List<DetalleEvaluacionRequest> detallesValidos() {
        return List.of(
                new DetalleEvaluacionRequest(ID_CRITERIO_1, new BigDecimal("3.50"), "Buen manejo conceptual"),
                new DetalleEvaluacionRequest(ID_CRITERIO_2, new BigDecimal("3.00"), null));
    }

    private EvaluacionAplicada evaluacionExistenteDelTutor() {
        EvaluacionAplicada existente = new EvaluacionAplicada(
                linea, pauta, TipoEvaluador.TUTOR, LocalDateTime.now().minusDays(1));
        existente.setIdEvaluacion(ID_EVALUACION_EXISTENTE);
        existente.setPuntajeTotal(new BigDecimal("2.00"));
        return existente;
    }

    // ---------------------------------------------------------------- guardar

    @Test
    @DisplayName("1. Primera vez: crea la evaluación con evaluador TUTOR y el total calculado")
    void guardar_primeraVez_creaEvaluacionDelTutor() {
        dadoUnaLineaDelTutorYUnaPautaConDosCriterios();
        dadoQueLaPautaTieneLosDosCriterios();
        when(evaluacionRepository.findByLineaYEvaluador(ID_LINEA, TipoEvaluador.TUTOR)).thenReturn(List.of());
        when(evaluacionRepository.save(any(EvaluacionAplicada.class))).thenAnswer(inv -> inv.getArgument(0));

        service.guardarComoTutor(ID_TUTOR, ID_LINEA, ID_PAUTA, detallesValidos());

        ArgumentCaptor<EvaluacionAplicada> captor = ArgumentCaptor.forClass(EvaluacionAplicada.class);
        verify(evaluacionRepository).save(captor.capture());
        EvaluacionAplicada guardada = captor.getValue();
        assertEquals(TipoEvaluador.TUTOR, guardada.getTipoEvaluador());
        assertEquals(0, new BigDecimal("6.50").compareTo(guardada.getPuntajeTotal()));
        assertEquals(ID_LINEA, guardada.getLinea().getIdLinea());
        assertEquals(ID_PAUTA, guardada.getPautaEvaluacion().getIdPautaEvaluacion());

        verify(detalleRepository).saveAll(detallesCaptor.capture());
        List<DetalleCriterio> detalles = detallesCaptor.getValue();
        assertEquals(2, detalles.size());
        assertEquals("Buen manejo conceptual", detalles.get(0).getTranscripcionComentario());
        assertNull(detalles.get(1).getTranscripcionComentario());
    }

    @Test
    @DisplayName("2. Si el tutor ya evaluó esa pauta, actualiza la misma evaluación (no crea otra)")
    void guardar_yaEvaluada_actualizaLaExistente() {
        dadoUnaLineaDelTutorYUnaPautaConDosCriterios();
        dadoQueLaPautaTieneLosDosCriterios();
        EvaluacionAplicada existente = evaluacionExistenteDelTutor();
        DetalleCriterio detalle1 = new DetalleCriterio(existente, criterio1, new BigDecimal("1.00"));
        DetalleCriterio detalle2 = new DetalleCriterio(existente, criterio2, new BigDecimal("1.00"));
        when(evaluacionRepository.findByLineaYEvaluador(ID_LINEA, TipoEvaluador.TUTOR))
                .thenReturn(List.of(existente));
        when(detalleRepository.findByEvaluaciones(List.of(ID_EVALUACION_EXISTENTE)))
                .thenReturn(List.of(detalle1, detalle2));

        service.guardarComoTutor(ID_TUTOR, ID_LINEA, ID_PAUTA, detallesValidos());

        verify(evaluacionRepository, times(1)).save(existente);
        assertEquals(TipoEvaluador.TUTOR, existente.getTipoEvaluador());
        assertEquals(0, new BigDecimal("6.50").compareTo(existente.getPuntajeTotal()));
        assertEquals(0, new BigDecimal("3.50").compareTo(detalle1.getPuntajeObtenido()));
        assertEquals(0, new BigDecimal("3.00").compareTo(detalle2.getPuntajeObtenido()));
        assertEquals("Buen manejo conceptual", detalle1.getTranscripcionComentario());
    }

    @Test
    @DisplayName("3. Puntaje sobre el máximo del criterio: error de validación y no se guarda nada")
    void guardar_puntajeSobreElMaximo_noGuarda() {
        dadoUnaLineaDelTutorYUnaPautaConDosCriterios();
        dadoQueLaPautaTieneLosDosCriterios();
        List<DetalleEvaluacionRequest> detalles = List.of(
                new DetalleEvaluacionRequest(ID_CRITERIO_1, new BigDecimal("4.50"), null),
                new DetalleEvaluacionRequest(ID_CRITERIO_2, new BigDecimal("3.00"), null));

        EvaluacionInvalidaException ex = assertThrows(EvaluacionInvalidaException.class,
                () -> service.guardarComoTutor(ID_TUTOR, ID_LINEA, ID_PAUTA, detalles));

        assertEquals(1, ex.getErrores().size());
        assertTrue(ex.getErrores().get(0).contains("supera el máximo"));
        verify(evaluacionRepository, never()).save(any());
        verify(detalleRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("4. Falta evaluar un criterio: error de validación y no se guarda nada")
    void guardar_faltaUnCriterio_noGuarda() {
        dadoUnaLineaDelTutorYUnaPautaConDosCriterios();
        dadoQueLaPautaTieneLosDosCriterios();
        List<DetalleEvaluacionRequest> soloUno = List.of(
                new DetalleEvaluacionRequest(ID_CRITERIO_1, new BigDecimal("3.50"), null));

        EvaluacionInvalidaException ex = assertThrows(EvaluacionInvalidaException.class,
                () -> service.guardarComoTutor(ID_TUTOR, ID_LINEA, ID_PAUTA, soloUno));

        assertTrue(ex.getErrores().get(0).contains("Faltan criterios por evaluar"));
        verify(evaluacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. Pauta que no corresponde al tipo de línea: lanza 404 y no guarda")
    void guardar_pautaDeOtroTipoDeLinea_lanza404() {
        when(lineaConsulta.obtenerLineaDelTutor(ID_TUTOR, ID_LINEA)).thenReturn(linea);
        pauta.setTipoLinea(TipoLinea.ORIENTACION); // la línea es de ESPECIALIDAD
        when(pautaRepository.findById(ID_PAUTA)).thenReturn(Optional.of(pauta));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.guardarComoTutor(ID_TUTOR, ID_LINEA, ID_PAUTA, detallesValidos()));

        assertEquals(404, ex.getStatusCode().value());
        verify(evaluacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. La pauta no existe: lanza 404 y no guarda")
    void guardar_pautaInexistente_lanza404() {
        when(lineaConsulta.obtenerLineaDelTutor(ID_TUTOR, ID_LINEA)).thenReturn(linea);
        when(pautaRepository.findById(ID_PAUTA)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.guardarComoTutor(ID_TUTOR, ID_LINEA, ID_PAUTA, detallesValidos()));

        assertEquals(404, ex.getStatusCode().value());
        verify(evaluacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Sin permiso sobre la línea: el error se propaga y no se guarda nada")
    void guardar_sinPermisoSobreLaLinea_propagaYNoGuarda() {
        when(lineaConsulta.obtenerLineaDelTutor(ID_TUTOR, ID_LINEA))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "sin permiso"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.guardarComoTutor(ID_TUTOR, ID_LINEA, ID_PAUTA, detallesValidos()));

        assertEquals(403, ex.getStatusCode().value());
        verify(evaluacionRepository, never()).save(any());
        verify(pautaRepository, never()).findById(any());
    }

    // ------------------------------------------------------------- formulario

    @Test
    @DisplayName("8. Formulario sin evaluación previa: modo creación y campos vacíos")
    void formulario_sinEvaluacionPrevia_vacio() {
        dadoUnaLineaDelTutorYUnaPautaConDosCriterios();
        when(evaluacionRepository.findByLineaYEvaluador(ID_LINEA, TipoEvaluador.TUTOR)).thenReturn(List.of());
        when(cargador.cargar(List.of(ID_PAUTA)))
                .thenReturn(Map.of(ID_PAUTA, List.of(new DimensionConCriterios(dimension, List.of(criterio1, criterio2)))));

        FormularioEvaluacion form = service.formularioComoTutor(ID_TUTOR, ID_LINEA, ID_PAUTA, null, null);

        assertFalse(form.edicion());
        assertEquals("Camila Soto", form.nombreEstudiante());
        assertEquals("ESPECIALIDAD", form.tipoLinea());
        assertEquals(1, form.dimensiones().size());
        assertEquals(2, form.dimensiones().get(0).criterios().size());
        assertEquals("", form.dimensiones().get(0).criterios().get(0).puntaje());
    }

    @Test
    @DisplayName("9. Formulario con evaluación previa del tutor: modo edición y valores precargados")
    void formulario_conEvaluacionPrevia_precarga() {
        dadoUnaLineaDelTutorYUnaPautaConDosCriterios();
        EvaluacionAplicada existente = evaluacionExistenteDelTutor();
        DetalleCriterio detalle1 = new DetalleCriterio(existente, criterio1, new BigDecimal("3.50"));
        detalle1.setTranscripcionComentario("Buen manejo conceptual");
        when(evaluacionRepository.findByLineaYEvaluador(ID_LINEA, TipoEvaluador.TUTOR))
                .thenReturn(List.of(existente));
        when(detalleRepository.findByEvaluaciones(List.of(ID_EVALUACION_EXISTENTE)))
                .thenReturn(List.of(detalle1));
        when(cargador.cargar(List.of(ID_PAUTA)))
                .thenReturn(Map.of(ID_PAUTA, List.of(new DimensionConCriterios(dimension, List.of(criterio1, criterio2)))));

        FormularioEvaluacion form = service.formularioComoTutor(ID_TUTOR, ID_LINEA, ID_PAUTA, null, null);

        assertTrue(form.edicion());
        var primero = form.dimensiones().get(0).criterios().get(0);
        assertEquals("3.50", primero.puntaje());
        assertEquals("Buen manejo conceptual", primero.comentario());
        assertEquals("", form.dimensiones().get(0).criterios().get(1).puntaje());
    }

    @Test
    @DisplayName("10. Formulario tras un error: conserva lo que escribió el tutor, no lo guardado")
    void formulario_trasError_conservaLoEnviado() {
        dadoUnaLineaDelTutorYUnaPautaConDosCriterios();
        when(evaluacionRepository.findByLineaYEvaluador(ID_LINEA, TipoEvaluador.TUTOR)).thenReturn(List.of());
        when(cargador.cargar(List.of(ID_PAUTA)))
                .thenReturn(Map.of(ID_PAUTA, List.of(new DimensionConCriterios(dimension, List.of(criterio1, criterio2)))));

        FormularioEvaluacion form = service.formularioComoTutor(ID_TUTOR, ID_LINEA, ID_PAUTA,
                Map.of(ID_CRITERIO_1, "9.99"), Map.of(ID_CRITERIO_1, "Revisar"));

        var primero = form.dimensiones().get(0).criterios().get(0);
        assertEquals("9.99", primero.puntaje());
        assertEquals("Revisar", primero.comentario());
        assertEquals("", form.dimensiones().get(0).criterios().get(1).puntaje());
    }
}
