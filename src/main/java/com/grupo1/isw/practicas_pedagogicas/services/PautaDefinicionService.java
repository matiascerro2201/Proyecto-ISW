package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.PautaFormulario;
import com.grupo1.isw.practicas_pedagogicas.dto.PautaFormulario.CriterioFormulario;
import com.grupo1.isw.practicas_pedagogicas.dto.PautaFormulario.DimensionFormulario;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Criterio;
import com.grupo1.isw.practicas_pedagogicas.models.Dimension;
import com.grupo1.isw.practicas_pedagogicas.models.PautaDocumento;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionAplicadaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaDocumentoRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionAsignaturaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * HU-01: el profesor de la asignatura define sus pautas de evaluación en un solo formulario (datos generales,
 * dimensiones y criterios con su puntaje máximo) para que colaboradores y tutores las completen en el sistema.
 *
 * <p>Reglas: solo se gestionan asignaturas que el profesor imparte; la pauta se valida completa, informando todos
 * los errores juntos, y se guarda completa en una sola transacción; una pauta que ya tiene evaluaciones aplicadas
 * no se puede modificar ni eliminar (para cambiarla se crea una nueva), porque cambiaría notas ya registradas.
 *
 * <p>Ponderaciones: la de cada pauta es el % que aporta a la nota final de la asignatura, y el conjunto de las
 * pautas de la asignatura (de evaluación y de documento) debe sumar 100. Como las pautas se crean de a una, al
 * guardar solo se bloquea que la suma pase de 100; si queda bajo 100 solo se avisa en pantalla, porque puede
 * faltar cargar otras pautas.
 */
@Service
public class PautaDefinicionService {
    private static final BigDecimal MAXIMO_PONDERACION = new BigDecimal("100");
    private static final BigDecimal TOTAL_ASIGNATURA = new BigDecimal("100");
    private static final BigDecimal MAXIMO_PUNTAJE = new BigDecimal("999.99");
    private static final int DECIMALES_MAX = 2;
    private static final int MAXIMO_NUMERO_APLICACION = 10;
    private static final int LARGO_MAXIMO_NOMBRE_DIMENSION = 200;
    private static final int LARGO_MAXIMO_DESCRIPCION_CRITERIO = 500;
    private static final int LARGO_MAXIMO_NUMERO = 10;

    public record PautaResumen(Long idPauta, TipoLinea tipoLinea, int numeroAplicacion,
                               BigDecimal ponderacion, int cantidadCriterios, boolean enUso) {}

    /**
     * {@code totalPonderacion} suma las pautas de evaluación y de documento de la asignatura. Si es 0 (por
     * ejemplo, una asignatura sin pautas) no se muestra ningún aviso.
     */
    public record AsignaturaConPautas(Long idAsignatura, String nombre, List<PautaResumen> pautas,
                                      BigDecimal totalPonderacion) {
        public boolean tieneTotal() {
            return totalPonderacion.signum() > 0;
        }

        /** Aviso, no bloqueo: aún falta ponderación para llegar a 100. */
        public boolean incompleto() {
            return tieneTotal() && totalPonderacion.compareTo(TOTAL_ASIGNATURA) < 0;
        }

        public boolean excedido() {
            return totalPonderacion.compareTo(TOTAL_ASIGNATURA) > 0;
        }

        public BigDecimal faltante() {
            return TOTAL_ASIGNATURA.subtract(totalPonderacion);
        }
    }

    /** Una pauta cargada para mostrarla en el formulario; {@code enUso} indica que solo se puede consultar. */
    public record PautaEditable(Long idPauta, Long idAsignatura, String nombreAsignatura,
                                boolean enUso, PautaFormulario formulario) {}

    private record CriterioValidado(String descripcion, BigDecimal puntajeMax) {}

    private record DimensionValidada(String nombre, List<CriterioValidado> criterios) {}

    private record PautaValidada(TipoLinea tipoLinea, int numeroAplicacion, BigDecimal ponderacion,
                                 List<DimensionValidada> dimensiones) {}

    private final PautaEvaluacionRepository pautaRepository;
    private final SeccionAsignaturaRepository seccionRepository;
    private final EvaluacionAplicadaRepository evaluacionRepository;
    private final PautaDocumentoRepository pautaDocumentoRepository;

    public PautaDefinicionService(PautaEvaluacionRepository pautaRepository,
                                  SeccionAsignaturaRepository seccionRepository,
                                  EvaluacionAplicadaRepository evaluacionRepository,
                                  PautaDocumentoRepository pautaDocumentoRepository) {
        this.pautaRepository = pautaRepository;
        this.seccionRepository = seccionRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.pautaDocumentoRepository = pautaDocumentoRepository;
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

    /**
     * Carga una pauta en el formulario.
     *
     * @throws ResponseStatusException 404 si la pauta no existe, 403 si su asignatura no es del profesor
     */
    @Transactional(readOnly = true)
    public PautaEditable obtenerParaEditar(Long idProfesor, Long idPauta) {
        PautaEvaluacion pauta = pautaDelProfesor(idProfesor, idPauta);
        return new PautaEditable(
                pauta.getIdPautaEvaluacion(),
                pauta.getAsignatura().getIdAsignatura(),
                pauta.getAsignatura().getNombre(),
                enUso(pauta),
                armarFormulario(pauta));
    }

    // ---------- escritura ----------

    /**
     * Crea la pauta completa (con sus dimensiones y criterios) en una sola transacción.
     *
     * @return el id de la pauta creada
     * @throws ResponseStatusException 403 si el profesor no imparte la asignatura
     * @throws PautaInvalidaException  la pauta tiene errores; vienen todos juntos
     */
    @Transactional
    public Long crear(Long idProfesor, Long idAsignatura, PautaFormulario formulario) {
        Asignatura asignatura = asignaturaDelProfesor(idProfesor, idAsignatura);
        PautaValidada datos = validar(formulario, idAsignatura, null);

        PautaEvaluacion pauta = new PautaEvaluacion(
                asignatura, datos.tipoLinea(), datos.numeroAplicacion(), datos.ponderacion());
        reemplazarDimensiones(pauta, datos);
        return pautaRepository.save(pauta).getIdPautaEvaluacion();
    }

    /**
     * Reemplaza los datos, dimensiones y criterios de la pauta en una sola transacción.
     *
     * @throws ResponseStatusException 404 si la pauta no existe, 403 si su asignatura no es del profesor
     * @throws IllegalStateException   la pauta ya tiene evaluaciones aplicadas
     * @throws PautaInvalidaException  la pauta tiene errores; vienen todos juntos
     */
    @Transactional
    public void actualizar(Long idProfesor, Long idPauta, PautaFormulario formulario) {
        PautaEvaluacion pauta = pautaDelProfesor(idProfesor, idPauta);
        exigirSinEvaluaciones(pauta);
        PautaValidada datos = validar(formulario, pauta.getAsignatura().getIdAsignatura(), idPauta);

        pauta.setTipoLinea(datos.tipoLinea());
        pauta.setNumeroAplicacion(datos.numeroAplicacion());
        pauta.setPonderacion(datos.ponderacion());
        reemplazarDimensiones(pauta, datos);
        pautaRepository.save(pauta);
    }

    /**
     * @throws ResponseStatusException 404 si la pauta no existe, 403 si su asignatura no es del profesor
     * @throws IllegalStateException   la pauta ya tiene evaluaciones aplicadas
     */
    @Transactional
    public void eliminar(Long idProfesor, Long idPauta) {
        PautaEvaluacion pauta = pautaDelProfesor(idProfesor, idPauta);
        exigirSinEvaluaciones(pauta);
        // Se quita también de la colección de la asignatura para que Hibernate no la vuelva a guardar en cascada.
        pauta.getAsignatura().getPautasEvaluacion().remove(pauta);
        pautaRepository.delete(pauta);
    }

    /**
     * Cambia las dimensiones y criterios aprovechando {@code cascade = ALL} y {@code orphanRemoval = true}:
     * los que ya no están en la colección se borran solos y los nuevos se insertan al guardar la pauta.
     * Es seguro porque una pauta con evaluaciones nunca llega hasta acá.
     */
    private void reemplazarDimensiones(PautaEvaluacion pauta, PautaValidada datos) {
        pauta.getDimensiones().clear();
        int ordenDimension = 1;
        for (DimensionValidada dato : datos.dimensiones()) {
            Dimension dimension = new Dimension(pauta, dato.nombre(), ordenDimension++);
            int ordenCriterio = 1;
            for (CriterioValidado criterio : dato.criterios()) {
                dimension.getCriterios().add(new Criterio(
                        dimension, criterio.descripcion(), criterio.puntajeMax(), ordenCriterio++));
            }
            pauta.getDimensiones().add(dimension);
        }
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

    private boolean enUso(PautaEvaluacion pauta) {
        return evaluacionRepository.existsByPauta(pauta.getIdPautaEvaluacion());
    }

    private void exigirSinEvaluaciones(PautaEvaluacion pauta) {
        if (enUso(pauta)) {
            throw new IllegalStateException(
                    "Esta pauta ya tiene evaluaciones aplicadas y no se puede modificar. "
                            + "Para cambiarla, crea una pauta nueva");
        }
    }

    // ---------- validación: todos los errores juntos ----------

    private PautaValidada validar(PautaFormulario formulario, Long idAsignatura, Long idPautaExcluida) {
        List<String> errores = new ArrayList<>();

        TipoLinea tipo = parsearTipoLinea(formulario.getTipoLinea(), errores);
        Integer numero = parsearNumeroAplicacion(formulario.getNumeroAplicacion(), errores);
        BigDecimal ponderacion = parsearDecimal(
                formulario.getPonderacion(), "La ponderación", MAXIMO_PONDERACION, errores);

        // Las demás pautas de la asignatura (evaluación y documento) más esta no pueden pasar de 100.
        if (ponderacion != null) {
            BigDecimal otras = totalPonderaciones(idAsignatura, idPautaExcluida);
            if (otras.add(ponderacion).compareTo(TOTAL_ASIGNATURA) > 0) {
                BigDecimal disponible = TOTAL_ASIGNATURA.subtract(otras).max(BigDecimal.ZERO);
                errores.add("Las demás pautas de la asignatura (de evaluación y de documento) ya suman "
                        + comoTexto(otras) + " %, así que esta pauta puede tener como máximo "
                        + comoTexto(disponible) + " %");
            }
        }

        // La tabla no tiene restricción única: la regla se aplica acá.
        if (tipo != null && numero != null && existeOtraPauta(idAsignatura, tipo, numero, idPautaExcluida)) {
            errores.add("Ya existe una pauta de tipo " + tipo + " para la aplicación " + numero
                    + " de esta asignatura");
        }

        List<DimensionValidada> dimensiones = new ArrayList<>();
        List<DimensionFormulario> filas = formulario.getDimensiones();
        if (filas == null || filas.isEmpty()) {
            errores.add("La pauta necesita al menos una dimensión");
        } else {
            for (int i = 0; i < filas.size(); i++) {
                DimensionValidada dimension = validarDimension(filas.get(i), i + 1, errores);
                if (dimension != null) {
                    dimensiones.add(dimension);
                }
            }
        }

        if (!errores.isEmpty()) {
            throw new PautaInvalidaException(errores);
        }
        return new PautaValidada(tipo, numero, ponderacion, dimensiones);
    }

    private DimensionValidada validarDimension(DimensionFormulario fila, int numero, List<String> errores) {
        if (fila == null) {
            errores.add("La dimensión " + numero + " no tiene datos");
            return null;
        }
        String nombre = limpiarTexto(
                fila.getNombre(), "El nombre de la dimensión " + numero, LARGO_MAXIMO_NOMBRE_DIMENSION, errores);

        List<CriterioValidado> criterios = new ArrayList<>();
        List<CriterioFormulario> filasCriterio = fila.getCriterios();
        if (filasCriterio == null || filasCriterio.isEmpty()) {
            errores.add("La dimensión " + numero + " necesita al menos un criterio");
        } else {
            for (int j = 0; j < filasCriterio.size(); j++) {
                CriterioFormulario criterio = filasCriterio.get(j);
                String donde = "criterio " + (j + 1) + " de la dimensión " + numero;
                if (criterio == null) {
                    errores.add("El " + donde + " no tiene datos");
                    continue;
                }
                String descripcion = limpiarTexto(criterio.getDescripcion(),
                        "La descripción del " + donde, LARGO_MAXIMO_DESCRIPCION_CRITERIO, errores);
                BigDecimal puntaje = parsearDecimal(criterio.getPuntajeMax(),
                        "El puntaje máximo del " + donde, MAXIMO_PUNTAJE, errores);
                if (descripcion != null && puntaje != null) {
                    criterios.add(new CriterioValidado(descripcion, puntaje));
                }
            }
        }
        return nombre == null ? null : new DimensionValidada(nombre, criterios);
    }

    private boolean existeOtraPauta(Long idAsignatura, TipoLinea tipo, int numero, Long idPautaExcluida) {
        return pautaRepository.findByAsignatura(idAsignatura).stream()
                .filter(p -> !Objects.equals(p.getIdPautaEvaluacion(), idPautaExcluida))
                .anyMatch(p -> p.getTipoLinea() == tipo && Objects.equals(p.getNumeroAplicacion(), numero));
    }

    /**
     * Suma las ponderaciones de todas las pautas de la asignatura: las de evaluación (menos la que se está
     * editando, cuyo valor se reemplaza) y las de documento.
     */
    private BigDecimal totalPonderaciones(Long idAsignatura, Long idPautaExcluida) {
        BigDecimal evaluaciones = pautaRepository.findByAsignatura(idAsignatura).stream()
                .filter(p -> !Objects.equals(p.getIdPautaEvaluacion(), idPautaExcluida))
                .map(PautaEvaluacion::getPonderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal documentos = pautaDocumentoRepository
                .findByAsignatura_IdAsignaturaOrderByTipoAscNumeroAsc(idAsignatura).stream()
                .map(PautaDocumento::getPonderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return evaluaciones.add(documentos);
    }

    private TipoLinea parsearTipoLinea(String texto, List<String> errores) {
        if (texto == null || texto.isBlank()) {
            errores.add("El tipo de línea es obligatorio");
            return null;
        }
        try {
            return TipoLinea.valueOf(texto.strip());
        } catch (IllegalArgumentException e) {
            errores.add("El tipo de línea no es válido");
            return null;
        }
    }

    private Integer parsearNumeroAplicacion(String texto, List<String> errores) {
        if (texto == null || texto.isBlank()) {
            errores.add("El número de aplicación es obligatorio");
            return null;
        }
        int numero;
        try {
            numero = Integer.parseInt(texto.strip());
        } catch (NumberFormatException e) {
            errores.add("El número de aplicación debe ser un número entero");
            return null;
        }
        if (numero < 1 || numero > MAXIMO_NUMERO_APLICACION) {
            errores.add("El número de aplicación debe estar entre 1 y " + MAXIMO_NUMERO_APLICACION);
            return null;
        }
        return numero;
    }

    /** Número mayor que 0 y hasta {@code maximo}, con hasta dos decimales (coma o punto). */
    private BigDecimal parsearDecimal(String texto, String sujeto, BigDecimal maximo, List<String> errores) {
        if (texto == null || texto.isBlank()) {
            errores.add(sujeto + " es un campo obligatorio");
            return null;
        }
        String limpio = texto.strip().replace(',', '.');
        BigDecimal valor;
        try {
            if (limpio.length() > LARGO_MAXIMO_NUMERO) {
                throw new NumberFormatException("texto demasiado largo");
            }
            valor = new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            errores.add(sujeto + " debe ser un número, por ejemplo 12,5");
            return null;
        }
        if (valor.signum() <= 0 || valor.compareTo(maximo) > 0) {
            errores.add(sujeto + " debe ser mayor que 0 y como máximo "
                    + maximo.stripTrailingZeros().toPlainString());
            return null;
        }
        if (valor.stripTrailingZeros().scale() > DECIMALES_MAX) {
            errores.add(sujeto + " admite hasta " + DECIMALES_MAX + " decimales");
            return null;
        }
        return valor.setScale(DECIMALES_MAX, RoundingMode.UNNECESSARY);
    }

    private String limpiarTexto(String texto, String sujeto, int largoMaximo, List<String> errores) {
        if (texto == null || texto.isBlank()) {
            errores.add(sujeto + " es un campo obligatorio");
            return null;
        }
        String limpio = texto.strip();
        if (limpio.length() > largoMaximo) {
            errores.add(sujeto + " admite como máximo " + largoMaximo + " caracteres");
            return null;
        }
        return limpio;
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
        return new AsignaturaConPautas(asignatura.getIdAsignatura(), asignatura.getNombre(), pautas,
                totalPonderaciones(asignatura.getIdAsignatura(), null));
    }

    private PautaFormulario armarFormulario(PautaEvaluacion pauta) {
        PautaFormulario formulario = new PautaFormulario();
        formulario.setTipoLinea(pauta.getTipoLinea().name());
        formulario.setNumeroAplicacion(String.valueOf(pauta.getNumeroAplicacion()));
        formulario.setPonderacion(comoTexto(pauta.getPonderacion()));

        List<DimensionFormulario> filas = new ArrayList<>();
        pauta.getDimensiones().stream()
                .sorted(Comparator.comparing(Dimension::getOrden))
                .forEach(dimension -> {
                    DimensionFormulario fila = new DimensionFormulario();
                    fila.setNombre(dimension.getNombre());
                    dimension.getCriterios().stream()
                            .sorted(Comparator.comparing(Criterio::getOrden))
                            .forEach(criterio -> {
                                CriterioFormulario filaCriterio = new CriterioFormulario();
                                filaCriterio.setDescripcion(criterio.getDescripcion());
                                filaCriterio.setPuntajeMax(comoTexto(criterio.getPuntajeMax()));
                                fila.getCriterios().add(filaCriterio);
                            });
                    filas.add(fila);
                });
        formulario.setDimensiones(filas);
        return formulario;
    }

    /** 30.50 se muestra como "30,5" y 100.00 como "100", como se escribe en el formulario. */
    private String comoTexto(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
