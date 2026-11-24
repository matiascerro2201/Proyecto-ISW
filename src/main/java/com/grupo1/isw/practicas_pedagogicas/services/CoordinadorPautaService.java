package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.PautaSemestreFormulario;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaSemestreRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaSemestreRepository.DimensionFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaSemestreRepository.PautaFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository.SeccionDatos;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * N-04: el coordinador define las pautas del semestre: quién las aplica (colaborador o tutor), a qué línea
 * corresponden, si son la aplicación A o B, su ponderación y su contenido (dimensiones y criterios).
 */
@Service
public class CoordinadorPautaService {
    static final int LARGO_NOMBRE = 150;
    static final int LARGO_DIMENSION = 200;
    static final int LARGO_CRITERIO = 500;
    private static final BigDecimal PUNTAJE_MAXIMO_CRITERIO = new BigDecimal("999.99");

    /** Una pauta con su contenido. */
    public record DetallePauta(SeccionDatos seccion, PautaFila pauta, List<DimensionFila> dimensiones) {
    }

    private final PautaSemestreRepository pautas;
    private final SeccionEstructuraRepository secciones;

    public CoordinadorPautaService(PautaSemestreRepository pautas, SeccionEstructuraRepository secciones) {
        this.pautas = pautas;
        this.secciones = secciones;
    }

    public SeccionDatos seccion(long idSeccion) {
        return secciones.buscar(idSeccion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada"));
    }

    /** Líneas que admite la asignatura de la sección: una sola, o especialidad y orientación. */
    public List<String> lineasDisponibles(long idSeccion) {
        return CoordinadorInscripcionService.tiposDeLinea(seccion(idSeccion).numeroLineas());
    }

    @Transactional(readOnly = true)
    public DetallePauta detalle(long idSeccion, long idPauta) {
        SeccionDatos seccion = seccion(idSeccion);
        PautaFila pauta = delaSeccion(idSeccion, idPauta);
        return new DetallePauta(seccion, pauta, pautas.dimensiones(idPauta));
    }

    public PautaSemestreFormulario formularioDe(long idSeccion, long idPauta) {
        PautaFila pauta = delaSeccion(idSeccion, idPauta);
        PautaSemestreFormulario formulario = new PautaSemestreFormulario();
        formulario.setNombre(pauta.nombre());
        formulario.setEvaluador(pauta.evaluador());
        formulario.setTipoLinea(pauta.tipoLinea());
        formulario.setAplicacion(String.valueOf(pauta.aplicacion()));
        formulario.setPonderacion(pauta.ponderacion().stripTrailingZeros().toPlainString());
        return formulario;
    }

    @Transactional
    public long crear(long idSeccion, PautaSemestreFormulario formulario) {
        SeccionDatos seccion = seccion(idSeccion);
        exigirEditable(seccion);
        Datos datos = validar(seccion, formulario, null);
        return pautas.insertar(seccion.idAsignatura(), idSeccion, datos.nombre, datos.evaluador, datos.tipoLinea,
                datos.aplicacion, datos.ponderacion, true, null);
    }

    @Transactional
    public void actualizar(long idSeccion, long idPauta, PautaSemestreFormulario formulario) {
        SeccionDatos seccion = seccion(idSeccion);
        exigirEditable(seccion);
        delaSeccion(idSeccion, idPauta);
        exigirSinEvaluaciones(idPauta);
        Datos datos = validar(seccion, formulario, idPauta);
        pautas.actualizar(idPauta, datos.nombre, datos.evaluador, datos.tipoLinea, datos.aplicacion, datos.ponderacion);
    }

    @Transactional
    public void cambiarHabilitada(long idSeccion, long idPauta, boolean habilitada) {
        exigirEditable(seccion(idSeccion));
        delaSeccion(idSeccion, idPauta);
        pautas.cambiarHabilitada(idPauta, habilitada);
    }

    @Transactional
    public void eliminar(long idSeccion, long idPauta) {
        exigirEditable(seccion(idSeccion));
        delaSeccion(idSeccion, idPauta);
        exigirSinEvaluaciones(idPauta);
        pautas.eliminar(idPauta);
    }

    // ---- contenido de la pauta

    @Transactional
    public void agregarDimension(long idSeccion, long idPauta, String nombre) {
        exigirEditable(seccion(idSeccion));
        delaSeccion(idSeccion, idPauta);
        exigirSinEvaluaciones(idPauta);
        String limpio = nombre == null ? "" : nombre.trim();
        List<String> errores = new ArrayList<>();
        if (limpio.isEmpty()) {
            errores.add("El nombre de la dimensión es obligatorio");
        } else if (limpio.length() > LARGO_DIMENSION) {
            errores.add("El nombre de la dimensión no puede superar " + LARGO_DIMENSION + " caracteres");
        }
        lanzarSiHayErrores(errores);
        pautas.insertarDimension(idPauta, limpio);
    }

    @Transactional
    public void eliminarDimension(long idSeccion, long idPauta, long idDimension) {
        exigirEditable(seccion(idSeccion));
        delaSeccion(idSeccion, idPauta);
        exigirSinEvaluaciones(idPauta);
        exigirDimensionDe(idPauta, idDimension);
        pautas.eliminarDimension(idDimension);
    }

    @Transactional
    public void agregarCriterio(long idSeccion, long idPauta, long idDimension, String descripcion, String puntaje) {
        exigirEditable(seccion(idSeccion));
        delaSeccion(idSeccion, idPauta);
        exigirSinEvaluaciones(idPauta);
        exigirDimensionDe(idPauta, idDimension);
        String limpia = descripcion == null ? "" : descripcion.trim();
        List<String> errores = new ArrayList<>();
        if (limpia.isEmpty()) {
            errores.add("La descripción del criterio es obligatoria");
        } else if (limpia.length() > LARGO_CRITERIO) {
            errores.add("La descripción del criterio no puede superar " + LARGO_CRITERIO + " caracteres");
        }
        BigDecimal valor = EntradaNumerica.decimal(puntaje, "El puntaje máximo", errores);
        if (valor != null && (valor.signum() <= 0 || valor.compareTo(PUNTAJE_MAXIMO_CRITERIO) > 0)) {
            errores.add("El puntaje máximo debe ser mayor que 0 y no superar 999,99");
        }
        lanzarSiHayErrores(errores);
        pautas.insertarCriterio(idDimension, limpia, valor);
    }

    @Transactional
    public void eliminarCriterio(long idSeccion, long idPauta, long idDimension, long idCriterio) {
        exigirEditable(seccion(idSeccion));
        delaSeccion(idSeccion, idPauta);
        exigirSinEvaluaciones(idPauta);
        exigirDimensionDe(idPauta, idDimension);
        long dimensionDelCriterio = pautas.dimensionDeCriterio(idCriterio)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Criterio no encontrado"));
        if (dimensionDelCriterio != idDimension) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El criterio no es de esa dimensión");
        }
        pautas.eliminarCriterio(idCriterio);
    }

    // ---- validación

    private record Datos(String nombre, String evaluador, String tipoLinea, int aplicacion, BigDecimal ponderacion) {
    }

    private Datos validar(SeccionDatos seccion, PautaSemestreFormulario formulario, Long idExcluida) {
        List<String> errores = new ArrayList<>();

        String nombre = formulario.getNombre() == null ? "" : formulario.getNombre().trim();
        if (nombre.isEmpty()) {
            errores.add("El nombre de la pauta es obligatorio");
        } else if (nombre.length() > LARGO_NOMBRE) {
            errores.add("El nombre de la pauta no puede superar " + LARGO_NOMBRE + " caracteres");
        }

        String evaluador = formulario.getEvaluador();
        if (!"COLABORADOR".equals(evaluador) && !"TUTOR".equals(evaluador)) {
            errores.add("Debes elegir quién aplica la pauta: profesor colaborador o tutor");
            evaluador = null;
        }

        String tipoLinea = formulario.getTipoLinea();
        if (tipoLinea == null || !CoordinadorInscripcionService.tiposDeLinea(seccion.numeroLineas()).contains(tipoLinea)) {
            errores.add("Debes elegir una línea válida para esta asignatura");
            tipoLinea = null;
        }

        int aplicacion = 0;
        if ("1".equals(formulario.getAplicacion()) || "2".equals(formulario.getAplicacion())) {
            aplicacion = Integer.parseInt(formulario.getAplicacion());
        } else {
            errores.add("La aplicación debe ser A o B");
        }

        BigDecimal ponderacion = EntradaNumerica.ponderacion(formulario.getPonderacion(), errores);

        if (evaluador != null && tipoLinea != null && aplicacion != 0
                && pautas.existe(seccion.id(), evaluador, tipoLinea, aplicacion, idExcluida)) {
            errores.add("Ya existe una pauta de ese evaluador, línea y aplicación en este semestre");
        }
        lanzarSiHayErrores(errores);
        return new Datos(nombre, evaluador, tipoLinea, aplicacion, ponderacion);
    }

    private void lanzarSiHayErrores(List<String> errores) {
        if (!errores.isEmpty()) {
            throw new EstructuraInvalidaException(errores);
        }
    }

    private void exigirEditable(SeccionDatos seccion) {
        if (seccion.iniciada()) {
            throw new IllegalStateException("El semestre ya fue iniciado; la estructura está congelada");
        }
    }

    private void exigirSinEvaluaciones(long idPauta) {
        if (pautas.tieneEvaluacionesAplicadas(idPauta)) {
            throw new IllegalStateException("La pauta ya tiene evaluaciones aplicadas y no se puede cambiar");
        }
    }

    private void exigirDimensionDe(long idPauta, long idDimension) {
        long pautaDeLaDimension = pautas.pautaDeDimension(idDimension)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dimensión no encontrada"));
        if (pautaDeLaDimension != idPauta) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La dimensión no es de esa pauta");
        }
    }

    private PautaFila delaSeccion(long idSeccion, long idPauta) {
        PautaFila pauta = pautas.buscar(idPauta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pauta no encontrada"));
        if (!pauta.idSeccion().equals(idSeccion)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La pauta no es de esta sección");
        }
        return pauta;
    }
}
