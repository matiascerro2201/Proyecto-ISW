package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository;
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

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * N-09: el coordinador o el profesor de la asignatura fijan la fecha límite de cada entrega y de cada pauta del
 * semestre. A diferencia del resto de la estructura, las fechas se pueden cambiar aunque el semestre ya esté iniciado.
 */
@Service
public class FechaLimiteService {

    public record FechasVista(SeccionDatos seccion, List<EntregaFila> entregas, List<PautaFila> pautas) {
    }

    private final SeccionEstructuraRepository secciones;
    private final EntregaSemestreRepository entregas;
    private final PautaSemestreRepository pautas;
    private final EntregaEstudianteRepository entregasEstudiantes;
    private final Clock reloj;

    @Autowired
    public FechaLimiteService(SeccionEstructuraRepository secciones, EntregaSemestreRepository entregas,
                              PautaSemestreRepository pautas, EntregaEstudianteRepository entregasEstudiantes) {
        this(secciones, entregas, pautas, entregasEstudiantes, Clock.systemDefaultZone());
    }

    public FechaLimiteService(SeccionEstructuraRepository secciones, EntregaSemestreRepository entregas,
                              PautaSemestreRepository pautas, EntregaEstudianteRepository entregasEstudiantes,
                              Clock reloj) {
        this.secciones = secciones;
        this.entregas = entregas;
        this.pautas = pautas;
        this.entregasEstudiantes = entregasEstudiantes;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public FechasVista vista(long idUsuario, boolean esCoordinador, long idSeccion) {
        SeccionDatos seccion = seccionAutorizada(idUsuario, esCoordinador, idSeccion);
        return new FechasVista(seccion,
                entregas.listar(idSeccion).stream().filter(EntregaFila::habilitada).toList(),
                pautas.listar(idSeccion).stream().filter(PautaFila::habilitada).toList());
    }

    /**
     * Fija (o quita, si viene vacía) la fecha límite de una entrega. Como el plazo se mueve a hoy o más adelante,
     * las notas 1,0 automáticas que ya se hubieran puesto por plazo vencido dejan de corresponder y se quitan.
     *
     * @throws IllegalArgumentException si la fecha no es válida o es anterior a hoy
     */
    @Transactional
    public void fijarFechaEntrega(long idUsuario, boolean esCoordinador, long idSeccion, long idEntrega, String fecha) {
        seccionAutorizada(idUsuario, esCoordinador, idSeccion);
        EntregaFila entrega = entregas.buscar(idEntrega)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrega no encontrada"));
        if (!entrega.idSeccion().equals(idSeccion)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La entrega no es de esta sección");
        }
        LocalDate nueva = leer(fecha);
        entregas.cambiarFechaLimite(idEntrega, nueva);
        entregasEstudiantes.eliminarNotasAutomaticas(idEntrega);
    }

    /** Fija (o quita) la fecha límite de una pauta de colaborador o tutor. */
    @Transactional
    public void fijarFechaPauta(long idUsuario, boolean esCoordinador, long idSeccion, long idPauta, String fecha) {
        seccionAutorizada(idUsuario, esCoordinador, idSeccion);
        PautaFila pauta = pautas.buscar(idPauta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pauta no encontrada"));
        if (!pauta.idSeccion().equals(idSeccion)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La pauta no es de esta sección");
        }
        pautas.cambiarFechaLimite(idPauta, leer(fecha));
    }

    private LocalDate leer(String texto) {
        LocalDate fecha = EntradaNumerica.fecha(texto);
        if (fecha != null && fecha.isBefore(LocalDate.now(reloj))) {
            throw new IllegalArgumentException("La fecha límite no puede ser anterior a hoy");
        }
        return fecha;
    }

    /** El coordinador accede a cualquier semestre; el profesor, solo a los suyos. */
    private SeccionDatos seccionAutorizada(long idUsuario, boolean esCoordinador, long idSeccion) {
        SeccionDatos seccion = secciones.buscar(idSeccion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada"));
        if (!esCoordinador && !seccion.idProfesor().equals(idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No eres el profesor de esta sección");
        }
        return seccion;
    }
}
