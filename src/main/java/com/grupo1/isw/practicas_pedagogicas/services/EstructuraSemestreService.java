package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaSemestreRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaSemestreRepository.EntregaFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaSemestreRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaSemestreRepository.PautaFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository.SeccionDatos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * N-05 suma 100 %, N-06 iniciar el semestre, N-07 copiar la estructura de otro semestre
 * y N-08 consulta en solo lectura para el profesor de la asignatura.
 */
@Service
public class EstructuraSemestreService {
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final SeccionEstructuraRepository secciones;
    private final EntregaSemestreRepository entregas;
    private final PautaSemestreRepository pautas;
    private final Clock reloj;

    @Autowired
    public EstructuraSemestreService(SeccionEstructuraRepository secciones, EntregaSemestreRepository entregas,
                                     PautaSemestreRepository pautas) {
        this(secciones, entregas, pautas, Clock.systemDefaultZone());
    }

    public EstructuraSemestreService(SeccionEstructuraRepository secciones, EntregaSemestreRepository entregas,
                                     PautaSemestreRepository pautas, Clock reloj) {
        this.secciones = secciones;
        this.entregas = entregas;
        this.pautas = pautas;
        this.reloj = reloj;
    }

    public SeccionDatos seccion(long idSeccion) {
        return secciones.buscar(idSeccion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada"));
    }

    // ---- consulta

    /** Toda la estructura del semestre, incluso lo deshabilitado, para que el coordinador la administre. */
    @Transactional(readOnly = true)
    public EstructuraVista vista(long idSeccion) {
        SeccionDatos seccion = seccion(idSeccion);
        List<EntregaFila> todasLasEntregas = entregas.listar(idSeccion);
        List<PautaFila> todasLasPautas = pautas.listar(idSeccion);
        return new EstructuraVista(seccion, todasLasEntregas, todasLasPautas,
                sumaHabilitadas(todasLasEntregas, todasLasPautas));
    }

    /** N-08: la estructura como la ve el profesor de la asignatura: solo lo habilitado y sin poder cambiarla. */
    @Transactional(readOnly = true)
    public EstructuraVista vistaParaProfesor(long idProfesor, long idSeccion) {
        SeccionDatos seccion = seccion(idSeccion);
        if (!seccion.idProfesor().equals(idProfesor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No eres el profesor de esta sección");
        }
        List<EntregaFila> habilitadasEntregas = entregas.listar(idSeccion).stream()
                .filter(EntregaFila::habilitada).toList();
        List<PautaFila> habilitadasPautas = pautas.listar(idSeccion).stream()
                .filter(PautaFila::habilitada).toList();
        return new EstructuraVista(seccion, habilitadasEntregas, habilitadasPautas,
                sumaHabilitadas(habilitadasEntregas, habilitadasPautas));
    }

    public List<SeccionDatos> seccionesDelProfesor(long idProfesor) {
        return secciones.delProfesor(idProfesor);
    }

    /** N-05: suma de las ponderaciones de todo lo habilitado. */
    public BigDecimal sumaHabilitadas(List<EntregaFila> listaEntregas, List<PautaFila> listaPautas) {
        BigDecimal suma = BigDecimal.ZERO;
        for (EntregaFila e : listaEntregas) {
            if (e.habilitada()) {
                suma = suma.add(e.ponderacion());
            }
        }
        for (PautaFila p : listaPautas) {
            if (p.habilitada()) {
                suma = suma.add(p.ponderacion());
            }
        }
        return suma;
    }

    // ---- N-06 iniciar

    /**
     * Inicia el semestre y congela la estructura. Exige que lo habilitado sume exactamente 100 % y que cada pauta
     * habilitada tenga al menos un criterio.
     *
     * @throws IllegalStateException    si el semestre ya estaba iniciado
     * @throws EstructuraInvalidaException con todo lo que falta corregir
     */
    @Transactional
    public void iniciar(long idSeccion) {
        SeccionDatos seccion = seccion(idSeccion);
        if (seccion.iniciada()) {
            throw new IllegalStateException("El semestre ya fue iniciado");
        }
        List<EntregaFila> listaEntregas = entregas.listar(idSeccion);
        List<PautaFila> listaPautas = pautas.listar(idSeccion);

        List<String> errores = new ArrayList<>();
        long habilitadas = listaEntregas.stream().filter(EntregaFila::habilitada).count()
                + listaPautas.stream().filter(PautaFila::habilitada).count();
        if (habilitadas == 0) {
            errores.add("El semestre no tiene ninguna evaluación habilitada");
        }
        BigDecimal suma = sumaHabilitadas(listaEntregas, listaPautas);
        if (suma.compareTo(CIEN) != 0) {
            errores.add("Las ponderaciones habilitadas suman " + suma.stripTrailingZeros().toPlainString()
                    + " % y deben sumar exactamente 100 %");
        }
        for (PautaFila pauta : listaPautas) {
            if (pauta.habilitada() && pauta.criterios() == 0) {
                errores.add("La pauta «" + pauta.nombre() + "» no tiene criterios");
            }
        }
        if (!errores.isEmpty()) {
            throw new EstructuraInvalidaException(errores);
        }
        secciones.marcarIniciada(idSeccion, LocalDateTime.now(reloj));
    }

    // ---- N-07 copiar

    /** Semestres anteriores (u otros) de la misma asignatura desde los que se puede copiar. */
    public List<SeccionDatos> origenesParaCopiar(long idSeccion) {
        SeccionDatos seccion = seccion(idSeccion);
        return secciones.conEstructuraDeLaAsignatura(seccion.idAsignatura(), idSeccion);
    }

    /**
     * Copia entregas y pautas (con dimensiones y criterios) de otro semestre de la misma asignatura. Las fechas
     * límite no se copian: se definen para cada semestre. Solo sirve en un semestre sin iniciar y todavía vacío.
     */
    @Transactional
    public void copiarDe(long idSeccionDestino, long idSeccionOrigen) {
        SeccionDatos destino = seccion(idSeccionDestino);
        if (destino.iniciada()) {
            throw new IllegalStateException("El semestre ya fue iniciado; su estructura no se puede cambiar");
        }
        if (idSeccionDestino == idSeccionOrigen) {
            throw new IllegalStateException("Elige otro semestre del cual copiar");
        }
        SeccionDatos origen = seccion(idSeccionOrigen);
        if (!origen.idAsignatura().equals(destino.idAsignatura())) {
            throw new IllegalStateException("Solo se puede copiar desde un semestre de la misma asignatura");
        }
        if (!entregas.listar(idSeccionDestino).isEmpty() || !pautas.listar(idSeccionDestino).isEmpty()) {
            throw new IllegalStateException(
                    "Este semestre ya tiene estructura. Elimina sus entregas y pautas antes de copiar otra");
        }
        int copiadas = entregas.copiarDe(idSeccionOrigen, idSeccionDestino)
                + pautas.copiarDe(idSeccionOrigen, idSeccionDestino, destino.idAsignatura());
        if (copiadas == 0) {
            throw new IllegalStateException("El semestre elegido no tiene estructura para copiar");
        }
    }
}
