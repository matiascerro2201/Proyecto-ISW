package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.DocumentoEstudiante;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoInscripcion;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.PautaDocumento;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.TipoDocumento;
import com.grupo1.isw.practicas_pedagogicas.repositories.DocumentoEstudianteRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.InscripcionAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaDocumentoRepository;
import com.grupo1.isw.practicas_pedagogicas.services.AlmacenArchivos;
import com.grupo1.isw.practicas_pedagogicas.services.DocumentoService;
import com.grupo1.isw.practicas_pedagogicas.services.DocumentoService.AsignaturaEntregas;
import com.grupo1.isw.practicas_pedagogicas.services.DocumentoService.DescargaDocumento;
import com.grupo1.isw.practicas_pedagogicas.services.DocumentoService.EntregaVista;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    private static final Long ID_ESTUDIANTE = 4L;
    private static final Long ID_OTRO_ESTUDIANTE = 5L;
    private static final Long ID_INSCRIPCION = 1L;
    private static final Long ID_ASIGNATURA = 1L;
    private static final Long ID_PAUTA = 7L;
    private static final Long ID_DOCUMENTO = 10L;
    private static final long VEINTE_MB = 20L * 1024 * 1024;
    private static final byte[] PDF = "%PDF-1.4 contenido de prueba".getBytes(StandardCharsets.US_ASCII);

    @Mock
    private InscripcionAsignaturaRepository inscripcionRepository;

    @Mock
    private PautaDocumentoRepository pautaDocumentoRepository;

    @Mock
    private DocumentoEstudianteRepository documentoRepository;

    @Mock
    private AlmacenArchivos almacenArchivos;

    @InjectMocks
    private DocumentoService service;

    private Asignatura asignatura;
    private InscripcionAsignatura inscripcion;
    private PautaDocumento pauta;

    @BeforeEach
    void armarEscenario() {
        asignatura = new Asignatura();
        asignatura.setIdAsignatura(ID_ASIGNATURA);
        asignatura.setNombre("Práctica Pedagógica");
        inscripcion = inscripcionDe(ID_ESTUDIANTE, EstadoInscripcion.EN_CURSO);
        pauta = pautaDe(ID_PAUTA, TipoDocumento.INFORME, 1, "30.00");
    }

    // ------------------------------------------------------------------ ayudas

    private InscripcionAsignatura inscripcionDe(Long idEstudiante, EstadoInscripcion estado) {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(idEstudiante);

        SeccionAsignatura seccion = new SeccionAsignatura();
        seccion.setAsignatura(asignatura);
        seccion.setAnio(2026);
        seccion.setSemestre(2);

        InscripcionAsignatura nueva = new InscripcionAsignatura();
        nueva.setIdInscripcion(ID_INSCRIPCION);
        nueva.setEstudiante(estudiante);
        nueva.setSeccion(seccion);
        nueva.setEstado(estado);
        return nueva;
    }

    private PautaDocumento pautaDe(Long id, TipoDocumento tipo, int numero, String ponderacion) {
        PautaDocumento nueva = new PautaDocumento(asignatura, new BigDecimal(ponderacion), tipo, numero);
        nueva.setIdPautaDocumento(id);
        return nueva;
    }

    private MockMultipartFile pdf(String nombre) {
        return new MockMultipartFile("archivo", nombre, "application/pdf", PDF);
    }

    private boolean subir(MultipartFile archivo) {
        return service.subirDocumento(ID_ESTUDIANTE, ID_INSCRIPCION, ID_PAUTA, archivo);
    }

    private void dadoQueExistenInscripcionYPauta() {
        when(inscripcionRepository.findById(ID_INSCRIPCION)).thenReturn(Optional.of(inscripcion));
        when(pautaDocumentoRepository.findById(ID_PAUTA)).thenReturn(Optional.of(pauta));
    }

    private void dadoQueLaEntregaPreviaEs(Optional<DocumentoEstudiante> previa) {
        when(documentoRepository
                .findByInscripcion_IdInscripcionAndPautaDocumento_IdPautaDocumento(ID_INSCRIPCION, ID_PAUTA))
                .thenReturn(previa);
    }

    private DocumentoEstudiante documentoSubido(String nombreEnDisco) {
        DocumentoEstudiante documento = new DocumentoEstudiante(
                inscripcion, pauta, nombreEnDisco, LocalDateTime.of(2026, 1, 1, 10, 0));
        documento.setIdDocumento(ID_DOCUMENTO);
        return documento;
    }

    private void verificarQueNoSeGuardoNada() {
        verify(almacenArchivos, never()).guardar(any(), any());
        verify(documentoRepository, never()).saveAndFlush(any());
    }

    // ------------------------------------------------- subir: quién y a qué

    @Test
    @DisplayName("1. Lanza 404 si la inscripción no existe")
    void inscripcionInexistente_lanza404() {
        when(inscripcionRepository.findById(ID_INSCRIPCION)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> subir(pdf("informe.pdf")));

        assertEquals(404, ex.getStatusCode().value());
        verificarQueNoSeGuardoNada();
    }

    @Test
    @DisplayName("2. Lanza 403 si la inscripción es de otro estudiante")
    void inscripcionAjena_lanza403() {
        inscripcion = inscripcionDe(ID_OTRO_ESTUDIANTE, EstadoInscripcion.EN_CURSO);
        when(inscripcionRepository.findById(ID_INSCRIPCION)).thenReturn(Optional.of(inscripcion));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> subir(pdf("informe.pdf")));

        assertEquals(403, ex.getStatusCode().value());
        verifyNoInteractions(pautaDocumentoRepository);
        verificarQueNoSeGuardoNada();
    }

    @ParameterizedTest(name = "3. Inscripción {0}: ya no admite entregas")
    @EnumSource(value = EstadoInscripcion.class, names = "EN_CURSO", mode = EnumSource.Mode.EXCLUDE)
    void inscripcionNoEnCurso_lanzaIllegalState(EstadoInscripcion estado) {
        inscripcion = inscripcionDe(ID_ESTUDIANTE, estado);
        when(inscripcionRepository.findById(ID_INSCRIPCION)).thenReturn(Optional.of(inscripcion));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> subir(pdf("informe.pdf")));

        assertTrue(ex.getMessage().contains("ya no admite entregas"));
        verifyNoInteractions(pautaDocumentoRepository);
        verificarQueNoSeGuardoNada();
    }

    @Test
    @DisplayName("4. Lanza 404 si la entrega no existe")
    void pautaInexistente_lanza404() {
        when(inscripcionRepository.findById(ID_INSCRIPCION)).thenReturn(Optional.of(inscripcion));
        when(pautaDocumentoRepository.findById(ID_PAUTA)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> subir(pdf("informe.pdf")));

        assertEquals(404, ex.getStatusCode().value());
        verificarQueNoSeGuardoNada();
    }

    @Test
    @DisplayName("5. Lanza 404 si la entrega es de otra asignatura")
    void pautaDeOtraAsignatura_lanza404() {
        Asignatura otra = new Asignatura();
        otra.setIdAsignatura(99L);
        PautaDocumento ajena = new PautaDocumento(otra, new BigDecimal("30.00"), TipoDocumento.INFORME, 1);
        ajena.setIdPautaDocumento(ID_PAUTA);
        when(inscripcionRepository.findById(ID_INSCRIPCION)).thenReturn(Optional.of(inscripcion));
        when(pautaDocumentoRepository.findById(ID_PAUTA)).thenReturn(Optional.of(ajena));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> subir(pdf("informe.pdf")));

        assertEquals(404, ex.getStatusCode().value());
        verificarQueNoSeGuardoNada();
    }

    // ----------------------------------------------------- subir: el archivo

    @Test
    @DisplayName("6. Archivo vacío: lanza IllegalArgument")
    void archivoVacio_lanzaIllegalArgument() {
        dadoQueExistenInscripcionYPauta();
        MockMultipartFile vacio = new MockMultipartFile("archivo", "informe.pdf", "application/pdf", new byte[0]);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> subir(vacio));

        assertTrue(ex.getMessage().contains("Debe seleccionar un archivo"));
        verificarQueNoSeGuardoNada();
    }

    @Test
    @DisplayName("7. Sin archivo (null): lanza IllegalArgument")
    void archivoNulo_lanzaIllegalArgument() {
        dadoQueExistenInscripcionYPauta();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> subir(null));

        assertTrue(ex.getMessage().contains("Debe seleccionar un archivo"));
        verificarQueNoSeGuardoNada();
    }

    @Test
    @DisplayName("8. Archivo de más de 20 MB: lanza IllegalArgument")
    void archivoDemasiadoGrande_lanzaIllegalArgument() {
        dadoQueExistenInscripcionYPauta();
        MultipartFile grande = mock(MultipartFile.class);
        when(grande.isEmpty()).thenReturn(false);
        when(grande.getSize()).thenReturn(VEINTE_MB + 1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> subir(grande));

        assertTrue(ex.getMessage().contains("20 MB"));
        verificarQueNoSeGuardoNada();
    }

    @Test
    @DisplayName("9. Archivo de exactamente 20 MB: se acepta (el límite es inclusivo)")
    void archivoDeExactamente20Mb_seAcepta() throws IOException {
        dadoQueExistenInscripcionYPauta();
        dadoQueLaEntregaPreviaEs(Optional.empty());
        MultipartFile limite = mock(MultipartFile.class);
        when(limite.isEmpty()).thenReturn(false);
        when(limite.getSize()).thenReturn(VEINTE_MB);
        when(limite.getOriginalFilename()).thenReturn("informe.pdf");
        when(limite.getInputStream()).thenAnswer(invocacion -> new ByteArrayInputStream(PDF));
        when(almacenArchivos.guardar(limite, "pdf")).thenReturn("nuevo.pdf");

        boolean reemplazo = subir(limite);

        assertFalse(reemplazo);
        verify(documentoRepository).saveAndFlush(any(DocumentoEstudiante.class));
    }

    @ParameterizedTest(name = "10. Extensión no permitida: [{0}]")
    @ValueSource(strings = {"informe.docx", "foto.png", "programa.exe", "informe", "informe.pdf.exe"})
    void extensionNoPermitida_lanzaIllegalArgument(String nombre) {
        dadoQueExistenInscripcionYPauta();
        MockMultipartFile archivo = pdf(nombre);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> subir(archivo));

        assertTrue(ex.getMessage().contains("Solo se permiten archivos PDF"));
        verificarQueNoSeGuardoNada();
    }

    @ParameterizedTest(name = "11. Se llama .pdf pero no lo es: [{0}]")
    @ValueSource(strings = {"MZ-programa-ejecutable", "%PD", "texto cualquiera"})
    void falsoPdf_lanzaIllegalArgument(String contenido) {
        dadoQueExistenInscripcionYPauta();
        MockMultipartFile falso = new MockMultipartFile("archivo", "falso.pdf", "application/pdf",
                contenido.getBytes(StandardCharsets.US_ASCII));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> subir(falso));

        assertTrue(ex.getMessage().contains("no es un PDF válido"));
        verificarQueNoSeGuardoNada();
    }

    @Test
    @DisplayName("12. La extensión en mayúsculas (.PDF) se acepta")
    void extensionEnMayusculas_seAcepta() {
        dadoQueExistenInscripcionYPauta();
        dadoQueLaEntregaPreviaEs(Optional.empty());
        when(almacenArchivos.guardar(any(), eq("pdf"))).thenReturn("nuevo.pdf");

        boolean reemplazo = subir(pdf("INFORME.PDF"));

        assertFalse(reemplazo);
        verify(documentoRepository).saveAndFlush(any(DocumentoEstudiante.class));
    }

    // ------------------------------------- subir: primera vez y reemplazo

    @Test
    @DisplayName("13. Primera subida: guarda el archivo y crea la fila")
    void primeraSubida_guardaArchivoYCreaFila() {
        dadoQueExistenInscripcionYPauta();
        dadoQueLaEntregaPreviaEs(Optional.empty());
        when(almacenArchivos.guardar(any(), eq("pdf"))).thenReturn("nuevo.pdf");
        LocalDateTime antes = LocalDateTime.now();

        boolean reemplazo = subir(pdf("informe.pdf"));

        assertFalse(reemplazo, "es la primera vez: no reemplaza nada");
        ArgumentCaptor<DocumentoEstudiante> guardado = ArgumentCaptor.forClass(DocumentoEstudiante.class);
        verify(documentoRepository).saveAndFlush(guardado.capture());
        DocumentoEstudiante creado = guardado.getValue();
        assertEquals("nuevo.pdf", creado.getContenidoUrl());
        assertSame(inscripcion, creado.getInscripcion());
        assertSame(pauta, creado.getPautaDocumento());
        assertFalse(creado.getFechaSubida().isBefore(antes), "fechaSubida debe ser la del momento de la subida");
        verify(almacenArchivos, never()).eliminar(any());
    }

    @Test
    @DisplayName("14. Reemplazo de un documento sin evaluar: actualiza la fila y borra el archivo anterior")
    void reemplazoSinEvaluar_actualizaFilaYBorraElAnterior() {
        dadoQueExistenInscripcionYPauta();
        DocumentoEstudiante previo = documentoSubido("viejo.pdf");
        LocalDateTime fechaAnterior = previo.getFechaSubida();
        dadoQueLaEntregaPreviaEs(Optional.of(previo));
        when(almacenArchivos.guardar(any(), eq("pdf"))).thenReturn("nuevo.pdf");

        boolean reemplazo = subir(pdf("informe.pdf"));

        assertTrue(reemplazo);
        assertEquals("nuevo.pdf", previo.getContenidoUrl());
        assertTrue(previo.getFechaSubida().isAfter(fechaAnterior), "la fecha debe actualizarse");
        // Se actualiza la misma fila: no se crea otra.
        verify(documentoRepository).saveAndFlush(previo);
        verify(almacenArchivos, never()).eliminar("nuevo.pdf");
        // El archivo viejo solo se borra DESPUÉS de que la base aceptó el cambio.
        InOrder orden = inOrder(documentoRepository, almacenArchivos);
        orden.verify(documentoRepository).saveAndFlush(previo);
        orden.verify(almacenArchivos).eliminar("viejo.pdf");
    }

    @Test
    @DisplayName("15. Reemplazo de un documento ya evaluado: lanza IllegalState y no toca nada")
    void reemplazoDeDocumentoEvaluado_lanzaIllegalState() {
        dadoQueExistenInscripcionYPauta();
        DocumentoEstudiante evaluado = documentoSubido("viejo.pdf");
        evaluado.setPuntajeObtenido(new BigDecimal("85.00"));
        dadoQueLaEntregaPreviaEs(Optional.of(evaluado));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> subir(pdf("informe.pdf")));

        assertTrue(ex.getMessage().contains("ya fue evaluado"));
        assertEquals("viejo.pdf", evaluado.getContenidoUrl());
        verificarQueNoSeGuardoNada();
        verify(almacenArchivos, never()).eliminar(any());
    }

    @Test
    @DisplayName("16. Si la base falla: borra el archivo nuevo y conserva el anterior")
    void fallaLaBase_borraElNuevoYConservaElAnterior() {
        dadoQueExistenInscripcionYPauta();
        DocumentoEstudiante previo = documentoSubido("viejo.pdf");
        dadoQueLaEntregaPreviaEs(Optional.of(previo));
        when(almacenArchivos.guardar(any(), eq("pdf"))).thenReturn("nuevo.pdf");
        when(documentoRepository.saveAndFlush(any(DocumentoEstudiante.class)))
                .thenThrow(new RuntimeException("falla de la base"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> subir(pdf("informe.pdf")));

        assertEquals("falla de la base", ex.getMessage());
        verify(almacenArchivos).eliminar("nuevo.pdf");
        verify(almacenArchivos, never()).eliminar("viejo.pdf");
    }

    // ------------------------------------------------------------ listado

    @Test
    @DisplayName("17. Listado: distingue pendiente, entregado y evaluado")
    void listar_marcaPendienteEntregadoYEvaluado() {
        PautaDocumento informe = pautaDe(1L, TipoDocumento.INFORME, 1, "30.00");
        PautaDocumento producto = pautaDe(2L, TipoDocumento.PRODUCTO, 1, "30.00");
        PautaDocumento proyecto = pautaDe(3L, TipoDocumento.PROYECTO_INTERVENCION, 1, "40.00");

        DocumentoEstudiante subidoSinEvaluar = new DocumentoEstudiante(
                inscripcion, informe, "a.pdf", LocalDateTime.of(2026, 10, 5, 10, 15));
        subidoSinEvaluar.setIdDocumento(10L);
        DocumentoEstudiante evaluado = new DocumentoEstudiante(
                inscripcion, producto, "b.pdf", LocalDateTime.of(2026, 10, 6, 16, 40));
        evaluado.setIdDocumento(11L);
        evaluado.setPuntajeObtenido(new BigDecimal("85.00"));
        evaluado.setComentariosEvaluador("Buen producto");

        when(inscripcionRepository.findByEstudianteConAsignatura(ID_ESTUDIANTE)).thenReturn(List.of(inscripcion));
        when(documentoRepository.findByInscripcion_IdInscripcion(ID_INSCRIPCION))
                .thenReturn(List.of(subidoSinEvaluar, evaluado));
        when(pautaDocumentoRepository.findByAsignatura_IdAsignaturaOrderByTipoAscNumeroAsc(ID_ASIGNATURA))
                .thenReturn(List.of(informe, producto, proyecto));

        List<AsignaturaEntregas> resultado = service.listarEntregasDelEstudiante(ID_ESTUDIANTE);

        assertEquals(1, resultado.size());
        AsignaturaEntregas asignaturaVista = resultado.get(0);
        assertEquals("Práctica Pedagógica", asignaturaVista.asignatura());
        assertEquals("2026-2", asignaturaVista.periodo());
        assertTrue(asignaturaVista.admiteEntregas());
        assertEquals(ID_INSCRIPCION, asignaturaVista.idInscripcion());
        assertEquals(3, asignaturaVista.entregas().size());

        EntregaVista entregaInforme = asignaturaVista.entregas().get(0);
        assertEquals("Informe 1", entregaInforme.etiqueta());
        assertTrue(entregaInforme.entregado());
        assertFalse(entregaInforme.evaluado());
        assertTrue(entregaInforme.puedeSubir(), "entregado pero sin evaluar: se puede reemplazar");

        EntregaVista entregaProducto = asignaturaVista.entregas().get(1);
        assertEquals("Producto 1", entregaProducto.etiqueta());
        assertTrue(entregaProducto.entregado());
        assertTrue(entregaProducto.evaluado());
        assertEquals(new BigDecimal("85.00"), entregaProducto.puntaje());
        assertEquals("Buen producto", entregaProducto.comentarios());
        assertFalse(entregaProducto.puedeSubir(), "evaluado: no se puede reemplazar");

        EntregaVista entregaProyecto = asignaturaVista.entregas().get(2);
        assertEquals("Proyecto de intervención", entregaProyecto.etiqueta());
        assertFalse(entregaProyecto.entregado());
        assertFalse(entregaProyecto.evaluado());
        assertTrue(entregaProyecto.puedeSubir());
    }

    @ParameterizedTest(name = "18. Listado con inscripción {0}: nadie puede subir")
    @EnumSource(value = EstadoInscripcion.class, names = "EN_CURSO", mode = EnumSource.Mode.EXCLUDE)
    void listar_inscripcionCerrada_nadiePuedeSubir(EstadoInscripcion estado) {
        inscripcion = inscripcionDe(ID_ESTUDIANTE, estado);
        when(inscripcionRepository.findByEstudianteConAsignatura(ID_ESTUDIANTE)).thenReturn(List.of(inscripcion));
        when(documentoRepository.findByInscripcion_IdInscripcion(ID_INSCRIPCION)).thenReturn(List.of());
        when(pautaDocumentoRepository.findByAsignatura_IdAsignaturaOrderByTipoAscNumeroAsc(ID_ASIGNATURA))
                .thenReturn(List.of(pauta));

        AsignaturaEntregas asignaturaVista = service.listarEntregasDelEstudiante(ID_ESTUDIANTE).get(0);

        assertFalse(asignaturaVista.admiteEntregas());
        assertFalse(asignaturaVista.entregas().get(0).puedeSubir());
    }

    @Test
    @DisplayName("19. Listado: un estudiante sin inscripciones recibe una lista vacía")
    void listar_sinInscripciones_devuelveListaVacia() {
        when(inscripcionRepository.findByEstudianteConAsignatura(ID_ESTUDIANTE)).thenReturn(List.of());

        assertTrue(service.listarEntregasDelEstudiante(ID_ESTUDIANTE).isEmpty());
    }

    // ----------------------------------------------------------- descarga

    @Test
    @DisplayName("20. Descarga del dueño: devuelve la ruta del archivo y un nombre legible")
    void descarga_delDueno_devuelveRutaYNombre(@TempDir Path carpeta) throws IOException {
        Path archivo = Files.write(carpeta.resolve("abc.pdf"), PDF);
        when(documentoRepository.findById(ID_DOCUMENTO)).thenReturn(Optional.of(documentoSubido("abc.pdf")));
        when(almacenArchivos.resolver("abc.pdf")).thenReturn(archivo);

        DescargaDocumento descarga = service.obtenerParaDescarga(ID_ESTUDIANTE, ID_DOCUMENTO);

        assertEquals(archivo, descarga.ruta());
        assertEquals("Informe-1.pdf", descarga.nombreSugerido());
    }

    @ParameterizedTest(name = "21. Nombre de descarga: {0} {1} -> {2}")
    @CsvSource({
            "INFORME, 1, Informe-1.pdf",
            "INFORME, 2, Informe-2.pdf",
            "PRODUCTO, 1, Producto-1.pdf",
            "PROYECTO_INTERVENCION, 1, Proyecto-de-intervencion.pdf"
    })
    void descarga_nombreSegunTipo(TipoDocumento tipo, int numero, String esperado, @TempDir Path carpeta)
            throws IOException {
        pauta = pautaDe(ID_PAUTA, tipo, numero, "30.00");
        Path archivo = Files.write(carpeta.resolve("abc.pdf"), PDF);
        when(documentoRepository.findById(ID_DOCUMENTO)).thenReturn(Optional.of(documentoSubido("abc.pdf")));
        when(almacenArchivos.resolver("abc.pdf")).thenReturn(archivo);

        assertEquals(esperado, service.obtenerParaDescarga(ID_ESTUDIANTE, ID_DOCUMENTO).nombreSugerido());
    }

    @Test
    @DisplayName("22. Descarga de un documento ajeno: lanza 403 y no toca el disco")
    void descarga_deOtroEstudiante_lanza403() {
        inscripcion = inscripcionDe(ID_OTRO_ESTUDIANTE, EstadoInscripcion.EN_CURSO);
        when(documentoRepository.findById(ID_DOCUMENTO)).thenReturn(Optional.of(documentoSubido("abc.pdf")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerParaDescarga(ID_ESTUDIANTE, ID_DOCUMENTO));

        assertEquals(403, ex.getStatusCode().value());
        verifyNoInteractions(almacenArchivos);
    }

    @Test
    @DisplayName("23. Descarga de un documento que no existe: lanza 404")
    void descarga_documentoInexistente_lanza404() {
        when(documentoRepository.findById(ID_DOCUMENTO)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerParaDescarga(ID_ESTUDIANTE, ID_DOCUMENTO));

        assertEquals(404, ex.getStatusCode().value());
        verifyNoInteractions(almacenArchivos);
    }

    @Test
    @DisplayName("24. Descarga cuando el archivo ya no está en disco: lanza 404")
    void descarga_archivoFaltanteEnDisco_lanza404(@TempDir Path carpeta) {
        when(documentoRepository.findById(ID_DOCUMENTO)).thenReturn(Optional.of(documentoSubido("abc.pdf")));
        when(almacenArchivos.resolver("abc.pdf")).thenReturn(carpeta.resolve("abc.pdf"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerParaDescarga(ID_ESTUDIANTE, ID_DOCUMENTO));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("25. Descarga con un nombre guardado inválido (../): lanza 404, no un error 500")
    void descarga_nombreGuardadoInvalido_lanza404() {
        when(documentoRepository.findById(ID_DOCUMENTO)).thenReturn(Optional.of(documentoSubido("../../secreto")));
        when(almacenArchivos.resolver("../../secreto"))
                .thenThrow(new IllegalArgumentException("Nombre de archivo inválido"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtenerParaDescarga(ID_ESTUDIANTE, ID_DOCUMENTO));

        assertEquals(404, ex.getStatusCode().value());
    }
}
