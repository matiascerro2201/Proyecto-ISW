package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.InscripcionFormulario;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.LineaFormulario;
import com.grupo1.isw.practicas_pedagogicas.repositories.InscripcionAdminRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.InscripcionAdminRepository.InscripcionFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository.SeccionDatos;
import com.grupo1.isw.practicas_pedagogicas.services.CoordinadorInscripcionService;
import com.grupo1.isw.practicas_pedagogicas.services.EstructuraInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoordinadorInscripcionServiceTest {
    private static final long ID_SECCION = 1L;
    private static final long ESTUDIANTE = 20L;
    private static final long CENTRO = 30L;
    private static final long COLABORADOR = 40L;
    private static final long TUTOR = 50L;

    @Mock
    InscripcionAdminRepository inscripciones;
    @Mock
    SeccionEstructuraRepository secciones;

    private CoordinadorInscripcionService servicio;

    @BeforeEach
    void preparar() {
        servicio = new CoordinadorInscripcionService(inscripciones, secciones);
        when(secciones.buscar(ID_SECCION)).thenReturn(Optional.of(seccion(2)));
        when(inscripciones.existePersonaActiva(ESTUDIANTE, "ESTUDIANTE")).thenReturn(true);
        when(inscripciones.existePersonaActiva(COLABORADOR, "COLABORADOR")).thenReturn(true);
        when(inscripciones.existePersonaActiva(TUTOR, "TUTOR")).thenReturn(true);
        when(inscripciones.existeCentro(CENTRO)).thenReturn(true);
        when(inscripciones.insertarInscripcion(ESTUDIANTE, ID_SECCION, CENTRO)).thenReturn(90L);
    }

    private static SeccionDatos seccion(int numeroLineas) {
        return new SeccionDatos(ID_SECCION, 10L, "Práctica Pedagógica", numeroLineas, 2026, 1, 5L, "Profesor",
                false, null);
    }

    private static InscripcionFormulario formulario(LineaFormulario... lineas) {
        InscripcionFormulario f = new InscripcionFormulario();
        f.setIdEstudiante(ESTUDIANTE);
        f.setIdCentro(CENTRO);
        f.setLineas(List.of(lineas));
        return f;
    }

    @Test
    @DisplayName("1. Cada asignatura exige una línea (única) o dos (especialidad y orientación)")
    void tiposDeLinea() {
        assertEquals(List.of("UNICA"), CoordinadorInscripcionService.tiposDeLinea(1));
        assertEquals(List.of("ESPECIALIDAD", "ORIENTACION"), CoordinadorInscripcionService.tiposDeLinea(2));
    }

    @Test
    @DisplayName("2. El formulario vacío trae una fila por línea de la asignatura")
    void formularioVacio() {
        InscripcionFormulario formulario = servicio.formularioVacio(ID_SECCION);

        assertEquals(2, formulario.getLineas().size());
        assertEquals("ESPECIALIDAD", formulario.getLineas().get(0).getTipo());
    }

    @Test
    @DisplayName("3. Inscribe al estudiante y crea sus dos líneas, una con tutor y otra sin")
    void inscribe() {
        long id = servicio.inscribir(ID_SECCION, formulario(
                new LineaFormulario("ESPECIALIDAD", COLABORADOR, TUTOR),
                new LineaFormulario("ORIENTACION", COLABORADOR, null)));

        assertEquals(90L, id);
        verify(inscripciones).guardarLinea(90L, "ESPECIALIDAD", COLABORADOR, TUTOR);
        verify(inscripciones).guardarLinea(90L, "ORIENTACION", COLABORADOR, null);
    }

    @Test
    @DisplayName("4. El colaborador es opcional: se inscribe con líneas sin colaborador")
    void colaboradorOpcional() {
        long id = servicio.inscribir(ID_SECCION, formulario(
                new LineaFormulario("ESPECIALIDAD", COLABORADOR, null),
                new LineaFormulario("ORIENTACION", null, null)));

        assertEquals(90L, id);
        verify(inscripciones).guardarLinea(90L, "ESPECIALIDAD", COLABORADOR, null);
        verify(inscripciones).guardarLinea(90L, "ORIENTACION", null, null);
    }

    @Test
    @DisplayName("5. No se inscribe dos veces al mismo estudiante en la sección")
    void yaInscrito() {
        when(inscripciones.estaInscrito(ESTUDIANTE, ID_SECCION)).thenReturn(true);

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> servicio.inscribir(ID_SECCION, formulario(
                        new LineaFormulario("ESPECIALIDAD", COLABORADOR, null),
                        new LineaFormulario("ORIENTACION", COLABORADOR, null))));

        assertTrue(ex.getErrores().get(0).contains("ya está inscrito"));
    }

    @Test
    @DisplayName("6. Estudiante, centro, colaborador y tutor deben existir y estar activos")
    void personasInexistentes() {
        when(inscripciones.existePersonaActiva(ESTUDIANTE, "ESTUDIANTE")).thenReturn(false);
        when(inscripciones.existeCentro(CENTRO)).thenReturn(false);
        when(inscripciones.existePersonaActiva(TUTOR, "TUTOR")).thenReturn(false);

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> servicio.inscribir(ID_SECCION, formulario(
                        new LineaFormulario("ESPECIALIDAD", COLABORADOR, TUTOR),
                        new LineaFormulario("ORIENTACION", COLABORADOR, null))));

        assertEquals(3, ex.getErrores().size());
    }

    @Test
    @DisplayName("6b. Si se elige un colaborador, debe existir y estar activo")
    void colaboradorElegidoDebeEstarActivo() {
        when(inscripciones.existePersonaActiva(COLABORADOR, "COLABORADOR")).thenReturn(false);

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> servicio.inscribir(ID_SECCION, formulario(
                        new LineaFormulario("ESPECIALIDAD", COLABORADOR, null),
                        new LineaFormulario("ORIENTACION", null, null))));

        assertEquals(1, ex.getErrores().size());
        assertTrue(ex.getErrores().get(0).contains("Especialidad"));
        verify(inscripciones, never()).insertarInscripcion(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("7. En una asignatura de una línea solo se guarda la línea única")
    void ignoraLineasQueSobran() {
        when(secciones.buscar(ID_SECCION)).thenReturn(Optional.of(seccion(1)));

        servicio.inscribir(ID_SECCION, formulario(
                new LineaFormulario("UNICA", COLABORADOR, null),
                new LineaFormulario("ORIENTACION", COLABORADOR, null)));

        verify(inscripciones).guardarLinea(90L, "UNICA", COLABORADOR, null);
        verify(inscripciones, never()).guardarLinea(anyLong(), org.mockito.ArgumentMatchers.eq("ORIENTACION"),
                org.mockito.ArgumentMatchers.<Long>any(), org.mockito.ArgumentMatchers.<Long>any());
    }

    @Test
    @DisplayName("8. Editar una inscripción cambia el centro y los profesores")
    void actualiza() {
        when(inscripciones.buscar(90L)).thenReturn(Optional.of(new InscripcionFila(90L, ID_SECCION, ESTUDIANTE,
                "Estudiante", "e@ubb.cl", CENTRO, "Centro", "EN_CURSO", List.of())));

        servicio.actualizar(ID_SECCION, 90L, formulario(
                new LineaFormulario("ESPECIALIDAD", COLABORADOR, TUTOR),
                new LineaFormulario("ORIENTACION", COLABORADOR, TUTOR)));

        verify(inscripciones).cambiarCentro(90L, CENTRO);
        verify(inscripciones).guardarLinea(90L, "ORIENTACION", COLABORADOR, TUTOR);
    }

    @Test
    @DisplayName("9. Una inscripción de otra sección se trata como inexistente")
    void inscripcionAjena() {
        when(inscripciones.buscar(90L)).thenReturn(Optional.of(new InscripcionFila(90L, 999L, ESTUDIANTE,
                "Estudiante", "e@ubb.cl", CENTRO, "Centro", "EN_CURSO", List.of())));

        assertThrows(ResponseStatusException.class, () -> servicio.quitar(ID_SECCION, 90L));
        assertThrows(ResponseStatusException.class, () -> servicio.formularioDe(ID_SECCION, 90L));
    }

    @Test
    @DisplayName("10. No se quita a un estudiante que ya tiene entregas o evaluaciones")
    void noQuitaConDatos() {
        when(inscripciones.buscar(90L)).thenReturn(Optional.of(new InscripcionFila(90L, ID_SECCION, ESTUDIANTE,
                "Estudiante", "e@ubb.cl", CENTRO, "Centro", "EN_CURSO", List.of())));
        when(inscripciones.tieneDatosEvaluativos(90L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> servicio.quitar(ID_SECCION, 90L));

        verify(inscripciones, never()).eliminar(anyLong());
    }

    @Test
    @DisplayName("11. Se quita a un estudiante sin datos")
    void quita() {
        when(inscripciones.buscar(90L)).thenReturn(Optional.of(new InscripcionFila(90L, ID_SECCION, ESTUDIANTE,
                "Estudiante", "e@ubb.cl", CENTRO, "Centro", "EN_CURSO", List.of())));

        servicio.quitar(ID_SECCION, 90L);

        verify(inscripciones).eliminar(90L);
    }

    @Test
    @DisplayName("12. El formulario de edición trae el centro y los profesores actuales")
    void formularioDeEdicion() {
        when(inscripciones.buscar(90L)).thenReturn(Optional.of(new InscripcionFila(90L, ID_SECCION, ESTUDIANTE,
                "Estudiante", "e@ubb.cl", CENTRO, "Centro", "EN_CURSO",
                List.of(new InscripcionAdminRepository.LineaFila(1L, "ESPECIALIDAD", COLABORADOR, "Colab", TUTOR, "Tutor")))));

        InscripcionFormulario formulario = servicio.formularioDe(ID_SECCION, 90L);

        assertEquals(Long.valueOf(CENTRO), formulario.getIdCentro());
        assertEquals(Long.valueOf(TUTOR), formulario.getLineas().get(0).getIdTutor());
        assertNull(formulario.getLineas().get(1).getIdColaborador());
    }
}
