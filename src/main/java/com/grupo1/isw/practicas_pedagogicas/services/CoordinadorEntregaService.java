package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.EntregaFormulario;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaSemestreRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaSemestreRepository.EntregaFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository.SeccionDatos;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** N-03: el coordinador define las entregas de un semestre (nombre y ponderación). */
@Service
public class CoordinadorEntregaService {
    static final int LARGO_NOMBRE = 150;

    private final EntregaSemestreRepository entregas;
    private final SeccionEstructuraRepository secciones;

    public CoordinadorEntregaService(EntregaSemestreRepository entregas, SeccionEstructuraRepository secciones) {
        this.entregas = entregas;
        this.secciones = secciones;
    }

    public SeccionDatos seccion(long idSeccion) {
        return secciones.buscar(idSeccion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada"));
    }

    public EntregaFormulario formularioDe(long idSeccion, long idEntrega) {
        EntregaFila entrega = delaSeccion(idSeccion, idEntrega);
        return new EntregaFormulario(entrega.nombre(), entrega.ponderacion().stripTrailingZeros().toPlainString());
    }

    @Transactional
    public long crear(long idSeccion, EntregaFormulario formulario) {
        exigirEditable(seccion(idSeccion));
        List<String> errores = new ArrayList<>();
        String nombre = validarNombre(idSeccion, null, formulario.getNombre(), errores);
        BigDecimal ponderacion = EntradaNumerica.ponderacion(formulario.getPonderacion(), errores);
        if (!errores.isEmpty()) {
            throw new EstructuraInvalidaException(errores);
        }
        return entregas.insertar(idSeccion, nombre, ponderacion, true, entregas.siguienteOrden(idSeccion));
    }

    @Transactional
    public void actualizar(long idSeccion, long idEntrega, EntregaFormulario formulario) {
        exigirEditable(seccion(idSeccion));
        delaSeccion(idSeccion, idEntrega);
        List<String> errores = new ArrayList<>();
        String nombre = validarNombre(idSeccion, idEntrega, formulario.getNombre(), errores);
        BigDecimal ponderacion = EntradaNumerica.ponderacion(formulario.getPonderacion(), errores);
        if (!errores.isEmpty()) {
            throw new EstructuraInvalidaException(errores);
        }
        entregas.actualizar(idEntrega, nombre, ponderacion);
    }

    /** Habilitar o deshabilitar: lo deshabilitado deja de contar en la suma y no se muestra al estudiante. */
    @Transactional
    public void cambiarHabilitada(long idSeccion, long idEntrega, boolean habilitada) {
        exigirEditable(seccion(idSeccion));
        delaSeccion(idSeccion, idEntrega);
        entregas.cambiarHabilitada(idEntrega, habilitada);
    }

    @Transactional
    public void eliminar(long idSeccion, long idEntrega) {
        exigirEditable(seccion(idSeccion));
        EntregaFila entrega = delaSeccion(idSeccion, idEntrega);
        if (entregas.tieneEntregasDeEstudiantes(idEntrega)) {
            throw new IllegalStateException("La entrega «" + entrega.nombre()
                    + "» ya tiene trabajos de estudiantes; deshabilítala en vez de eliminarla");
        }
        entregas.eliminar(idEntrega);
    }

    private String validarNombre(long idSeccion, Long idExcluida, String nombre, List<String> errores) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty()) {
            errores.add("El nombre de la entrega es obligatorio");
        } else if (limpio.length() > LARGO_NOMBRE) {
            errores.add("El nombre de la entrega no puede superar " + LARGO_NOMBRE + " caracteres");
        } else if (entregas.existeNombre(idSeccion, limpio, idExcluida)) {
            errores.add("Ya hay una entrega con el nombre «" + limpio + "» en este semestre");
        }
        return limpio;
    }

    private void exigirEditable(SeccionDatos seccion) {
        if (seccion.iniciada()) {
            throw new IllegalStateException("El semestre ya fue iniciado; la estructura está congelada");
        }
    }

    private EntregaFila delaSeccion(long idSeccion, long idEntrega) {
        EntregaFila entrega = entregas.buscar(idEntrega)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrega no encontrada"));
        if (!entrega.idSeccion().equals(idSeccion)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La entrega no es de esta sección");
        }
        return entrega;
    }
}
