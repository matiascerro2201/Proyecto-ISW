package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorColaborador;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PlanificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.services.NotificacionService;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionService;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionService.PlanificacionVista;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HU-26: ciclo de vida con BORRADOR. Cubre crear/guardar/enviar borradores, que un borrador sea
 * invisible para el colaborador y el orden del listado del estudiante.
 */
@ExtendWith(MockitoExtension.class)
class PlanificacionBorradorServiceTest {

    private static final Long ID_PLANIFICACION = 10L;
    private static final Long ID_PLANIFICACION_NUEVA = 77L;
    private static final Long ID_PROFESOR = 1L;
    private static final Long ID_OTRO_PROFESOR = 2L;
    private static final Long ID_ESTUDIANTE = 4L;
    private static final Long ID_OTRO_ESTUDIANTE = 5L;
    private static final Long ID_LINEA = 7L;
    private static final LocalDateTime FECHA_ENVIO_ORIGINAL = LocalDateTime.of(2026, 10, 1, 9, 0);

    @Mock
    private PlanificacionRepository planificacionRepository;

    @Mock
    private LineaPracticaRepository lineaPracticaRepository;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private PlanificacionService service;

    // ------------------------------------------------------------------ ayudas

    private LineaPractica lineaDe(Long idEstudiante, Long idProfesor) {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(idEstudiante);
        estudiante.setNombre("Camila Soto");
        Asignatura asignatura = new Asignatura();
        asignatura.setNombre("Práctica Pedagógica");
        ProfesorAsignatura profesor = new ProfesorAsignatura();
        profesor.setIdUsuario(idProfesor);
        InscripcionAsignatura inscripcion =
                new InscripcionAsignatura(estudiante, new SeccionAsignatura(asignatura, profesor, 2026, 2), null);
        ProfesorColaborador colaborador = new ProfesorColaborador();
        colaborador.setIdUsuario(99L); // el colaborador ya no revisa planificaciones
        return new LineaPractica(inscripcion, colaborador, null, TipoLinea.ESPECIALIDAD);
    }

    /** Un borrador no tiene fecha de envío; cualquier otro estado conserva la fecha de su envío. */
    private Planificacion planificacion(EstadoPlanificacion estado, Long idEstudiante, Long idProfesor) {
        LineaPractica linea = lineaDe(idEstudiante, idProfesor);
        Planificacion planificacion = estado == EstadoPlanificacion.BORRADOR
                ? Planificacion.borrador(linea, "Plan de clase")
                : new Planificacion(linea, "Plan de clase", FECHA_ENVIO_ORIGINAL);
        planificacion.setIdPlanificacion(ID_PLANIFICACION);
        planificacion.setEstado(estado);
        return planificacion;
    }

    private Planificacion planificacion(EstadoPlanificacion estado) {
        return planificacion(estado, ID_ESTUDIANTE, ID_PROFESOR);
    }

    private Planificacion conIdNuevo(Planificacion guardada) {
        guardada.setIdPlanificacion(ID_PLANIFICACION_NUEVA);
        return guardada;
    }

    /** HU-31/32: la planificación tiene su PDF subido (lo que exige el envío). */
    private Planificacion conArchivo(Planificacion planificacion) {
        planificacion.adjuntarArchivo("archivo.pdf", "plan.pdf", 1234L, "Texto extraído del PDF",
                LocalDateTime.of(2026, 10, 1, 9, 0));
        return planificacion;
    }

    private Planificacion borradorConArchivo() {
        return conArchivo(planificacion(EstadoPlanificacion.BORRADOR));
    }

    private void dadoQueExiste(Planificacion planificacion) {
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.of(planificacion));
    }

    private void verificarQueNoSeNotifico() {
        verify(notificacionService, never()).crear(any(), any(), any(), any());
    }

    private void verificarQueNoSeGuardoNiSeNotifico() {
        verify(planificacionRepository, never()).save(any());
        verificarQueNoSeNotifico();
    }

    // ----------------------------------------------------------- enviarBorrador

    @Test
    @DisplayName("HU-31/32. Enviar un borrador con PDF lo pasa a ENVIADA con fecha y notifica al profesor")
    void enviarBorrador_pasaAEnviadaYNotifica() {
        Planificacion borrador = borradorConArchivo();
        dadoQueExiste(borrador);
        LocalDateTime antes = LocalDateTime.now();

        service.enviarBorrador(ID_ESTUDIANTE, ID_PLANIFICACION, PlanificacionService.fechaMinimaRevision());

        assertEquals(EstadoPlanificacion.ENVIADA, borrador.getEstado());
        assertTrue(borrador.tieneArchivo(), "el PDF debe seguir adjunto");
        assertFalse(borrador.getFechaEnvio().isBefore(antes), "la fecha de envío debe ser la del momento del envío");

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).crear(eq(ID_PROFESOR), eq(NotificacionService.TIPO_PLANIFICACION),
                mensaje.capture(), eq("/planificaciones/" + ID_PLANIFICACION + "/evaluar"));
        assertTrue(mensaje.getValue().contains("Camila Soto envió"), mensaje.getValue());

        // Primero se guarda y solo después se avisa: si falla el guardado no hay aviso.
        InOrder orden = inOrder(planificacionRepository, notificacionService);
        orden.verify(planificacionRepository).save(borrador);
        orden.verify(notificacionService).crear(any(), any(), any(), any());
    }

    @Test
    @DisplayName("HU-31/32. Enviar un borrador sin PDF lanza IllegalArgument: sigue siendo borrador y no se notifica")
    void enviarBorrador_sinArchivo_sigueSiendoBorrador() {
        Planificacion borrador = planificacion(EstadoPlanificacion.BORRADOR);
        dadoQueExiste(borrador);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.enviarBorrador(ID_ESTUDIANTE, ID_PLANIFICACION, PlanificacionService.fechaMinimaRevision()));

        assertTrue(ex.getMessage().contains("PDF"), ex.getMessage());
        assertEquals(EstadoPlanificacion.BORRADOR, borrador.getEstado());
        assertNull(borrador.getFechaEnvio());
        verificarQueNoSeGuardoNiSeNotifico();
    }

    @ParameterizedTest(name = "HU-26. Solo se envía un borrador: no se puede enviar una {0}")
    @EnumSource(value = EstadoPlanificacion.class, names = {"ENVIADA", "APROBADA", "RECHAZADA"})
    void enviarBorrador_noEsBorrador_lanzaIllegalState(EstadoPlanificacion estado) {
        Planificacion planificacion = conArchivo(planificacion(estado));
        dadoQueExiste(planificacion);

        assertThrows(IllegalStateException.class,
                () -> service.enviarBorrador(ID_ESTUDIANTE, ID_PLANIFICACION, PlanificacionService.fechaMinimaRevision()));

        assertEquals(estado, planificacion.getEstado());
        verificarQueNoSeGuardoNiSeNotifico();
    }

    @Test
    @DisplayName("HU-26. Enviar el borrador de otro estudiante lanza 403 y no notifica")
    void enviarBorrador_ajeno_lanza403() {
        Planificacion ajeno = conArchivo(
                planificacion(EstadoPlanificacion.BORRADOR, ID_OTRO_ESTUDIANTE, ID_PROFESOR));
        dadoQueExiste(ajeno);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.enviarBorrador(ID_ESTUDIANTE, ID_PLANIFICACION, PlanificacionService.fechaMinimaRevision()));

        assertEquals(403, ex.getStatusCode().value());
        assertEquals(EstadoPlanificacion.BORRADOR, ajeno.getEstado());
        verificarQueNoSeGuardoNiSeNotifico();
    }

    @Test
    @DisplayName("HU-26. Enviar una planificación que no existe lanza 404")
    void enviarBorrador_noExiste_lanza404() {
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.enviarBorrador(ID_ESTUDIANTE, ID_PLANIFICACION, PlanificacionService.fechaMinimaRevision()));

        assertEquals(404, ex.getStatusCode().value());
        verificarQueNoSeGuardoNiSeNotifico();
    }

    @Test
    @DisplayName("HU-26. Si falla el guardado al enviar no se notifica al profesor")
    void enviarBorrador_falloAlGuardar_noNotifica() {
        dadoQueExiste(borradorConArchivo());
        when(planificacionRepository.save(any(Planificacion.class)))
                .thenThrow(new RuntimeException("Falla de base de datos"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.enviarBorrador(ID_ESTUDIANTE, ID_PLANIFICACION, PlanificacionService.fechaMinimaRevision()));

        assertEquals("Falla de base de datos", ex.getMessage());
        verificarQueNoSeNotifico();
    }

    @Test
    @DisplayName("HU-26. Enviar dos veces (dos pestañas) deja una sola notificación")
    void enviarBorrador_dosVeces_notificaUnaSolaVez() {
        dadoQueExiste(borradorConArchivo());

        service.enviarBorrador(ID_ESTUDIANTE, ID_PLANIFICACION, PlanificacionService.fechaMinimaRevision());
        assertThrows(IllegalStateException.class,
                () -> service.enviarBorrador(ID_ESTUDIANTE, ID_PLANIFICACION, PlanificacionService.fechaMinimaRevision()));

        verify(notificacionService, times(1)).crear(any(), any(), any(), any());
    }

    // ------------------------- un borrador es privado del estudiante (ítem 5b)

    @Test
    @DisplayName("HU-26. El colaborador de la línea no puede ver un borrador: 404")
    void colaborador_noVeUnBorrador_lanza404() {
        dadoQueExiste(planificacion(EstadoPlanificacion.BORRADOR));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerPlanificacionParaColaborador(ID_PLANIFICACION, ID_PROFESOR));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("HU-26. Un colaborador ajeno tampoco se entera de que existe un borrador: 404, no 403")
    void colaboradorAjeno_noVeUnBorrador_lanza404() {
        dadoQueExiste(planificacion(EstadoPlanificacion.BORRADOR));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerPlanificacionParaColaborador(ID_PLANIFICACION, ID_OTRO_PROFESOR));

        assertEquals(404, ex.getStatusCode().value());
    }

    @ParameterizedTest(name = "HU-26. El colaborador sí ve una planificación {0}")
    @EnumSource(value = EstadoPlanificacion.class, names = {"ENVIADA", "APROBADA", "RECHAZADA"})
    void colaborador_veLasPlanificacionesEnviadas(EstadoPlanificacion estado) {
        Planificacion planificacion = planificacion(estado);
        dadoQueExiste(planificacion);

        assertSame(planificacion, service.obtenerPlanificacionParaColaborador(ID_PLANIFICACION, ID_PROFESOR));
    }

    @Test
    @DisplayName("HU-26. Una planificación enviada de otra línea sigue dando 403 al colaborador ajeno")
    void colaboradorAjeno_planificacionEnviada_lanza403() {
        dadoQueExiste(planificacion(EstadoPlanificacion.ENVIADA));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerPlanificacionParaColaborador(ID_PLANIFICACION, ID_OTRO_PROFESOR));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("HU-26. Pedir como colaborador una planificación que no existe lanza 404")
    void colaborador_planificacionInexistente_lanza404() {
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerPlanificacionParaColaborador(ID_PLANIFICACION, ID_PROFESOR));

        assertEquals(404, ex.getStatusCode().value());
    }

    @ParameterizedTest(name = "HU-26. No se puede evaluar un borrador ({0}): 404 y no queda nada guardado")
    @EnumSource(value = EstadoPlanificacion.class, names = {"APROBADA", "RECHAZADA"})
    void evaluar_unBorrador_lanza404(EstadoPlanificacion decision) {
        Planificacion borrador = planificacion(EstadoPlanificacion.BORRADOR);
        dadoQueExiste(borrador);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.evaluarPlanificacion(ID_PLANIFICACION, ID_PROFESOR, "Comentario", decision));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals(EstadoPlanificacion.BORRADOR, borrador.getEstado());
        assertNull(borrador.getRetroalimentacion());
        verificarQueNoSeGuardoNiSeNotifico();
    }

    // ------------------------------------------------------ listado y vista

    @Test
    @DisplayName("HU-26. El listado pone primero los borradores (el más reciente arriba) y luego las enviadas")
    void listar_borradoresPrimero() {
        Planificacion enviadaReciente = planificacion(EstadoPlanificacion.ENVIADA);
        enviadaReciente.setIdPlanificacion(1L);
        Planificacion enviadaAntigua = planificacion(EstadoPlanificacion.ENVIADA);
        enviadaAntigua.setIdPlanificacion(2L);
        Planificacion borradorViejo = planificacion(EstadoPlanificacion.BORRADOR);
        borradorViejo.setIdPlanificacion(3L);
        Planificacion borradorNuevo = planificacion(EstadoPlanificacion.BORRADOR);
        borradorNuevo.setIdPlanificacion(4L);
        // El orden que entrega la base: por fecha de envío descendente, con los borradores (sin fecha) al final.
        when(planificacionRepository.findByLinea_Inscripcion_Estudiante_IdUsuarioOrderByFechaEnvioDesc(ID_ESTUDIANTE))
                .thenReturn(List.of(enviadaReciente, enviadaAntigua, borradorViejo, borradorNuevo));

        List<PlanificacionVista> vistas = service.listarPlanificacionesDelEstudiante(ID_ESTUDIANTE);

        assertEquals(List.of(4L, 3L, 1L, 2L), vistas.stream().map(PlanificacionVista::idPlanificacion).toList());
        assertTrue(vistas.get(0).esBorrador());
        assertNull(vistas.get(0).fechaEnvio());
        assertFalse(vistas.get(2).esBorrador());
        assertEquals(FECHA_ENVIO_ORIGINAL, vistas.get(2).fechaEnvio());
    }

    @Test
    @DisplayName("HU-26. Un estudiante sin planificaciones recibe una lista vacía")
    void listar_sinPlanificaciones_devuelveListaVacia() {
        when(planificacionRepository.findByLinea_Inscripcion_Estudiante_IdUsuarioOrderByFechaEnvioDesc(ID_ESTUDIANTE))
                .thenReturn(List.of());

        assertTrue(service.listarPlanificacionesDelEstudiante(ID_ESTUDIANTE).isEmpty());
    }

    @ParameterizedTest(name = "HU-26. Vista de una planificación {0}: borrador={1}, editable={2}, reenviable={3}")
    @CsvSource({
            "BORRADOR,  true,  true,  false",
            "ENVIADA,   false, false, false",
            "APROBADA,  false, false, false",
            "RECHAZADA, false, true,  true"
    })
    void vista_segunElEstado(EstadoPlanificacion estado, boolean borrador, boolean editable, boolean reenviable) {
        PlanificacionVista vista = new PlanificacionVista(1L, "Línea", "Contenido", estado, null, null, null);

        assertEquals(borrador, vista.esBorrador());
        assertEquals(editable, vista.puedeEditar());
        assertEquals(reenviable, vista.puedeReenviar());
        assertEquals(editable, PlanificacionService.esEditable(estado));
    }

    @Test
    @DisplayName("HU-31/32. La vista informa si la planificación tiene PDF y sus datos")
    void vista_conArchivo() {
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 1, 9, 0);
        Planificacion conArchivo = conArchivo(planificacion(EstadoPlanificacion.BORRADOR));
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.of(conArchivo));

        PlanificacionVista vista = service.obtenerVistaDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION);

        assertTrue(vista.tieneArchivo());
        assertEquals("plan.pdf", vista.archivoOriginal());
        assertEquals(1234L, vista.archivoTamano());
        assertEquals(fecha, vista.archivoFecha());
    }

    @Test
    @DisplayName("HU-31/32. Una planificación antigua, solo con texto, no tiene PDF")
    void vista_sinArchivo() {
        PlanificacionVista vista = new PlanificacionVista(1L, "Línea", "Texto", EstadoPlanificacion.BORRADOR,
                null, null, null);

        assertFalse(vista.tieneArchivo());
    }

    // ------------------------------------------------------------- entidad

    @Test
    @DisplayName("HU-31/32. Un borrador puede crearse solo con el PDF, sin texto")
    void entidad_borradorSoloConArchivo() {
        Planificacion borrador = Planificacion.borrador(lineaDe(ID_ESTUDIANTE, ID_PROFESOR), null);
        assertFalse(borrador.tieneArchivo());

        borrador.adjuntarArchivo("uuid.pdf", "plan.pdf", 500L, "Texto del PDF", LocalDateTime.of(2026, 10, 1, 9, 0));

        assertTrue(borrador.tieneArchivo());
        assertNull(borrador.getContenido());
        assertEquals("uuid.pdf", borrador.getArchivoNombre());
        assertEquals("plan.pdf", borrador.getArchivoOriginal());
        assertEquals("Texto del PDF", borrador.getTextoExtraido());
    }

    @Test
    @DisplayName("HU-31/32. Adjuntar otro PDF reemplaza al anterior en la misma planificación")
    void entidad_adjuntarReemplazaElArchivoAnterior() {
        Planificacion borrador = conArchivo(planificacion(EstadoPlanificacion.BORRADOR));

        borrador.adjuntarArchivo("nuevo.pdf", "version2.pdf", 900L, "Texto nuevo", LocalDateTime.of(2026, 10, 5, 9, 0));

        assertEquals("nuevo.pdf", borrador.getArchivoNombre());
        assertEquals("version2.pdf", borrador.getArchivoOriginal());
        assertEquals("Texto nuevo", borrador.getTextoExtraido());
    }

    @Test
    @DisplayName("HU-26. Planificacion.borrador crea un BORRADOR sin fecha de envío")
    void entidad_borrador_noTieneFechaDeEnvio() {
        LineaPractica linea = lineaDe(ID_ESTUDIANTE, ID_PROFESOR);

        Planificacion borrador = Planificacion.borrador(linea, "Plan de clase");

        assertEquals(EstadoPlanificacion.BORRADOR, borrador.getEstado());
        assertNull(borrador.getFechaEnvio());
        assertSame(linea, borrador.getLinea());
        assertEquals("Plan de clase", borrador.getContenido());
    }

    @Test
    @DisplayName("HU-26. El constructor con fecha de envío sigue creando una planificación ENVIADA")
    void entidad_constructorConFecha_esEnviada() {
        Planificacion enviada = new Planificacion(lineaDe(ID_ESTUDIANTE, ID_PROFESOR), "Plan de clase",
                FECHA_ENVIO_ORIGINAL);

        assertEquals(EstadoPlanificacion.ENVIADA, enviada.getEstado());
        assertEquals(FECHA_ENVIO_ORIGINAL, enviada.getFechaEnvio());
    }
}
