package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.InscripcionFormulario;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.LineaFormulario;
import com.grupo1.isw.practicas_pedagogicas.repositories.InscripcionAdminRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.InscripcionAdminRepository.InscripcionFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.InscripcionAdminRepository.Opcion;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository.SeccionDatos;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/** N-02: el coordinador inscribe estudiantes en una sección y asigna, si corresponde, colaborador y tutor en cada línea. */
@Service
public class CoordinadorInscripcionService {
    private final InscripcionAdminRepository inscripciones;
    private final SeccionEstructuraRepository secciones;

    public CoordinadorInscripcionService(InscripcionAdminRepository inscripciones,
                                         SeccionEstructuraRepository secciones) {
        this.inscripciones = inscripciones;
        this.secciones = secciones;
    }

    /** Líneas que debe tener cada estudiante según la asignatura: una sola, o especialidad y orientación. */
    public static List<String> tiposDeLinea(int numeroLineas) {
        return numeroLineas >= 2 ? List.of("ESPECIALIDAD", "ORIENTACION") : List.of("UNICA");
    }

    public SeccionDatos seccion(long idSeccion) {
        return secciones.buscar(idSeccion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<InscripcionFila> listar(long idSeccion) {
        seccion(idSeccion);
        return inscripciones.listar(idSeccion);
    }

    public List<Opcion> estudiantesDisponibles(long idSeccion) {
        return inscripciones.estudiantesSinInscribir(idSeccion);
    }

    public List<Opcion> colaboradores() {
        return inscripciones.personasActivas("COLABORADOR");
    }

    public List<Opcion> tutores() {
        return inscripciones.personasActivas("TUTOR");
    }

    public List<Opcion> centros() {
        return inscripciones.centros();
    }

    /** Formulario en blanco con una fila por cada línea que exige la asignatura. */
    public InscripcionFormulario formularioVacio(long idSeccion) {
        InscripcionFormulario formulario = new InscripcionFormulario();
        for (String tipo : tiposDeLinea(seccion(idSeccion).numeroLineas())) {
            formulario.getLineas().add(new LineaFormulario(tipo, null, null));
        }
        return formulario;
    }

    @Transactional(readOnly = true)
    public InscripcionFormulario formularioDe(long idSeccion, long idInscripcion) {
        InscripcionFila inscripcion = delaSeccion(idSeccion, idInscripcion);
        InscripcionFormulario formulario = formularioVacio(idSeccion);
        formulario.setIdEstudiante(inscripcion.idEstudiante());
        formulario.setIdCentro(inscripcion.idCentro());
        for (LineaFormulario fila : formulario.getLineas()) {
            inscripcion.lineas().stream().filter(l -> l.tipo().equals(fila.getTipo())).findFirst().ifPresent(l -> {
                fila.setIdColaborador(l.idColaborador());
                fila.setIdTutor(l.idTutor());
            });
        }
        return formulario;
    }

    public InscripcionFila inscripcion(long idSeccion, long idInscripcion) {
        return delaSeccion(idSeccion, idInscripcion);
    }

    @Transactional
    public long inscribir(long idSeccion, InscripcionFormulario formulario) {
        SeccionDatos seccion = seccion(idSeccion);
        List<String> errores = new ArrayList<>();
        if (formulario.getIdEstudiante() == null) {
            errores.add("Debes elegir un estudiante");
        } else if (!inscripciones.existePersonaActiva(formulario.getIdEstudiante(), "ESTUDIANTE")) {
            errores.add("El estudiante elegido no existe o está desactivado");
        } else if (inscripciones.estaInscrito(formulario.getIdEstudiante(), idSeccion)) {
            errores.add("El estudiante ya está inscrito en esta sección");
        }
        validarCentroYLineas(seccion, formulario, errores);
        if (!errores.isEmpty()) {
            throw new EstructuraInvalidaException(errores);
        }

        long idInscripcion = inscripciones.insertarInscripcion(formulario.getIdEstudiante(), idSeccion,
                formulario.getIdCentro());
        guardarLineas(idInscripcion, seccion, formulario);
        return idInscripcion;
    }

    /** Cambia el centro y los profesores de un estudiante ya inscrito (el estudiante no se puede cambiar). */
    @Transactional
    public void actualizar(long idSeccion, long idInscripcion, InscripcionFormulario formulario) {
        SeccionDatos seccion = seccion(idSeccion);
        delaSeccion(idSeccion, idInscripcion);
        List<String> errores = new ArrayList<>();
        validarCentroYLineas(seccion, formulario, errores);
        if (!errores.isEmpty()) {
            throw new EstructuraInvalidaException(errores);
        }
        inscripciones.cambiarCentro(idInscripcion, formulario.getIdCentro());
        guardarLineas(idInscripcion, seccion, formulario);
    }

    /** Quita al estudiante de la sección, solo si todavía no tiene nada evaluado ni entregado. */
    @Transactional
    public void quitar(long idSeccion, long idInscripcion) {
        InscripcionFila inscripcion = delaSeccion(idSeccion, idInscripcion);
        if (inscripciones.tieneDatosEvaluativos(idInscripcion)) {
            throw new IllegalStateException("No se puede quitar a " + inscripcion.estudiante()
                    + ": ya tiene entregas, evaluaciones, planificaciones o informe");
        }
        inscripciones.eliminar(idInscripcion);
    }

    private void validarCentroYLineas(SeccionDatos seccion, InscripcionFormulario formulario, List<String> errores) {
        if (formulario.getIdCentro() == null) {
            errores.add("Debes elegir el centro de práctica");
        } else if (!inscripciones.existeCentro(formulario.getIdCentro())) {
            errores.add("El centro de práctica elegido no existe");
        }

        List<String> esperados = tiposDeLinea(seccion.numeroLineas());
        List<LineaFormulario> lineas = formulario.getLineas() == null ? List.of() : formulario.getLineas();
        for (String tipo : esperados) {
            LineaFormulario linea = lineas.stream().filter(l -> tipo.equals(l.getTipo())).findFirst().orElse(null);
            String nombre = linea == null ? tipo : linea.etiquetaTipo();
            if (linea == null) {
                continue;
            }
            // El colaborador y el tutor son opcionales: algunas asignaturas no cuentan con uno de ellos.
            if (linea.getIdColaborador() != null
                    && !inscripciones.existePersonaActiva(linea.getIdColaborador(), "COLABORADOR")) {
                errores.add("El profesor colaborador de la línea " + nombre + " no existe o está desactivado");
            }
            if (linea.getIdTutor() != null && !inscripciones.existePersonaActiva(linea.getIdTutor(), "TUTOR")) {
                errores.add("El tutor de la línea " + nombre + " no existe o está desactivado");
            }
        }
    }

    /** Solo guarda las líneas que la asignatura exige, aunque el formulario traiga otras. */
    private void guardarLineas(long idInscripcion, SeccionDatos seccion, InscripcionFormulario formulario) {
        List<String> esperados = tiposDeLinea(seccion.numeroLineas());
        for (LineaFormulario linea : formulario.getLineas()) {
            if (!esperados.contains(linea.getTipo())) {
                continue;
            }
            inscripciones.guardarLinea(idInscripcion, linea.getTipo(), linea.getIdColaborador(), linea.getIdTutor());
        }
    }

    private InscripcionFila delaSeccion(long idSeccion, long idInscripcion) {
        InscripcionFila inscripcion = inscripciones.buscar(idInscripcion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscripción no encontrada"));
        if (!inscripcion.idSeccion().equals(idSeccion)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La inscripción no es de esta sección");
        }
        return inscripcion;
    }
}
