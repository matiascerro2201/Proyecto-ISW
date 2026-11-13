package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoInscripcion;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorColaborador;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorTutor;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.TipoEvaluador;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionAplicadaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PlanificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.services.MisEstudiantesService;
import com.grupo1.isw.practicas_pedagogicas.services.MisEstudiantesService.EstudianteSupervisado;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** "Mis estudiantes" del tutor y del colaborador. Repositorios falsos, sin base de datos. */
@ExtendWith(MockitoExtension.class)
class MisEstudiantesServiceTest {

    private static final Long ID_TUTOR = 7L;
    private static final Long ID_COLABORADOR = 6L;

    @Mock private LineaPracticaRepository lineaRepository;
    @Mock private PautaEvaluacionRepository pautaRepository;
    @Mock private EvaluacionAplicadaRepository evaluacionRepository;
    @Mock private PlanificacionRepository planificacionRepository;

    private MisEstudiantesService service;
    private Asignatura asignatura;
    private SeccionAsignatura seccion;
    private ProfesorTutor tutor;
    private ProfesorColaborador colaborador;

    @BeforeEach
    void preparar() {
        service = new MisEstudiantesService(lineaRepository, pautaRepository, evaluacionRepository, planificacionRepository);
        asignatura = new Asignatura("Práctica Pedagógica", Ciclo.FINAL, 2);
        asignatura.setIdAsignatura(10L);
        seccion = new SeccionAsignatura(asignatura, null, 2026, 2);
        seccion.setIdSeccion(20L);
        tutor = new ProfesorTutor();
        tutor.setIdUsuario(ID_TUTOR);
        colaborador = new ProfesorColaborador();
        colaborador.setIdUsuario(ID_COLABORADOR);
    }

    private InscripcionAsignatura inscripcion(Long id, String nombre) {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(100L + id);
        estudiante.setNombre(nombre);
        InscripcionAsignatura inscripcion = new InscripcionAsignatura(estudiante, seccion, null);
        inscripcion.setIdInscripcion(id);
        return inscripcion;
    }

    private LineaPractica linea(Long id, InscripcionAsignatura inscripcion, TipoLinea tipo) {
        LineaPractica linea = new LineaPractica(inscripcion, colaborador, tutor, tipo);
        linea.setIdLinea(id);
        return linea;
    }

    private PautaEvaluacion pauta(Long id, TipoLinea tipo, int aplicacion) {
        PautaEvaluacion pauta = new PautaEvaluacion(asignatura, tipo, aplicacion, new BigDecimal("50"));
        pauta.setIdPautaEvaluacion(id);
        return pauta;
    }

    private EvaluacionAplicada evaluacion(LineaPractica linea, PautaEvaluacion pauta, TipoEvaluador quien, String puntaje) {
        EvaluacionAplicada evaluacion = new EvaluacionAplicada(linea, pauta, quien, LocalDateTime.of(2026, 10, 12, 9, 0));
        evaluacion.setPuntajeTotal(new BigDecimal(puntaje));
        return evaluacion;
    }

    // ---------------------------------------------------------------- tutor

    @Test
    @DisplayName("1. Un tutor sin estudiantes recibe una lista vacía")
    void tutorSinLineas() {
        when(lineaRepository.findByTutor(ID_TUTOR)).thenReturn(List.of());

        assertTrue(service.delTutor(ID_TUTOR).isEmpty());
    }

    @Test
    @DisplayName("2. Las líneas de un mismo estudiante van en una sola fila, y las pautas de la asignatura se piden una vez")
    void unaFilaPorEstudiante() {
        InscripcionAsignatura camila = inscripcion(30L, "Camila Soto");
        when(lineaRepository.findByTutor(ID_TUTOR)).thenReturn(List.of(
                linea(11L, camila, TipoLinea.ESPECIALIDAD), linea(12L, camila, TipoLinea.ORIENTACION)));
        when(pautaRepository.findByAsignatura(10L)).thenReturn(List.of(
                pauta(100L, TipoLinea.ESPECIALIDAD, 1), pauta(101L, TipoLinea.ORIENTACION, 1)));

        List<EstudianteSupervisado> estudiantes = service.delTutor(ID_TUTOR);

        assertEquals(1, estudiantes.size());
        EstudianteSupervisado vista = estudiantes.get(0);
        assertEquals(11L, vista.id());
        assertEquals("Camila Soto", vista.nombre());
        assertEquals("Semestre 2 de 2026", vista.periodo());
        assertEquals("11 12", vista.idsDeLineas());
        assertEquals("Especialidad y Orientación", vista.tipos());
        assertEquals(2, vista.totalPautas());
        verify(pautaRepository, times(1)).findByAsignatura(10L);
    }

    @Test
    @DisplayName("3. Marca las pautas que el tutor ya evaluó, las ordena por aplicación y cuenta las pendientes")
    void avanceDeLasPautas() {
        InscripcionAsignatura camila = inscripcion(30L, "Camila Soto");
        LineaPractica linea = linea(11L, camila, TipoLinea.ESPECIALIDAD);
        PautaEvaluacion primera = pauta(100L, TipoLinea.ESPECIALIDAD, 1);
        PautaEvaluacion segunda = pauta(101L, TipoLinea.ESPECIALIDAD, 2);
        when(lineaRepository.findByTutor(ID_TUTOR)).thenReturn(List.of(linea));
        when(pautaRepository.findByAsignatura(10L)).thenReturn(List.of(segunda, primera));
        when(evaluacionRepository.findDelTutorByLinea(11L))
                .thenReturn(List.of(evaluacion(linea, primera, TipoEvaluador.TUTOR, "17.50")));

        EstudianteSupervisado estudiante = service.delTutor(ID_TUTOR).get(0);

        var pautas = estudiante.lineas().get(0).pautas();
        assertEquals(List.of(1, 2), pautas.stream().map(p -> p.numeroAplicacion()).toList());
        assertTrue(pautas.get(0).evaluada());
        assertEquals(new BigDecimal("17.50"), pautas.get(0).puntaje());
        assertEquals(LocalDateTime.of(2026, 10, 12, 9, 0), pautas.get(0).fecha());
        assertFalse(pautas.get(1).evaluada());
        assertNull(pautas.get(1).puntaje());
        assertEquals(1, estudiante.evaluadas());
        assertEquals(1, estudiante.pendientes());
        assertEquals("PENDIENTE", estudiante.estado());
        assertTrue(estudiante.planificaciones().isEmpty());
        verify(planificacionRepository, never()).findByEstadoAndLinea_Colaborador_IdUsuarioOrderByFechaEnvioAsc(
                Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("4. Con todas las pautas evaluadas el estudiante está al día; sin pautas, queda sin pautas")
    void estados() {
        InscripcionAsignatura camila = inscripcion(30L, "Camila Soto");
        InscripcionAsignatura diego = inscripcion(31L, "Diego Pérez");
        LineaPractica deCamila = linea(11L, camila, TipoLinea.ESPECIALIDAD);
        LineaPractica deDiego = linea(12L, diego, TipoLinea.ORIENTACION);
        PautaEvaluacion pauta = pauta(100L, TipoLinea.ESPECIALIDAD, 1);
        when(lineaRepository.findByTutor(ID_TUTOR)).thenReturn(List.of(deCamila, deDiego));
        when(pautaRepository.findByAsignatura(10L)).thenReturn(List.of(pauta));
        when(evaluacionRepository.findDelTutorByLinea(11L))
                .thenReturn(List.of(evaluacion(deCamila, pauta, TipoEvaluador.TUTOR, "20")));

        List<EstudianteSupervisado> estudiantes = service.delTutor(ID_TUTOR);

        assertEquals("AL_DIA", estudiantes.get(0).estado());
        assertEquals("SIN_PAUTAS", estudiantes.get(1).estado());
    }

    @Test
    @DisplayName("5. Solo cuentan las pautas del tipo de la línea")
    void soloPautasDelTipoDeLinea() {
        InscripcionAsignatura camila = inscripcion(30L, "Camila Soto");
        when(lineaRepository.findByTutor(ID_TUTOR)).thenReturn(List.of(linea(11L, camila, TipoLinea.ESPECIALIDAD)));
        when(pautaRepository.findByAsignatura(10L)).thenReturn(List.of(
                pauta(100L, TipoLinea.ESPECIALIDAD, 1), pauta(101L, TipoLinea.ORIENTACION, 1)));

        assertEquals(1, service.delTutor(ID_TUTOR).get(0).totalPautas());
    }

    @Test
    @DisplayName("6. Los estudiantes retirados no aparecen")
    void sinRetirados() {
        InscripcionAsignatura camila = inscripcion(30L, "Camila Soto");
        InscripcionAsignatura retirado = inscripcion(31L, "Diego Pérez");
        retirado.setEstado(EstadoInscripcion.RETIRADO);
        when(lineaRepository.findByTutor(ID_TUTOR)).thenReturn(List.of(
                linea(11L, camila, TipoLinea.ESPECIALIDAD), linea(12L, retirado, TipoLinea.ESPECIALIDAD)));
        when(pautaRepository.findByAsignatura(10L)).thenReturn(List.of());

        List<EstudianteSupervisado> estudiantes = service.delTutor(ID_TUTOR);

        assertEquals(1, estudiantes.size());
        assertEquals("Camila Soto", estudiantes.get(0).nombre());
    }

    // ---------------------------------------------------------------- colaborador

    @Test
    @DisplayName("7. El colaborador ve sus estudiantes con las pautas que él evaluó, no las del tutor")
    void colaboradorUsaSusPropiasEvaluaciones() {
        InscripcionAsignatura camila = inscripcion(30L, "Camila Soto");
        LineaPractica linea = linea(11L, camila, TipoLinea.ESPECIALIDAD);
        PautaEvaluacion pauta = pauta(100L, TipoLinea.ESPECIALIDAD, 1);
        when(lineaRepository.findByColaborador(ID_COLABORADOR)).thenReturn(List.of(linea));
        when(pautaRepository.findByAsignatura(10L)).thenReturn(List.of(pauta));
        when(evaluacionRepository.findDelColaboradorByLinea(11L))
                .thenReturn(List.of(evaluacion(linea, pauta, TipoEvaluador.COLABORADOR, "18")));
        when(planificacionRepository.findByEstadoAndLinea_Colaborador_IdUsuarioOrderByFechaEnvioAsc(
                EstadoPlanificacion.ENVIADA, ID_COLABORADOR)).thenReturn(List.of());

        EstudianteSupervisado estudiante = service.delColaborador(ID_COLABORADOR).get(0);

        assertTrue(estudiante.lineas().get(0).pautas().get(0).evaluada());
        assertEquals("AL_DIA", estudiante.estado());
        verify(evaluacionRepository, never()).findDelTutorByLinea(Mockito.any());
        verify(lineaRepository, never()).findByTutor(Mockito.any());
    }

    @Test
    @DisplayName("8. Las planificaciones enviadas cuentan como pendientes del estudiante, aunque sus pautas estén evaluadas")
    void colaboradorConPlanificacionPorEvaluar() {
        InscripcionAsignatura camila = inscripcion(30L, "Camila Soto");
        InscripcionAsignatura diego = inscripcion(31L, "Diego Pérez");
        LineaPractica deCamila = linea(11L, camila, TipoLinea.ESPECIALIDAD);
        LineaPractica deDiego = linea(12L, diego, TipoLinea.ESPECIALIDAD);
        PautaEvaluacion pauta = pauta(100L, TipoLinea.ESPECIALIDAD, 1);
        when(lineaRepository.findByColaborador(ID_COLABORADOR)).thenReturn(List.of(deCamila, deDiego));
        when(pautaRepository.findByAsignatura(10L)).thenReturn(List.of(pauta));
        when(evaluacionRepository.findDelColaboradorByLinea(11L))
                .thenReturn(List.of(evaluacion(deCamila, pauta, TipoEvaluador.COLABORADOR, "18")));
        when(evaluacionRepository.findDelColaboradorByLinea(12L))
                .thenReturn(List.of(evaluacion(deDiego, pauta, TipoEvaluador.COLABORADOR, "15")));

        Planificacion enviada = Mockito.mock(Planificacion.class);
        when(enviada.getLinea()).thenReturn(deCamila);
        when(enviada.getIdPlanificacion()).thenReturn(77L);
        when(enviada.getFechaEnvio()).thenReturn(LocalDateTime.of(2026, 9, 14, 10, 30));
        when(planificacionRepository.findByEstadoAndLinea_Colaborador_IdUsuarioOrderByFechaEnvioAsc(
                EstadoPlanificacion.ENVIADA, ID_COLABORADOR)).thenReturn(List.of(enviada));

        List<EstudianteSupervisado> estudiantes = service.delColaborador(ID_COLABORADOR);

        EstudianteSupervisado conPlanificacion = estudiantes.get(0);
        assertEquals(1, conPlanificacion.planificaciones().size());
        assertEquals(77L, conPlanificacion.planificaciones().get(0).idPlanificacion());
        assertEquals(LocalDateTime.of(2026, 9, 14, 10, 30), conPlanificacion.planificaciones().get(0).fechaEnvio());
        assertEquals(1, conPlanificacion.pendientes());
        assertEquals("PENDIENTE", conPlanificacion.estado());

        EstudianteSupervisado alDia = estudiantes.get(1);
        assertTrue(alDia.planificaciones().isEmpty());
        assertEquals("AL_DIA", alDia.estado());
    }

    @Test
    @DisplayName("9. Un estudiante sin pautas pero con una planificación por evaluar no queda como \"sin pautas\"")
    void planificacionSinPautas() {
        InscripcionAsignatura camila = inscripcion(30L, "Camila Soto");
        LineaPractica linea = linea(11L, camila, TipoLinea.ESPECIALIDAD);
        when(lineaRepository.findByColaborador(ID_COLABORADOR)).thenReturn(List.of(linea));
        when(pautaRepository.findByAsignatura(10L)).thenReturn(List.of());
        Planificacion enviada = Mockito.mock(Planificacion.class);
        when(enviada.getLinea()).thenReturn(linea);
        when(enviada.getIdPlanificacion()).thenReturn(77L);
        when(enviada.getFechaEnvio()).thenReturn(LocalDateTime.of(2026, 9, 14, 10, 30));
        when(planificacionRepository.findByEstadoAndLinea_Colaborador_IdUsuarioOrderByFechaEnvioAsc(
                EstadoPlanificacion.ENVIADA, ID_COLABORADOR)).thenReturn(List.of(enviada));

        assertEquals("PENDIENTE", service.delColaborador(ID_COLABORADOR).get(0).estado());
    }
}
