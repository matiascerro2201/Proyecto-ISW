package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.PlazoRevisionResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.inicio.InicioResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.inicio.PendienteInicio;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorColaborador;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.TipoEvaluador;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.repositories.DocumentoEstudianteRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionAplicadaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.InscripcionAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaDocumentoRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PlanificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.services.InicioService;
import com.grupo1.isw.practicas_pedagogicas.services.PlazoRevisionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/** Cursos y pendientes de la página de inicio. Repositorios y servicios falsos, sin base de datos. */
@ExtendWith(MockitoExtension.class)
class InicioServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 28);

    @Mock private InscripcionAsignaturaRepository inscripcionRepository;
    @Mock private LineaPracticaRepository lineaRepository;
    @Mock private PlanificacionRepository planificacionRepository;
    @Mock private PautaDocumentoRepository pautaDocumentoRepository;
    @Mock private DocumentoEstudianteRepository documentoRepository;
    @Mock private PautaEvaluacionRepository pautaEvaluacionRepository;
    @Mock private EvaluacionAplicadaRepository evaluacionRepository;
    @Mock private PlazoRevisionService plazoRevisionService;

    private InicioService service;

    @BeforeEach
    void preparar() {
        service = new InicioService(inscripcionRepository, lineaRepository, planificacionRepository,
                pautaDocumentoRepository, documentoRepository, pautaEvaluacionRepository,
                evaluacionRepository, plazoRevisionService);
    }

    @Test
    @DisplayName("1. La sigla toma las iniciales de las palabras principales, hasta tres")
    void sigla() {
        assertEquals("PP", InicioService.sigla("Práctica Pedagógica"));
        assertEquals("PEF", InicioService.sigla("Práctica de Especialidad Final"));
        assertEquals("PPI", InicioService.sigla("Práctica Pedagógica Inicial Avanzada"));
        assertEquals("?", InicioService.sigla("   "));
    }

    @Test
    @DisplayName("2. El rol se muestra con su nombre en español")
    void etiquetaRol() {
        assertEquals("Estudiante", InicioService.etiquetaRol("ESTUDIANTE"));
        assertEquals("Profesor colaborador", InicioService.etiquetaRol("COLABORADOR"));
        assertEquals("Profesor supervisor", InicioService.etiquetaRol("TUTOR"));
        assertEquals("Profesor de asignatura", InicioService.etiquetaRol("PROFESOR_ASIGNATURA"));
        assertEquals("Administrador", InicioService.etiquetaRol("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("3. Profesor de asignatura: un curso por sección y pendientes con fecha primero; sin pendientes no aparece")
    void profesorDeAsignatura() {
        PlazoRevisionResponse sinFecha =
                PlazoRevisionResponse.de(2L, "Práctica Final", 2026, 2, null, 1, 4, HOY);
        PlazoRevisionResponse vencida =
                PlazoRevisionResponse.de(1L, "Práctica Inicial", 2026, 2, HOY.minusDays(3), 2, 5, HOY);
        PlazoRevisionResponse completa =
                PlazoRevisionResponse.de(3L, "Práctica Profesional", 2026, 2, HOY.plusDays(20), 0, 3, HOY);
        when(plazoRevisionService.listarPlazos(3L)).thenReturn(List.of(sinFecha, vencida, completa));

        InicioResponse inicio = service.armar(3L, "PROFESOR_ASIGNATURA");

        assertEquals(3, inicio.cursos().size());
        assertEquals("Práctica Final", inicio.cursos().get(0).nombre());
        assertEquals("Semestre 2 de 2026", inicio.cursos().get(0).periodo());
        assertEquals("4 estudiantes", inicio.cursos().get(0).detalle());

        assertEquals(2, inicio.pendientes().size(), "la sección con la revisión completa no es un pendiente");
        PendienteInicio primera = inicio.pendientes().get(0);
        assertEquals(HOY.minusDays(3), primera.fecha());
        assertTrue(primera.vencida());
        assertEquals("Venció hace 3 días", primera.etiqueta());
        assertEquals("rechazada", primera.claseEstado());
        assertEquals("2 de 5 por revisar", primera.detalle());

        PendienteInicio segunda = inicio.pendientes().get(1);
        assertNull(segunda.fecha(), "las actividades sin fecha van después de las que tienen");
        assertFalse(segunda.vencida());
        assertEquals("Revisar informes finales de Práctica Final", segunda.titulo());
    }

    @Test
    @DisplayName("4. Colaborador: pendiente cada pauta de la línea que todavía no evaluó")
    void colaboradorConUnaPautaPendiente() {
        Estudiante camila = new Estudiante();
        camila.setIdUsuario(4L);
        camila.setNombre("Camila Soto");
        Asignatura asignatura = new Asignatura("Práctica Pedagógica", Ciclo.FINAL, 2);
        asignatura.setIdAsignatura(10L);
        SeccionAsignatura seccion = new SeccionAsignatura(asignatura, null, 2026, 2);
        seccion.setIdSeccion(20L);
        InscripcionAsignatura inscripcion = new InscripcionAsignatura(camila, seccion, null);
        inscripcion.setIdInscripcion(30L);
        ProfesorColaborador colaborador = new ProfesorColaborador();
        colaborador.setIdUsuario(6L);
        LineaPractica linea = new LineaPractica(inscripcion, colaborador, null, TipoLinea.ESPECIALIDAD);
        linea.setIdLinea(1L);

        PautaEvaluacion primera = new PautaEvaluacion(asignatura, TipoLinea.ESPECIALIDAD, 1, new BigDecimal("50"));
        primera.setIdPautaEvaluacion(100L);
        PautaEvaluacion segunda = new PautaEvaluacion(asignatura, TipoLinea.ESPECIALIDAD, 2, new BigDecimal("50"));
        segunda.setIdPautaEvaluacion(101L);
        EvaluacionAplicada yaHecha =
                new EvaluacionAplicada(linea, primera, TipoEvaluador.COLABORADOR, LocalDateTime.of(2026, 10, 1, 9, 0));

        when(lineaRepository.findByColaborador(6L)).thenReturn(List.of(linea));
        when(pautaEvaluacionRepository.findByAsignatura(10L)).thenReturn(List.of(primera, segunda));
        when(evaluacionRepository.findDelColaboradorByLinea(1L)).thenReturn(List.of(yaHecha));

        InicioResponse inicio = service.armar(6L, "COLABORADOR");

        assertEquals(1, inicio.cursos().size());
        assertEquals("1 estudiante", inicio.cursos().get(0).detalle());
        assertEquals("/colaborador/lineas", inicio.cursos().get(0).enlace());

        assertEquals(1, inicio.pendientes().size());
        PendienteInicio pendiente = inicio.pendientes().get(0);
        assertEquals("Evaluar la pauta 2 de Camila Soto", pendiente.titulo());
        assertEquals("/colaborador/lineas/1", pendiente.enlace());
        assertEquals("Práctica Pedagógica", pendiente.detalle());
        assertNull(pendiente.fecha());
    }
}
