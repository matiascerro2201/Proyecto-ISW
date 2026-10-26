package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Criterio;
import com.grupo1.isw.practicas_pedagogicas.models.Dimension;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionAsignaturaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * HU-01: el profesor de la asignatura define, en pantalla, las pautas de evaluación de sus asignaturas
 * (dimensiones y criterios con su puntaje máximo) para que colaboradores y tutores las completen.
 *
 * <p>Reglas: solo se gestionan asignaturas que el profesor imparte, y una pauta que ya tiene evaluaciones
 * aplicadas queda de solo lectura para no alterar puntajes ya registrados.
 */
@Service
public class GestionPautaService {
    private static final BigDecimal MAXIMO_PONDERACION = new BigDecimal("100");
    private static final BigDecimal MAXIMO_PUNTAJE = new BigDecimal("999.99");
    private static final int MAXIMO_NUMERO_APLICACION = 10;
    private static final int LARGO_MAXIMO_NOMBRE_DIMENSION = 200;
    private static final int LARGO_MAXIMO_DESCRIPCION_CRITERIO = 500;
    private static final int LARGO_MAXIMO_NUMERO = 10;

    public record CriterioVista(Long idCriterio, String descripcion, BigDecimal puntajeMax, int orden) {}

    public record DimensionVista(Long idDimension, String nombre, int orden, List<CriterioVista> criterios) {}

    public record PautaResumen(Long idPauta, TipoLinea tipoLinea, int numeroAplicacion,
                               BigDecimal ponderacion, int cantidadCriterios, boolean enUso) {}

    public record AsignaturaConPautas(Long idAsignatura, String nombre, List<PautaResumen> pautas) {}

    public record PautaDetalle(Long idPauta, Long idAsignatura, String nombreAsignatura, TipoLinea tipoLinea,
                               int numeroAplicacion, BigDecimal ponderacion, boolean enUso,
                               List<DimensionVista> dimensiones, BigDecimal puntajeTotal) {
        public boolean editable() {
            return !enUso;
        }

        /** Una pauta sirve para evaluar si tiene al menos una dimensión y todas tienen criterios. */
        public boolean completa() {
            return !dimensiones.isEmpty() && dimensiones.stream().noneMatch(d -> d.criterios().isEmpty());
        }
    }

    private final PautaEvaluacionRepository pautaRepository;
    private final SeccionAsignaturaRepository seccionRepository;

    public GestionPautaService(PautaEvaluacionRepository pautaRepository,
                               SeccionAsignaturaRepository seccionRepository) {
        this.pautaRepository = pautaRepository;
        this.seccionRepository = seccionRepository;
    }

    // ---------- consulta ----------

    @Transactional(readOnly = true)
    public List<AsignaturaConPautas> listarAsignaturas(Long idProfesor) {
        return seccionRepository.findAsignaturasDelProfesor(idProfesor).stream()
                .map(this::resumirAsignatura)
                .toList();
    }

    /** @throws ResponseStatusException 403 si el profesor no imparte la asignatura */
    @Transactional(readOnly = true)
    public AsignaturaConPautas obtenerAsignatura(Long idProfesor, Long idAsignatura) {
        return resumirAsignatura(asignaturaDelProfesor(idProfesor, idAsignatura));
    }

    /** @throws ResponseStatusException 404 si la pauta no existe, 403 si no es de una asignatura del profesor */
    @Transactional(readOnly = true)
    public PautaDetalle obtenerPauta(Long idProfesor, Long idPauta) {
        return armarDetalle(pautaDelProfesor(idProfesor, idPauta));
    }

    // ---------- pauta ----------

    /**
     * @return el id de la pauta creada
     * @throws IllegalArgumentException datos inválidos o pauta repetida (misma asignatura, tipo y aplicación)
     * @throws ResponseStatusException  403 si el profesor no imparte la asignatura
     */
    @Transactional
    public Long crearPauta(Long idProfesor, Long idAsignatura, String tipoLinea,
                           String numeroAplicacion, String ponderacion) {
        Asignatura asignatura = asignaturaDelProfesor(idProfesor, idAsignatura);
        TipoLinea tipo = parsearTipoLinea(tipoLinea);
        int numero = parsearNumeroAplicacion(numeroAplicacion);
        BigDecimal pond = parsearDecimal(ponderacion, "Ponderación", MAXIMO_PONDERACION);
        exigirSinDuplicado(idAsignatura, tipo, numero, null);

        PautaEvaluacion guardada = pautaRepository.save(new PautaEvaluacion(asignatura, tipo, numero, pond));
        return guardada.getIdPautaEvaluacion();
    }

    /**
     * @throws IllegalStateException   la pauta ya tiene evaluaciones aplicadas
     * @throws IllegalArgumentException datos inválidos o pauta repetida
     */
    @Transactional
    public void actualizarDatos(Long idProfesor, Long idPauta, String tipoLinea,
                                String numeroAplicacion, String ponderacion) {
        PautaEvaluacion pauta = pautaEditable(idProfesor, idPauta);
        TipoLinea tipo = parsearTipoLinea(tipoLinea);
        int numero = parsearNumeroAplicacion(numeroAplicacion);
        BigDecimal pond = parsearDecimal(ponderacion, "Ponderación", MAXIMO_PONDERACION);
        exigirSinDuplicado(pauta.getAsignatura().getIdAsignatura(), tipo, numero, pauta.getIdPautaEvaluacion());

        pauta.setTipoLinea(tipo);
        pauta.setNumeroAplicacion(numero);
        pauta.setPonderacion(pond);
        pautaRepository.save(pauta);
    }

    /** @throws IllegalStateException la pauta ya tiene evaluaciones aplicadas */
    @Transactional
    public void eliminarPauta(Long idProfesor, Long idPauta) {
        PautaEvaluacion pauta = pautaEditable(idProfesor, idPauta);
        // Se quita también de la colección de la asignatura para que Hibernate no la vuelva a guardar en cascada.
        pauta.getAsignatura().getPautasEvaluacion().remove(pauta);
        pautaRepository.delete(pauta);
    }

    // ---------- dimensiones ----------

    @Transactional
    public void agregarDimension(Long idProfesor, Long idPauta, String nombre) {
        PautaEvaluacion pauta = pautaEditable(idProfesor, idPauta);
        String limpio = limpiarTexto(nombre, "Nombre de la dimensión", LARGO_MAXIMO_NOMBRE_DIMENSION);

        int orden = pauta.getDimensiones().stream().mapToInt(Dimension::getOrden).max().orElse(0) + 1;
        pauta.getDimensiones().add(new Dimension(pauta, limpio, orden));
        pautaRepository.save(pauta);
    }

    /** Elimina la dimensión junto con sus criterios. */
    @Transactional
    public void eliminarDimension(Long idProfesor, Long idPauta, Long idDimension) {
        PautaEvaluacion pauta = pautaEditable(idProfesor, idPauta);
        boolean quitada = pauta.getDimensiones().removeIf(d -> Objects.equals(d.getIdDimension(), idDimension));
        if (!quitada) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La dimensión no existe en esta pauta");
        }
        pautaRepository.save(pauta);
    }

    // ---------- criterios ----------

    @Transactional
    public void agregarCriterio(Long idProfesor, Long idPauta, Long idDimension,
                                String descripcion, String puntajeMax) {
        PautaEvaluacion pauta = pautaEditable(idProfesor, idPauta);
        Dimension dimension = pauta.getDimensiones().stream()
                .filter(d -> Objects.equals(d.getIdDimension(), idDimension))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "La dimensión no existe en esta pauta"));

        String limpia = limpiarTexto(descripcion, "Descripción del criterio", LARGO_MAXIMO_DESCRIPCION_CRITERIO);
        BigDecimal puntaje = parsearDecimal(puntajeMax, "Puntaje máximo", MAXIMO_PUNTAJE);

        int orden = dimension.getCriterios().stream().mapToInt(Criterio::getOrden).max().orElse(0) + 1;
        dimension.getCriterios().add(new Criterio(dimension, limpia, puntaje, orden));
        pautaRepository.save(pauta);
    }

    @Transactional
    public void eliminarCriterio(Long idProfesor, Long idPauta, Long idCriterio) {
        PautaEvaluacion pauta = pautaEditable(idProfesor, idPauta);
        boolean quitado = pauta.getDimensiones().stream()
                .anyMatch(d -> d.getCriterios().removeIf(c -> Objects.equals(c.getIdCriterio(), idCriterio)));
        if (!quitado) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El criterio no existe en esta pauta");
        }
        pautaRepository.save(pauta);
    }

    // ---------- acceso y estado ----------

    private Asignatura asignaturaDelProfesor(Long idProfesor, Long idAsignatura) {
        return seccionRepository.findAsignaturasDelProfesor(idProfesor).stream()
                .filter(a -> Objects.equals(a.getIdAsignatura(), idAsignatura))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No impartes esta asignatura"));
    }

    private PautaEvaluacion pautaDelProfesor(Long idProfesor, Long idPauta) {
        PautaEvaluacion pauta = pautaRepository.findById(idPauta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La pauta no existe"));
        if (!seccionRepository.profesorImparte(idProfesor, pauta.getAsignatura().getIdAsignatura())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No tienes permiso para gestionar las pautas de esta asignatura");
        }
        return pauta;
    }

    private PautaEvaluacion pautaEditable(Long idProfesor, Long idPauta) {
        PautaEvaluacion pauta = pautaDelProfesor(idProfesor, idPauta);
        if (enUso(pauta)) {
            throw new IllegalStateException(
                    "Esta pauta ya tiene evaluaciones aplicadas y no se puede modificar");
        }
        return pauta;
    }

    private boolean enUso(PautaEvaluacion pauta) {
        return !pauta.getEvaluacionesAplicadas().isEmpty();
    }

    private void exigirSinDuplicado(Long idAsignatura, TipoLinea tipo, int numero, Long idPautaExcluida) {
        boolean existe = pautaRepository.findByAsignatura(idAsignatura).stream()
                .filter(p -> !Objects.equals(p.getIdPautaEvaluacion(), idPautaExcluida))
                .anyMatch(p -> p.getTipoLinea() == tipo && Objects.equals(p.getNumeroAplicacion(), numero));
        if (existe) {
            throw new IllegalArgumentException("Ya existe una pauta de tipo " + tipo
                    + " para la aplicación " + numero + " de esta asignatura");
        }
    }

    // ---------- armado de vistas ----------

    private AsignaturaConPautas resumirAsignatura(Asignatura asignatura) {
        List<PautaResumen> pautas = pautaRepository.findByAsignatura(asignatura.getIdAsignatura()).stream()
                .map(p -> new PautaResumen(
                        p.getIdPautaEvaluacion(),
                        p.getTipoLinea(),
                        p.getNumeroAplicacion(),
                        p.getPonderacion(),
                        p.getDimensiones().stream().mapToInt(d -> d.getCriterios().size()).sum(),
                        enUso(p)))
                .toList();
        return new AsignaturaConPautas(asignatura.getIdAsignatura(), asignatura.getNombre(), pautas);
    }

    private PautaDetalle armarDetalle(PautaEvaluacion pauta) {
        List<DimensionVista> dimensiones = pauta.getDimensiones().stream()
                .sorted(Comparator.comparing(Dimension::getOrden))
                .map(d -> new DimensionVista(
                        d.getIdDimension(),
                        d.getNombre(),
                        d.getOrden(),
                        d.getCriterios().stream()
                                .sorted(Comparator.comparing(Criterio::getOrden))
                                .map(c -> new CriterioVista(
                                        c.getIdCriterio(), c.getDescripcion(), c.getPuntajeMax(), c.getOrden()))
                                .toList()))
                .toList();

        BigDecimal total = dimensiones.stream()
                .flatMap(d -> d.criterios().stream())
                .map(CriterioVista::puntajeMax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PautaDetalle(
                pauta.getIdPautaEvaluacion(),
                pauta.getAsignatura().getIdAsignatura(),
                pauta.getAsignatura().getNombre(),
                pauta.getTipoLinea(),
                pauta.getNumeroAplicacion(),
                pauta.getPonderacion(),
                enUso(pauta),
                dimensiones,
                total);
    }

    // ---------- validación de entradas ----------

    private TipoLinea parsearTipoLinea(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Tipo de línea: elige una opción");
        }
        try {
            return TipoLinea.valueOf(texto.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de línea: elige una opción válida");
        }
    }

    private int parsearNumeroAplicacion(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Número de aplicación: ingresa un valor");
        }
        int numero;
        try {
            numero = Integer.parseInt(texto.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Número de aplicación: debe ser un número entero");
        }
        if (numero < 1 || numero > MAXIMO_NUMERO_APLICACION) {
            throw new IllegalArgumentException(
                    "Número de aplicación: debe estar entre 1 y " + MAXIMO_NUMERO_APLICACION);
        }
        return numero;
    }

    /** Número mayor que 0 y hasta {@code maximo}, con hasta dos decimales (coma o punto). */
    private BigDecimal parsearDecimal(String texto, String campo, BigDecimal maximo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(campo + ": ingresa un valor");
        }
        String limpio = texto.strip().replace(',', '.');
        BigDecimal valor;
        try {
            if (limpio.length() > LARGO_MAXIMO_NUMERO) {
                throw new NumberFormatException("texto demasiado largo");
            }
            valor = new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + ": debe ser un número, por ejemplo 12,5");
        }
        if (valor.signum() <= 0 || valor.compareTo(maximo) > 0) {
            throw new IllegalArgumentException(campo + ": debe ser mayor que 0 y como máximo "
                    + maximo.stripTrailingZeros().toPlainString());
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(campo + ": admite hasta dos decimales");
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    private String limpiarTexto(String texto, String campo, int largoMaximo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(campo + ": ingresa un texto");
        }
        String limpio = texto.strip();
        if (limpio.length() > largoMaximo) {
            throw new IllegalArgumentException(campo + ": máximo " + largoMaximo + " caracteres");
        }
        return limpio;
    }
}
