package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.AsignaturaFormulario;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.SeccionFormulario;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.RolUsuario;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.repositories.AsignaturaAdminRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.ProfesorAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.services.CoordinadorAsignaturaService;
import com.grupo1.isw.practicas_pedagogicas.services.EstructuraInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * N-01: reglas de asignaturas y secciones del coordinador. Repositorios falsos, sin base de datos.
 * El reloj queda fijo en noviembre de 2026, así que el rango de años permitido es 2021 a 2031.
 */
@ExtendWith(MockitoExtension.class)
class CoordinadorAsignaturaServiceTest {

    private static final Long ID_ASIGNATURA = 1L;
    private static final Long ID_SECCION = 20L;
    private static final Long ID_PROFESOR = 3L;
    private static final Long ID_OTRO_PROFESOR = 4L;

    @Mock private AsignaturaAdminRepository asignaturaRepository;
    @Mock private SeccionAsignaturaRepository seccionRepository;
    @Mock private ProfesorAsignaturaRepository profesorRepository;

    private CoordinadorAsignaturaService service;
    private Asignatura asignatura;
    private ProfesorAsignatura profesor;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-22T12:00:00Z"), ZoneId.of("America/Santiago"));
        service = new CoordinadorAsignaturaService(asignaturaRepository, seccionRepository, profesorRepository, reloj);

        asignatura = new Asignatura("Práctica Pedagógica", Ciclo.FINAL, 2);
        asignatura.setIdAsignatura(ID_ASIGNATURA);
        profesor = profesor(ID_PROFESOR, true, RolUsuario.PROFESOR_ASIGNATURA);

        // Comportamiento por defecto: nada repetido, sin inscritos ni secciones, todo existe.
        // Cada prueba cambia solo lo que necesita.
        lenient().when(asignaturaRepository.findByNombreIgnoreCase(anyString())).thenReturn(Optional.empty());
        lenient().when(asignaturaRepository.existeSeccion(any(), any(), any(), any())).thenReturn(false);
        lenient().when(asignaturaRepository.contarInscripcionesDeAsignatura(any())).thenReturn(0L);
        lenient().when(asignaturaRepository.contarInscripcionesDeSeccion(any())).thenReturn(0L);
        lenient().when(asignaturaRepository.contarSecciones(any())).thenReturn(0L);
        lenient().when(asignaturaRepository.findById(ID_ASIGNATURA)).thenReturn(Optional.of(asignatura));
        lenient().when(profesorRepository.findById(ID_PROFESOR)).thenReturn(Optional.of(profesor));
        lenient().when(asignaturaRepository.save(any(Asignatura.class))).thenAnswer(inv -> {
            Asignatura guardada = inv.getArgument(0);
            if (guardada.getIdAsignatura() == null) {
                guardada.setIdAsignatura(99L);
            }
            return guardada;
        });
        lenient().when(seccionRepository.save(any(SeccionAsignatura.class))).thenAnswer(inv -> {
            SeccionAsignatura guardada = inv.getArgument(0);
            if (guardada.getIdSeccion() == null) {
                guardada.setIdSeccion(88L);
            }
            return guardada;
        });
    }

    // ------------------------------------------------------------------ ayudas

    private static ProfesorAsignatura profesor(Long id, boolean activo, RolUsuario rol) {
        ProfesorAsignatura p = new ProfesorAsignatura();
        p.setIdUsuario(id);
        p.setNombre("Profesor " + id);
        p.setRol(rol);
        p.setActivo(activo);
        return p;
    }

    private static AsignaturaFormulario formularioAsignatura(String nombre, Ciclo ciclo, Integer lineas) {
        AsignaturaFormulario f = new AsignaturaFormulario();
        f.setNombre(nombre);
        f.setCiclo(ciclo);
        f.setNumeroLineas(lineas);
        return f;
    }

    private static SeccionFormulario formularioSeccion(Long idAsignatura, Integer anio, Integer semestre,
                                                      Long idProfesor) {
        SeccionFormulario f = new SeccionFormulario();
        f.setIdAsignatura(idAsignatura);
        f.setAnio(anio);
        f.setSemestre(semestre);
        f.setIdProfesor(idProfesor);
        return f;
    }

    /** Una sección ya guardada de la asignatura de la prueba, dentro de la colección de esta. */
    private SeccionAsignatura seccionGuardada(int anio, int semestre) {
        SeccionAsignatura seccion = new SeccionAsignatura(asignatura, profesor, anio, semestre);
        seccion.setIdSeccion(ID_SECCION);
        asignatura.getSecciones().add(seccion);
        when(asignaturaRepository.findSeccionConDetalle(ID_SECCION)).thenReturn(Optional.of(seccion));
        return seccion;
    }

    private static boolean contiene(EstructuraInvalidaException ex, String texto) {
        return ex.getErrores().stream().anyMatch(e -> e.contains(texto));
    }

    // =========================================================== asignaturas

    @Test
    @DisplayName("1. Crear una asignatura válida la guarda con el nombre sin espacios sobrantes")
    void crearAsignatura_valida_guardaConNombreLimpio() {
        Long id = service.crearAsignatura(formularioAsignatura("  Práctica Intermedia  ", Ciclo.INTERMEDIO, 1));

        ArgumentCaptor<Asignatura> guardada = ArgumentCaptor.forClass(Asignatura.class);
        verify(asignaturaRepository).save(guardada.capture());
        assertEquals("Práctica Intermedia", guardada.getValue().getNombre());
        assertEquals(Ciclo.INTERMEDIO, guardada.getValue().getCiclo());
        assertEquals(1, guardada.getValue().getNumeroLineas());
        assertEquals(99L, id);
    }

    @Test
    @DisplayName("2. Los errores de la asignatura vienen todos juntos y no se guarda nada")
    void crearAsignatura_conVariosErrores_losInformaTodosJuntos() {
        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> service.crearAsignatura(formularioAsignatura("   ", null, 3)));

        assertEquals(3, ex.getErrores().size());
        assertTrue(contiene(ex, "El nombre de la asignatura es obligatorio"));
        assertTrue(contiene(ex, "Elige el ciclo"));
        assertTrue(contiene(ex, "El número de líneas debe ser 1 o 2"));
        verify(asignaturaRepository, never()).save(any(Asignatura.class));
    }

    @Test
    @DisplayName("3. Un nombre repetido se rechaza aunque cambien mayúsculas o tildes")
    void crearAsignatura_nombreRepetido_seRechaza() {
        when(asignaturaRepository.findByNombreIgnoreCase("practica pedagogica")).thenReturn(Optional.of(asignatura));

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> service.crearAsignatura(formularioAsignatura("practica pedagogica", Ciclo.FINAL, 2)));

        assertTrue(contiene(ex, "Ya existe una asignatura con el nombre"));
        verify(asignaturaRepository, never()).save(any(Asignatura.class));
    }

    @Test
    @DisplayName("4. Un nombre de más de 150 caracteres se rechaza")
    void crearAsignatura_nombreMuyLargo_seRechaza() {
        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> service.crearAsignatura(formularioAsignatura("A".repeat(151), Ciclo.FINAL, 1)));

        assertTrue(contiene(ex, "150 caracteres"));
    }

    @Test
    @DisplayName("5. Sin número de líneas se rechaza")
    void crearAsignatura_sinLineas_seRechaza() {
        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> service.crearAsignatura(formularioAsignatura("Nueva", Ciclo.INICIAL, null)));

        assertTrue(contiene(ex, "Indica el número de líneas"));
    }

    @Test
    @DisplayName("6. Al editar, conservar el propio nombre no cuenta como repetido")
    void actualizarAsignatura_mismoNombre_noEsRepetido() {
        when(asignaturaRepository.findByNombreIgnoreCase("Práctica Pedagógica")).thenReturn(Optional.of(asignatura));

        service.actualizarAsignatura(ID_ASIGNATURA, formularioAsignatura("Práctica Pedagógica", Ciclo.INTERMEDIO, 2));

        verify(asignaturaRepository).save(asignatura);
        assertEquals(Ciclo.INTERMEDIO, asignatura.getCiclo());
    }

    @Test
    @DisplayName("7. Con estudiantes inscritos no se puede cambiar el número de líneas")
    void actualizarAsignatura_cambiarLineasConInscritos_seRechaza() {
        when(asignaturaRepository.contarInscripcionesDeAsignatura(ID_ASIGNATURA)).thenReturn(5L);

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> service.actualizarAsignatura(ID_ASIGNATURA,
                        formularioAsignatura("Práctica Pedagógica", Ciclo.FINAL, 1)));

        assertTrue(contiene(ex, "No se puede cambiar el número de líneas"));
        assertEquals(2, asignatura.getNumeroLineas());
        verify(asignaturaRepository, never()).save(any(Asignatura.class));
    }

    @Test
    @DisplayName("8. Sin estudiantes inscritos sí se puede cambiar el número de líneas")
    void actualizarAsignatura_cambiarLineasSinInscritos_seGuarda() {
        service.actualizarAsignatura(ID_ASIGNATURA, formularioAsignatura("Práctica Pedagógica", Ciclo.FINAL, 1));

        assertEquals(1, asignatura.getNumeroLineas());
        verify(asignaturaRepository).save(asignatura);
    }

    @Test
    @DisplayName("9. Con inscritos sí se pueden cambiar el nombre y el ciclo si las líneas no cambian")
    void actualizarAsignatura_nombreYCicloConInscritos_seGuarda() {
        service.actualizarAsignatura(ID_ASIGNATURA, formularioAsignatura("Práctica Final", Ciclo.INTERMEDIO, 2));

        assertEquals("Práctica Final", asignatura.getNombre());
        assertEquals(Ciclo.INTERMEDIO, asignatura.getCiclo());
        verify(asignaturaRepository).save(asignatura);
    }

    @Test
    @DisplayName("10. Editar una asignatura que no existe: 404")
    void actualizarAsignatura_inexistente_lanza404() {
        when(asignaturaRepository.findById(77L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.actualizarAsignatura(77L, formularioAsignatura("X", Ciclo.FINAL, 1)));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("11. No se puede eliminar una asignatura que tiene secciones")
    void eliminarAsignatura_conSecciones_seRechaza() {
        when(asignaturaRepository.contarSecciones(ID_ASIGNATURA)).thenReturn(3L);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.eliminarAsignatura(ID_ASIGNATURA));

        assertTrue(ex.getMessage().contains("3 secciones"));
        verify(asignaturaRepository, never()).delete(any(Asignatura.class));
    }

    @Test
    @DisplayName("12. Una asignatura sin secciones se elimina")
    void eliminarAsignatura_sinSecciones_seElimina() {
        service.eliminarAsignatura(ID_ASIGNATURA);

        verify(asignaturaRepository).delete(asignatura);
    }

    // ============================================================= secciones

    @Test
    @DisplayName("13. Crear una sección válida la guarda con su asignatura, período y profesor")
    void crearSeccion_valida_laGuarda() {
        Long id = service.crearSeccion(formularioSeccion(ID_ASIGNATURA, 2026, 1, ID_PROFESOR));

        ArgumentCaptor<SeccionAsignatura> guardada = ArgumentCaptor.forClass(SeccionAsignatura.class);
        verify(seccionRepository).save(guardada.capture());
        assertEquals(asignatura, guardada.getValue().getAsignatura());
        assertEquals(profesor, guardada.getValue().getProfesor());
        assertEquals(2026, guardada.getValue().getAnio());
        assertEquals(1, guardada.getValue().getSemestre());
        assertEquals(88L, id);
    }

    @Test
    @DisplayName("14. Los errores de la sección vienen todos juntos y no se guarda nada")
    void crearSeccion_conVariosErrores_losInformaTodosJuntos() {
        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> service.crearSeccion(formularioSeccion(null, 2040, 3, null)));

        assertEquals(4, ex.getErrores().size());
        assertTrue(contiene(ex, "Elige la asignatura"));
        assertTrue(contiene(ex, "El año debe estar entre 2021 y 2031"));
        assertTrue(contiene(ex, "El semestre debe ser 1 o 2"));
        assertTrue(contiene(ex, "Elige el profesor"));
        verify(seccionRepository, never()).save(any(SeccionAsignatura.class));
    }

    @ParameterizedTest(name = "El año {0} está fuera del rango y se rechaza")
    @ValueSource(ints = {2020, 2032})
    @DisplayName("15. Un año a más de 5 años del actual se rechaza")
    void crearSeccion_anioFueraDeRango_seRechaza(int anio) {
        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> service.crearSeccion(formularioSeccion(ID_ASIGNATURA, anio, 1, ID_PROFESOR)));

        assertTrue(contiene(ex, "El año debe estar entre 2021 y 2031"));
    }

    @ParameterizedTest(name = "El año {0} está en el borde del rango y se acepta")
    @ValueSource(ints = {2021, 2031})
    @DisplayName("16. Los años en el borde del rango se aceptan")
    void crearSeccion_anioEnElBorde_seAcepta(int anio) {
        service.crearSeccion(formularioSeccion(ID_ASIGNATURA, anio, 1, ID_PROFESOR));

        verify(seccionRepository).save(any(SeccionAsignatura.class));
    }

    @Test
    @DisplayName("17. Ya existe una sección de esa asignatura en ese año y semestre")
    void crearSeccion_duplicada_seRechaza() {
        when(asignaturaRepository.existeSeccion(ID_ASIGNATURA, 2026, 2, -1L)).thenReturn(true);

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> service.crearSeccion(formularioSeccion(ID_ASIGNATURA, 2026, 2, ID_PROFESOR)));

        assertTrue(contiene(ex, "Ya existe una sección de Práctica Pedagógica para el semestre 2 de 2026"));
        verify(seccionRepository, never()).save(any(SeccionAsignatura.class));
    }

    @Test
    @DisplayName("18. Un profesor que no existe se rechaza")
    void crearSeccion_profesorInexistente_seRechaza() {
        when(profesorRepository.findById(ID_OTRO_PROFESOR)).thenReturn(Optional.empty());

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> service.crearSeccion(formularioSeccion(ID_ASIGNATURA, 2026, 1, ID_OTRO_PROFESOR)));

        assertTrue(contiene(ex, "no existe o no es profesor de asignatura"));
    }

    @Test
    @DisplayName("19. Un profesor con la cuenta desactivada se rechaza")
    void crearSeccion_profesorDesactivado_seRechaza() {
        when(profesorRepository.findById(ID_OTRO_PROFESOR))
                .thenReturn(Optional.of(profesor(ID_OTRO_PROFESOR, false, RolUsuario.PROFESOR_ASIGNATURA)));

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> service.crearSeccion(formularioSeccion(ID_ASIGNATURA, 2026, 1, ID_OTRO_PROFESOR)));

        assertTrue(contiene(ex, "desactivada"));
    }

    @Test
    @DisplayName("20. Un usuario que no tiene el rol de profesor de asignatura se rechaza")
    void crearSeccion_profesorConOtroRol_seRechaza() {
        when(profesorRepository.findById(ID_OTRO_PROFESOR))
                .thenReturn(Optional.of(profesor(ID_OTRO_PROFESOR, true, RolUsuario.COLABORADOR)));

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> service.crearSeccion(formularioSeccion(ID_ASIGNATURA, 2026, 1, ID_OTRO_PROFESOR)));

        assertTrue(contiene(ex, "no es profesor de asignatura"));
    }

    @Test
    @DisplayName("21. Con inscritos se puede cambiar el profesor sin tocar el período")
    void actualizarSeccion_cambiarProfesorConInscritos_seGuarda() {
        SeccionAsignatura seccion = seccionGuardada(2026, 2);
        ProfesorAsignatura nuevo = profesor(ID_OTRO_PROFESOR, true, RolUsuario.PROFESOR_ASIGNATURA);
        when(profesorRepository.findById(ID_OTRO_PROFESOR)).thenReturn(Optional.of(nuevo));

        service.actualizarSeccion(ID_SECCION, formularioSeccion(ID_ASIGNATURA, 2026, 2, ID_OTRO_PROFESOR));

        assertEquals(nuevo, seccion.getProfesor());
        verify(seccionRepository).save(seccion);
        verify(asignaturaRepository, never()).contarInscripcionesDeSeccion(any());
    }

    @Test
    @DisplayName("22. Con inscritos no se puede cambiar el año ni el semestre")
    void actualizarSeccion_cambiarPeriodoConInscritos_seRechaza() {
        SeccionAsignatura seccion = seccionGuardada(2026, 2);
        when(asignaturaRepository.contarInscripcionesDeSeccion(ID_SECCION)).thenReturn(2L);

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> service.actualizarSeccion(ID_SECCION, formularioSeccion(ID_ASIGNATURA, 2027, 2, ID_PROFESOR)));

        assertTrue(contiene(ex, "No se puede cambiar la asignatura, el año ni el semestre"));
        assertEquals(2026, seccion.getAnio());
        verify(seccionRepository, never()).save(any(SeccionAsignatura.class));
    }

    @Test
    @DisplayName("23. Sin inscritos se puede cambiar el período de la sección")
    void actualizarSeccion_cambiarPeriodoSinInscritos_seGuarda() {
        SeccionAsignatura seccion = seccionGuardada(2026, 2);

        service.actualizarSeccion(ID_SECCION, formularioSeccion(ID_ASIGNATURA, 2027, 1, ID_PROFESOR));

        assertEquals(2027, seccion.getAnio());
        assertEquals(1, seccion.getSemestre());
        verify(seccionRepository).save(seccion);
    }

    @Test
    @DisplayName("24. Cambiar a un período que ya tiene sección se rechaza, excluyendo la propia")
    void actualizarSeccion_cambiarAPeriodoOcupado_seRechaza() {
        seccionGuardada(2026, 2);
        when(asignaturaRepository.existeSeccion(ID_ASIGNATURA, 2027, 1, ID_SECCION)).thenReturn(true);

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> service.actualizarSeccion(ID_SECCION, formularioSeccion(ID_ASIGNATURA, 2027, 1, ID_PROFESOR)));

        assertTrue(contiene(ex, "Ya existe una sección de"));
    }

    @Test
    @DisplayName("25. Una sección antigua conserva su año fuera de rango al cambiarle el profesor")
    void actualizarSeccion_anioAntiguoSinCambios_noSeRevisaElRango() {
        SeccionAsignatura seccion = seccionGuardada(2018, 2);
        ProfesorAsignatura nuevo = profesor(ID_OTRO_PROFESOR, true, RolUsuario.PROFESOR_ASIGNATURA);
        when(profesorRepository.findById(ID_OTRO_PROFESOR)).thenReturn(Optional.of(nuevo));

        service.actualizarSeccion(ID_SECCION, formularioSeccion(ID_ASIGNATURA, 2018, 2, ID_OTRO_PROFESOR));

        assertEquals(2018, seccion.getAnio());
        assertEquals(nuevo, seccion.getProfesor());
        verify(seccionRepository).save(seccion);
    }

    @Test
    @DisplayName("26. Cambiar una sección antigua a otro año fuera de rango sí se rechaza")
    void actualizarSeccion_anioAntiguoCambiadoAOtroFueraDeRango_seRechaza() {
        seccionGuardada(2018, 2);

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> service.actualizarSeccion(ID_SECCION, formularioSeccion(ID_ASIGNATURA, 2019, 2, ID_PROFESOR)));

        assertTrue(contiene(ex, "El año debe estar entre 2021 y 2031"));
    }

    @Test
    @DisplayName("27. Editar una sección que no existe: 404")
    void actualizarSeccion_inexistente_lanza404() {
        when(asignaturaRepository.findSeccionConDetalle(77L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.actualizarSeccion(77L, formularioSeccion(ID_ASIGNATURA, 2026, 1, ID_PROFESOR)));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("28. No se puede eliminar una sección con estudiantes inscritos")
    void eliminarSeccion_conInscritos_seRechaza() {
        SeccionAsignatura seccion = seccionGuardada(2026, 2);
        when(asignaturaRepository.contarInscripcionesDeSeccion(ID_SECCION)).thenReturn(1L);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.eliminarSeccion(ID_SECCION));

        assertTrue(ex.getMessage().contains("1 estudiante inscrito"));
        verify(seccionRepository, never()).delete(any(SeccionAsignatura.class));
        assertTrue(asignatura.getSecciones().contains(seccion));
    }

    @Test
    @DisplayName("29. Una sección sin inscritos se elimina y sale de la colección de su asignatura")
    void eliminarSeccion_sinInscritos_seElimina() {
        SeccionAsignatura seccion = seccionGuardada(2026, 2);

        service.eliminarSeccion(ID_SECCION);

        verify(seccionRepository).delete(seccion);
        assertFalse(asignatura.getSecciones().contains(seccion));
    }

    // ================================================================ listados

    @Test
    @DisplayName("30. El listado de asignaturas informa cuántas secciones e inscritos tiene cada una")
    void listarAsignaturas_calculaConteos() {
        Asignatura otra = new Asignatura("Práctica Inicial", Ciclo.INICIAL, 1);
        otra.setIdAsignatura(2L);
        when(asignaturaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(otra, asignatura));
        when(asignaturaRepository.contarSeccionesPorAsignatura())
                .thenReturn(List.<Object[]>of(new Object[]{ID_ASIGNATURA, 2L}));
        when(asignaturaRepository.contarInscripcionesPorAsignatura())
                .thenReturn(List.<Object[]>of(new Object[]{ID_ASIGNATURA, 7L}));

        var filas = service.listarAsignaturas();

        assertEquals(2, filas.size());
        assertTrue(filas.get(0).puedeEliminar());
        assertTrue(filas.get(0).puedeCambiarLineas());
        assertEquals(2L, filas.get(1).secciones());
        assertEquals(7L, filas.get(1).inscripciones());
        assertFalse(filas.get(1).puedeEliminar());
        assertFalse(filas.get(1).puedeCambiarLineas());
    }

    @Test
    @DisplayName("31. El rango de años que ofrece el formulario sale del reloj")
    void rangoDeAnios_sigueAlReloj() {
        assertEquals(2021, service.anioMinimo());
        assertEquals(2031, service.anioMaximo());
    }
}
