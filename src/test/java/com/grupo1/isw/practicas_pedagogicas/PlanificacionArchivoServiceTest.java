package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorColaborador;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PlanificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.services.AlmacenArchivos;
import com.grupo1.isw.practicas_pedagogicas.services.ExtractorTextoPdf;
import com.grupo1.isw.practicas_pedagogicas.services.ExtractorTextoPdf.Resultado;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionArchivoService;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionArchivoService.ArchivoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionArchivoService.ResultadoSubida;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU-31/32: subida, reemplazo, validación y acceso al PDF de la planificación. */
@ExtendWith(MockitoExtension.class)
class PlanificacionArchivoServiceTest {

    private static final Long ID_ESTUDIANTE = 4L;
    private static final Long ID_OTRO_ESTUDIANTE = 5L;
    private static final Long ID_COLABORADOR = 1L;
    private static final Long ID_LINEA = 7L;
    private static final Long ID_PLANIFICACION = 10L;
    private static final Long ID_NUEVA = 77L;
    private static final String NOMBRE_NUEVO = "nuevo-uuid.pdf";
    private static final String NOMBRE_VIEJO = "viejo-uuid.pdf";

    /** Texto con las seis secciones del formato A y más de 100 caracteres. */
    private static final String TEXTO_COMPLETO = "Objetivo de la clase: reconocer fracciones equivalentes en la recta numérica. "
            + "Inicio: problema de contexto. Desarrollo: trabajo en parejas. Cierre: ticket de salida. "
            + "Errores y dificultades: confundir numerador y denominador. Referencias bibliográficas: MINEDUC.";

    @Mock
    private PlanificacionRepository planificacionRepository;
    @Mock
    private LineaPracticaRepository lineaPracticaRepository;
    @Mock
    private PlanificacionService planificacionService;
    @Mock
    private AlmacenArchivos almacenArchivos;
    @Mock
    private ExtractorTextoPdf extractor;

    @InjectMocks
    private PlanificacionArchivoService service;

    // ------------------------------------------------------------------ ayudas

    private LineaPractica lineaDe(Long idEstudiante) {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(idEstudiante);
        estudiante.setNombre("Camila Soto");
        Asignatura asignatura = new Asignatura();
        asignatura.setNombre("Práctica Pedagógica");
        InscripcionAsignatura inscripcion =
                new InscripcionAsignatura(estudiante, new SeccionAsignatura(asignatura, null, 2026, 2), null);
        ProfesorColaborador colaborador = new ProfesorColaborador();
        colaborador.setIdUsuario(ID_COLABORADOR);
        return new LineaPractica(inscripcion, colaborador, null, TipoLinea.ESPECIALIDAD);
    }

    private static MockMultipartFile pdfValido() {
        return new MockMultipartFile("archivo", "plan.pdf", "application/pdf",
                "%PDF-1.4 contenido de prueba".getBytes(StandardCharsets.US_ASCII));
    }

    private Planificacion conArchivoViejo(EstadoPlanificacion estado) {
        Planificacion planificacion = estado == EstadoPlanificacion.BORRADOR
                ? Planificacion.borrador(lineaDe(ID_ESTUDIANTE), null)
                : new Planificacion(lineaDe(ID_ESTUDIANTE), null, LocalDateTime.of(2026, 10, 1, 9, 0));
        planificacion.setIdPlanificacion(ID_PLANIFICACION);
        planificacion.setEstado(estado);
        planificacion.adjuntarArchivo(NOMBRE_VIEJO, "viejo.pdf", 100L, "Texto viejo", LocalDateTime.of(2026, 10, 1, 9, 0));
        return planificacion;
    }

    private void dadoQueLaPlanificacionEs(Planificacion planificacion) {
        when(planificacionService.obtenerPlanificacionDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenReturn(planificacion);
    }

    private void dadoQueLaLineaEsDelEstudiante() {
        when(lineaPracticaRepository.findById(ID_LINEA)).thenReturn(Optional.of(lineaDe(ID_ESTUDIANTE)));
    }

    private void dadoQueElPdfTiene(String texto) {
        when(extractor.extraer(any(byte[].class))).thenReturn(new Resultado(texto, 2));
    }

    private void dadoQueSeGuardaEnDisco() {
        when(almacenArchivos.guardar(any(MultipartFile.class), eq("pdf"))).thenReturn(NOMBRE_NUEVO);
    }

    private void dadoQueLaBaseGuarda() {
        when(planificacionRepository.saveAndFlush(any(Planificacion.class))).thenAnswer(invocacion -> {
            Planificacion guardada = invocacion.getArgument(0);
            if (guardada.getIdPlanificacion() == null) {
                guardada.setIdPlanificacion(ID_NUEVA);
            }
            return guardada;
        });
    }

    private Planificacion planificacionGuardada() {
        ArgumentCaptor<Planificacion> guardada = ArgumentCaptor.forClass(Planificacion.class);
        verify(planificacionRepository).saveAndFlush(guardada.capture());
        return guardada.getValue();
    }

    private void verificarQueNoSeGuardoNada() {
        verify(almacenArchivos, never()).guardar(any(), any());
        verify(planificacionRepository, never()).saveAndFlush(any());
    }

    // ----------------------------------------------------------- subida nueva

    @Test
    @DisplayName("HU-31/32. Subir un PDF sin planificación crea un borrador con el PDF y su texto, sin avisar a nadie")
    void subir_creaUnBorradorConElPdf() {
        dadoQueLaLineaEsDelEstudiante();
        dadoQueElPdfTiene(TEXTO_COMPLETO);
        dadoQueSeGuardaEnDisco();
        dadoQueLaBaseGuarda();
        LocalDateTime antes = LocalDateTime.now();

        ResultadoSubida resultado = service.subirPdf(ID_ESTUDIANTE, ID_LINEA, null, pdfValido());

        Planificacion guardada = planificacionGuardada();
        assertEquals(EstadoPlanificacion.BORRADOR, guardada.getEstado());
        assertNull(guardada.getFechaEnvio());
        assertNull(guardada.getContenido());
        assertEquals(NOMBRE_NUEVO, guardada.getArchivoNombre());
        assertEquals("plan.pdf", guardada.getArchivoOriginal());
        assertEquals(TEXTO_COMPLETO, guardada.getTextoExtraido());
        assertEquals(ID_NUEVA, resultado.idPlanificacion());
        assertEquals("plan.pdf", resultado.nombreOriginal());
        assertFalse(resultado.reemplazo());
        assertTrue(resultado.advertencias().isEmpty(), resultado.advertencias().toString());
        assertFalse(resultado.fecha().isBefore(antes));
        verify(almacenArchivos, never()).eliminar(any());
    }

    @Test
    @DisplayName("HU-31/32. Sin línea de práctica seleccionada lanza IllegalArgument")
    void subir_sinLinea_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.subirPdf(ID_ESTUDIANTE, null, null, pdfValido()));

        verifyNoInteractions(extractor, almacenArchivos);
        verify(planificacionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("HU-31/32. Una línea que no existe lanza 404")
    void subir_lineaInexistente_lanza404() {
        when(lineaPracticaRepository.findById(ID_LINEA)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.subirPdf(ID_ESTUDIANTE, ID_LINEA, null, pdfValido()));

        assertEquals(404, ex.getStatusCode().value());
        verifyNoInteractions(extractor, almacenArchivos);
    }

    @Test
    @DisplayName("HU-31/32. La línea de otro estudiante lanza 403 y no se guarda nada")
    void subir_lineaAjena_lanza403() {
        when(lineaPracticaRepository.findById(ID_LINEA)).thenReturn(Optional.of(lineaDe(ID_OTRO_ESTUDIANTE)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.subirPdf(ID_ESTUDIANTE, ID_LINEA, null, pdfValido()));

        assertEquals(403, ex.getStatusCode().value());
        verifyNoInteractions(extractor, almacenArchivos);
        verify(planificacionRepository, never()).saveAndFlush(any());
    }

    // ------------------------------------------------------------ reemplazo

    @ParameterizedTest(name = "HU-31/32. Reemplazar el PDF de una planificación {0} borra el anterior y conserva el estado")
    @EnumSource(value = EstadoPlanificacion.class, names = {"BORRADOR", "RECHAZADA"})
    void subir_reemplazaElPdfAnterior(EstadoPlanificacion estado) {
        Planificacion planificacion = conArchivoViejo(estado);
        planificacion.setRetroalimentacion("Faltan objetivos");
        dadoQueLaPlanificacionEs(planificacion);
        dadoQueElPdfTiene(TEXTO_COMPLETO);
        dadoQueSeGuardaEnDisco();
        dadoQueLaBaseGuarda();

        ResultadoSubida resultado = service.subirPdf(ID_ESTUDIANTE, null, ID_PLANIFICACION, pdfValido());

        assertTrue(resultado.reemplazo());
        assertEquals(ID_PLANIFICACION, resultado.idPlanificacion());
        assertEquals(NOMBRE_NUEVO, planificacion.getArchivoNombre());
        assertEquals(TEXTO_COMPLETO, planificacion.getTextoExtraido());
        assertEquals(estado, planificacion.getEstado());
        assertEquals("Faltan objetivos", planificacion.getRetroalimentacion());
        verify(almacenArchivos).eliminar(NOMBRE_VIEJO);
        verify(lineaPracticaRepository, never()).findById(any());
    }

    @ParameterizedTest(name = "HU-31/32. No se puede cambiar el PDF de una planificación {0}")
    @EnumSource(value = EstadoPlanificacion.class, names = {"ENVIADA", "APROBADA"})
    void subir_noEditable_lanzaIllegalState(EstadoPlanificacion estado) {
        Planificacion planificacion = conArchivoViejo(estado);
        dadoQueLaPlanificacionEs(planificacion);

        assertThrows(IllegalStateException.class,
                () -> service.subirPdf(ID_ESTUDIANTE, null, ID_PLANIFICACION, pdfValido()));

        assertEquals(NOMBRE_VIEJO, planificacion.getArchivoNombre());
        verifyNoInteractions(extractor);
        verificarQueNoSeGuardoNada();
    }

    @Test
    @DisplayName("HU-31/32. La planificación de otro estudiante o inexistente se propaga (403/404) sin guardar nada")
    void subir_planificacionAjena_sePropaga() {
        when(planificacionService.obtenerPlanificacionDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.subirPdf(ID_ESTUDIANTE, null, ID_PLANIFICACION, pdfValido()));

        assertEquals(403, ex.getStatusCode().value());
        verifyNoInteractions(extractor);
        verificarQueNoSeGuardoNada();
    }

    @Test
    @DisplayName("HU-31/32. Si falla la base, el PDF nuevo no queda huérfano en disco y el anterior se conserva")
    void subir_falloDeLaBase_borraElNuevoYConservaElAnterior() {
        dadoQueLaPlanificacionEs(conArchivoViejo(EstadoPlanificacion.BORRADOR));
        dadoQueElPdfTiene(TEXTO_COMPLETO);
        dadoQueSeGuardaEnDisco();
        when(planificacionRepository.saveAndFlush(any(Planificacion.class)))
                .thenThrow(new RuntimeException("Falla de base de datos"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.subirPdf(ID_ESTUDIANTE, null, ID_PLANIFICACION, pdfValido()));

        assertEquals("Falla de base de datos", ex.getMessage());
        verify(almacenArchivos).eliminar(NOMBRE_NUEVO);
        verify(almacenArchivos, never()).eliminar(NOMBRE_VIEJO);
    }

    // ------------------------------------------------------------ validación

    static Stream<Arguments> archivosInvalidos() {
        return Stream.of(
                Arguments.of("sin archivo", null, "Debe seleccionar un archivo"),
                Arguments.of("archivo vacío",
                        new MockMultipartFile("archivo", "plan.pdf", "application/pdf", new byte[0]),
                        "Debe seleccionar un archivo"),
                Arguments.of("extensión que no es pdf",
                        new MockMultipartFile("archivo", "plan.docx", "application/pdf",
                                "%PDF-1.4 x".getBytes(StandardCharsets.US_ASCII)),
                        "Solo se permiten archivos PDF"),
                Arguments.of("renombrado: no empieza con %PDF-",
                        new MockMultipartFile("archivo", "plan.pdf", "application/pdf",
                                "esto es un texto cualquiera".getBytes(StandardCharsets.US_ASCII)),
                        "no es un PDF válido"));
    }

    @ParameterizedTest(name = "HU-31/32. Archivo rechazado: {0}")
    @MethodSource("archivosInvalidos")
    void subir_archivoInvalido_lanzaIllegalArgument(String caso, MockMultipartFile archivo, String mensaje) {
        dadoQueLaLineaEsDelEstudiante();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.subirPdf(ID_ESTUDIANTE, ID_LINEA, null, archivo));

        assertTrue(ex.getMessage().contains(mensaje), ex.getMessage());
        verifyNoInteractions(extractor, almacenArchivos);
        verify(planificacionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("HU-31/32. Un PDF sin texto suficiente (escaneo o foto) se rechaza y no se guarda")
    void subir_pdfSinTexto_seRechaza() {
        dadoQueLaLineaEsDelEstudiante();
        dadoQueElPdfTiene("Muy corto");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.subirPdf(ID_ESTUDIANTE, ID_LINEA, null, pdfValido()));

        assertTrue(ex.getMessage().contains("No se pudo leer texto"), ex.getMessage());
        verificarQueNoSeGuardoNada();
    }

    @Test
    @DisplayName("HU-31/32. Si el PDF está dañado o con contraseña el error del extractor llega al usuario y no se guarda")
    void subir_pdfIlegible_sePropagaElErrorDelExtractor() {
        dadoQueLaLineaEsDelEstudiante();
        when(extractor.extraer(any(byte[].class)))
                .thenThrow(new IllegalArgumentException("No se pudo leer el PDF; revisa que no esté dañado ni protegido"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.subirPdf(ID_ESTUDIANTE, ID_LINEA, null, pdfValido()));

        assertTrue(ex.getMessage().contains("No se pudo leer el PDF"), ex.getMessage());
        verificarQueNoSeGuardoNada();
    }

    @Test
    @DisplayName("HU-31/32. El texto guardado para el asistente se corta en 200.000 caracteres")
    void subir_textoMuyLargo_seRecorta() {
        dadoQueLaLineaEsDelEstudiante();
        dadoQueElPdfTiene("a".repeat(250_000));
        dadoQueSeGuardaEnDisco();
        dadoQueLaBaseGuarda();

        service.subirPdf(ID_ESTUDIANTE, ID_LINEA, null, pdfValido());

        assertEquals(200_000, planificacionGuardada().getTextoExtraido().length());
    }

    @Test
    @DisplayName("HU-31/32. El nombre visible del archivo no incluye carpetas")
    void subir_nombreOriginalSinCarpetas() {
        dadoQueLaLineaEsDelEstudiante();
        dadoQueElPdfTiene(TEXTO_COMPLETO);
        dadoQueSeGuardaEnDisco();
        dadoQueLaBaseGuarda();
        MockMultipartFile conRuta = new MockMultipartFile("archivo", "C:\\Users\\Camila\\Documentos\\plan.pdf",
                "application/pdf", "%PDF-1.4 x".getBytes(StandardCharsets.US_ASCII));

        ResultadoSubida resultado = service.subirPdf(ID_ESTUDIANTE, ID_LINEA, null, conRuta);

        assertEquals("plan.pdf", resultado.nombreOriginal());
        assertEquals("plan.pdf", planificacionGuardada().getArchivoOriginal());
    }

    // ---------------------------------------------------------- advertencias

    @Test
    @DisplayName("HU-31/32. Si faltan secciones del formato estándar se avisa de cuáles, sin impedir la subida")
    void subir_seccionesFaltantes_generaAdvertencias() {
        dadoQueLaLineaEsDelEstudiante();
        dadoQueElPdfTiene("Objetivo de la clase: reconocer fracciones equivalentes en la recta numérica y compararlas. "
                + "Inicio: problema de contexto. Desarrollo: trabajo en parejas con material concreto. "
                + "Errores y dificultades: confundir numerador y denominador al comparar.");
        dadoQueSeGuardaEnDisco();
        dadoQueLaBaseGuarda();

        ResultadoSubida resultado = service.subirPdf(ID_ESTUDIANTE, ID_LINEA, null, pdfValido());

        assertEquals(2, resultado.advertencias().size(), resultado.advertencias().toString());
        assertTrue(resultado.advertencias().stream().anyMatch(a -> a.contains("Cierre")));
        assertTrue(resultado.advertencias().stream().anyMatch(a -> a.contains("Referencias bibliográficas")));
        verify(planificacionRepository).saveAndFlush(any(Planificacion.class));
    }

    @Test
    @DisplayName("HU-31/32. La búsqueda de secciones ignora mayúsculas y tildes")
    void subir_seccionesSinTildesNiMinusculas_noGeneranAdvertencias() {
        dadoQueLaLineaEsDelEstudiante();
        dadoQueElPdfTiene("OBJETIVO DE LA CLASE: COMPARAR FRACCIONES USANDO LA RECTA NUMERICA Y MATERIAL CONCRETO. "
                + "INICIO DE LA CLASE. DESARROLLO DE LAS ACTIVIDADES. CIERRE Y SINTESIS. "
                + "ERRORES Y DIFICULTADES FRECUENTES. REFERENCIAS BIBLIOGRAFICAS.");
        dadoQueSeGuardaEnDisco();
        dadoQueLaBaseGuarda();

        ResultadoSubida resultado = service.subirPdf(ID_ESTUDIANTE, ID_LINEA, null, pdfValido());

        assertTrue(resultado.advertencias().isEmpty(), resultado.advertencias().toString());
    }

    // ---------------------------------------------------------------- acceso

    @Test
    @DisplayName("HU-31/32. El estudiante obtiene la ruta de su PDF y un nombre legible")
    void obtenerParaEstudiante_devuelveLaRuta(@TempDir Path carpeta) throws IOException {
        Path pdf = Files.writeString(carpeta.resolve(NOMBRE_VIEJO), "%PDF-1.4 x");
        dadoQueLaPlanificacionEs(conArchivoViejo(EstadoPlanificacion.BORRADOR));
        when(almacenArchivos.resolver(NOMBRE_VIEJO)).thenReturn(pdf);

        ArchivoPlanificacion archivo = service.obtenerParaEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION);

        assertEquals(pdf, archivo.ruta());
        assertEquals("Planificacion-10.pdf", archivo.nombreSugerido());
    }

    @Test
    @DisplayName("HU-31/32. Pedir el PDF de una planificación sin PDF lanza 404")
    void obtenerParaEstudiante_sinArchivo_lanza404() {
        Planificacion sinArchivo = Planificacion.borrador(lineaDe(ID_ESTUDIANTE), "Plan antiguo");
        sinArchivo.setIdPlanificacion(ID_PLANIFICACION);
        dadoQueLaPlanificacionEs(sinArchivo);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerParaEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION));

        assertEquals(404, ex.getStatusCode().value());
        verifyNoInteractions(almacenArchivos);
    }

    @Test
    @DisplayName("HU-31/32. Si el archivo ya no está en disco lanza 404")
    void obtenerParaEstudiante_archivoPerdido_lanza404(@TempDir Path carpeta) {
        dadoQueLaPlanificacionEs(conArchivoViejo(EstadoPlanificacion.BORRADOR));
        when(almacenArchivos.resolver(NOMBRE_VIEJO)).thenReturn(carpeta.resolve("no-existe.pdf"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerParaEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("HU-31/32. Un nombre guardado inválido se trata como archivo inexistente (404)")
    void obtenerParaEstudiante_nombreInvalido_lanza404() {
        dadoQueLaPlanificacionEs(conArchivoViejo(EstadoPlanificacion.BORRADOR));
        when(almacenArchivos.resolver(NOMBRE_VIEJO)).thenThrow(new IllegalArgumentException("Nombre de archivo inválido"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerParaEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("HU-31/32. La planificación de otro estudiante da 403 al pedir su PDF")
    void obtenerParaEstudiante_ajena_lanza403() {
        when(planificacionService.obtenerPlanificacionDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerParaEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION));

        assertEquals(403, ex.getStatusCode().value());
        verifyNoInteractions(almacenArchivos);
    }

    @Test
    @DisplayName("HU-31/32. El colaborador de la línea obtiene el PDF de una planificación enviada")
    void obtenerParaColaborador_devuelveLaRuta(@TempDir Path carpeta) throws IOException {
        Path pdf = Files.writeString(carpeta.resolve(NOMBRE_VIEJO), "%PDF-1.4 x");
        when(planificacionService.obtenerPlanificacionParaColaborador(ID_PLANIFICACION, ID_COLABORADOR))
                .thenReturn(conArchivoViejo(EstadoPlanificacion.ENVIADA));
        when(almacenArchivos.resolver(NOMBRE_VIEJO)).thenReturn(pdf);

        ArchivoPlanificacion archivo = service.obtenerParaColaborador(ID_PLANIFICACION, ID_COLABORADOR);

        assertSame(pdf, archivo.ruta());
        assertEquals("Planificacion-10.pdf", archivo.nombreSugerido());
    }

    @ParameterizedTest(name = "HU-31/32. Para el colaborador, el PDF de un borrador o de otra línea da {0}")
    @EnumSource(value = HttpStatus.class, names = {"NOT_FOUND", "FORBIDDEN"})
    void obtenerParaColaborador_sinPermiso_sePropaga(HttpStatus estado) {
        when(planificacionService.obtenerPlanificacionParaColaborador(ID_PLANIFICACION, ID_COLABORADOR))
                .thenThrow(new ResponseStatusException(estado));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerParaColaborador(ID_PLANIFICACION, ID_COLABORADOR));

        assertEquals(estado.value(), ex.getStatusCode().value());
        verifyNoInteractions(almacenArchivos);
    }
}
