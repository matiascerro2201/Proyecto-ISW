package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.models.DocumentoEstudiante;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoInscripcion;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.PautaDocumento;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.TipoDocumento;
import com.grupo1.isw.practicas_pedagogicas.repositories.DocumentoEstudianteRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.InscripcionAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaDocumentoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DocumentoService {
    /** Debe coincidir con spring.servlet.multipart.max-file-size en application.properties. */
    static final long TAMANO_MAXIMO_BYTES = 20L * 1024 * 1024;
    private static final String EXTENSION_PDF = "pdf";
    private static final byte[] CABECERA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final InscripcionAsignaturaRepository inscripcionRepository;
    private final PautaDocumentoRepository pautaDocumentoRepository;
    private final DocumentoEstudianteRepository documentoRepository;
    private final AlmacenArchivos almacenArchivos;

    public DocumentoService(InscripcionAsignaturaRepository inscripcionRepository,
                            PautaDocumentoRepository pautaDocumentoRepository,
                            DocumentoEstudianteRepository documentoRepository,
                            AlmacenArchivos almacenArchivos) {
        this.inscripcionRepository = inscripcionRepository;
        this.pautaDocumentoRepository = pautaDocumentoRepository;
        this.documentoRepository = documentoRepository;
        this.almacenArchivos = almacenArchivos;
    }

    /** Una entrega exigida por la asignatura, con lo que el estudiante ha subido (si algo). */
    public record EntregaVista(Long idPautaDocumento,
                               String etiqueta,
                               BigDecimal ponderacion,
                               Long idDocumento,
                               LocalDateTime fechaSubida,
                               BigDecimal puntaje,
                               String comentarios,
                               boolean inscripcionAbierta) {
        public boolean entregado() {
            return idDocumento != null;
        }

        public boolean evaluado() {
            return puntaje != null;
        }

        /** Se puede subir si la inscripción admite entregas y lo ya subido (si hay algo) no fue evaluado. */
        public boolean puedeSubir() {
            return inscripcionAbierta && !evaluado();
        }
    }

    public record AsignaturaEntregas(Long idInscripcion,
                                     String asignatura,
                                     String periodo,
                                     boolean admiteEntregas,
                                     List<EntregaVista> entregas) {
    }

    /** Archivo que se entrega al usuario y el nombre con que se le ofrece para guardar. */
    public record DescargaDocumento(Path ruta, String nombreSugerido) {
    }

    @Transactional(readOnly = true)
    public List<AsignaturaEntregas> listarEntregasDelEstudiante(Long idEstudiante) {
        return inscripcionRepository.findByEstudianteConAsignatura(idEstudiante).stream()
                .map(this::aAsignaturaEntregas)
                .toList();
    }

    /**
     * Sube el PDF de una entrega. Si ya había uno sin evaluar, lo reemplaza.
     *
     * @return true si reemplazó una entrega anterior, false si es la primera vez
     * @throws IllegalArgumentException archivo vacío, demasiado grande o que no es PDF
     * @throws IllegalStateException    la inscripción ya no está EN_CURSO, o la entrega anterior ya fue evaluada
     * @throws ResponseStatusException  404 si algo no existe, 403 si la inscripción no es del estudiante
     */
    @Transactional
    public boolean subirDocumento(Long idEstudiante, Long idInscripcion, Long idPautaDocumento,
                                  MultipartFile archivo) {
        InscripcionAsignatura inscripcion = inscripcionRepository.findById(idInscripcion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscripción no encontrada"));

        if (!inscripcion.getEstudiante().getIdUsuario().equals(idEstudiante)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tiene permiso para subir documentos en esta inscripción");
        }

        if (!admiteEntregas(inscripcion)) {
            throw new IllegalStateException("Esta asignatura ya no admite entregas");
        }

        PautaDocumento pauta = pautaDocumentoRepository.findById(idPautaDocumento)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrega no encontrada"));

        Long asignaturaDeLaInscripcion = inscripcion.getSeccion().getAsignatura().getIdAsignatura();
        if (!pauta.getAsignatura().getIdAsignatura().equals(asignaturaDeLaInscripcion)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La entrega no corresponde a esta asignatura");
        }

        validarArchivo(archivo);

        Optional<DocumentoEstudiante> existente = documentoRepository
                .findByInscripcion_IdInscripcionAndPautaDocumento_IdPautaDocumento(idInscripcion, idPautaDocumento);

        if (existente.isPresent() && existente.get().getPuntajeObtenido() != null) {
            throw new IllegalStateException("Este documento ya fue evaluado y no puede reemplazarse");
        }

        String nombreNuevo = almacenArchivos.guardar(archivo, EXTENSION_PDF);
        String nombreAnterior = existente.map(DocumentoEstudiante::getContenidoUrl).orElse(null);

        try {
            if (existente.isPresent()) {
                DocumentoEstudiante documento = existente.get();
                documento.setContenidoUrl(nombreNuevo);
                documento.setFechaSubida(LocalDateTime.now());
                documentoRepository.saveAndFlush(documento);
            } else {
                documentoRepository.saveAndFlush(
                        new DocumentoEstudiante(inscripcion, pauta, nombreNuevo, LocalDateTime.now()));
            }
        } catch (RuntimeException e) {
            // Si la base falla, no dejamos un archivo huérfano en disco.
            almacenArchivos.eliminar(nombreNuevo);
            throw e;
        }

        if (nombreAnterior != null) {
            almacenArchivos.eliminar(nombreAnterior);
            return true;
        }
        return false;
    }

    /**
     * Localiza el archivo de un documento para descargarlo, solo si es del estudiante.
     *
     * @throws ResponseStatusException 404 si el documento o su archivo no existen, 403 si es de otro estudiante
     */
    @Transactional(readOnly = true)
    public DescargaDocumento obtenerParaDescarga(Long idEstudiante, Long idDocumento) {
        DocumentoEstudiante documento = documentoRepository.findById(idDocumento)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado"));

        if (!documento.getInscripcion().getEstudiante().getIdUsuario().equals(idEstudiante)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para acceder a este documento");
        }

        Path ruta = rutaDelArchivo(documento.getContenidoUrl());
        return new DescargaDocumento(ruta, nombreDeDescarga(documento.getPautaDocumento()));
    }

    private Path rutaDelArchivo(String nombreGuardado) {
        try {
            Path ruta = almacenArchivos.resolver(nombreGuardado);
            if (Files.isRegularFile(ruta)) {
                return ruta;
            }
        } catch (IllegalArgumentException e) {
            // Un nombre guardado inválido se trata igual que un archivo que no existe.
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo ya no está disponible");
    }

    /** En disco el archivo es un UUID; al usuario se le ofrece un nombre legible según la entrega. */
    private String nombreDeDescarga(PautaDocumento pauta) {
        return switch (pauta.getTipo()) {
            case INFORME -> "Informe-" + pauta.getNumero() + ".pdf";
            case PRODUCTO -> "Producto-" + pauta.getNumero() + ".pdf";
            case PROYECTO_INTERVENCION -> "Proyecto-de-intervencion.pdf";
        };
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar un archivo");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo de 20 MB");
        }
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !nombre.toLowerCase(Locale.ROOT).endsWith("." + EXTENSION_PDF)) {
            throw new IllegalArgumentException("Solo se permiten archivos PDF");
        }
        if (!empiezaComoPdf(archivo)) {
            throw new IllegalArgumentException("El archivo no es un PDF válido");
        }
    }

    /** La extensión se puede falsificar renombrando el archivo; la cabecera "%PDF-" es más difícil de fingir. */
    private boolean empiezaComoPdf(MultipartFile archivo) {
        try (InputStream entrada = archivo.getInputStream()) {
            return Arrays.equals(entrada.readNBytes(CABECERA_PDF.length), CABECERA_PDF);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo subido", e);
        }
    }

    private AsignaturaEntregas aAsignaturaEntregas(InscripcionAsignatura inscripcion) {
        SeccionAsignatura seccion = inscripcion.getSeccion();
        Long idAsignatura = seccion.getAsignatura().getIdAsignatura();
        boolean admiteEntregas = admiteEntregas(inscripcion);

        Map<Long, DocumentoEstudiante> subidos = documentoRepository
                .findByInscripcion_IdInscripcion(inscripcion.getIdInscripcion()).stream()
                .collect(Collectors.toMap(
                        d -> d.getPautaDocumento().getIdPautaDocumento(),
                        d -> d,
                        (a, b) -> a.getFechaSubida().isAfter(b.getFechaSubida()) ? a : b));

        List<EntregaVista> entregas = pautaDocumentoRepository
                .findByAsignatura_IdAsignaturaOrderByTipoAscNumeroAsc(idAsignatura).stream()
                .map(pauta -> aEntregaVista(pauta, subidos.get(pauta.getIdPautaDocumento()), admiteEntregas))
                .toList();

        return new AsignaturaEntregas(
                inscripcion.getIdInscripcion(),
                seccion.getAsignatura().getNombre(),
                seccion.getAnio() + "-" + seccion.getSemestre(),
                admiteEntregas,
                entregas);
    }

    private EntregaVista aEntregaVista(PautaDocumento pauta, DocumentoEstudiante documento, boolean inscripcionAbierta) {
        return new EntregaVista(
                pauta.getIdPautaDocumento(),
                etiquetaDe(pauta),
                pauta.getPonderacion(),
                documento == null ? null : documento.getIdDocumento(),
                documento == null ? null : documento.getFechaSubida(),
                documento == null ? null : documento.getPuntajeObtenido(),
                documento == null ? null : documento.getComentariosEvaluador(),
                inscripcionAbierta);
    }

    /** Solo las inscripciones EN_CURSO admiten subir o reemplazar documentos. */
    private boolean admiteEntregas(InscripcionAsignatura inscripcion) {
        return inscripcion.getEstado() == EstadoInscripcion.EN_CURSO;
    }

    private String etiquetaDe(PautaDocumento pauta) {
        return switch (pauta.getTipo()) {
            case INFORME -> "Informe " + pauta.getNumero();
            case PRODUCTO -> "Producto " + pauta.getNumero();
            case PROYECTO_INTERVENCION -> "Proyecto de intervención";
        };
    }
}
