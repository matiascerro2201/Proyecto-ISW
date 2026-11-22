package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.AsignaturaFila;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.AsignaturaFormulario;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.ProfesorOpcion;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.SeccionFila;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.SeccionFormulario;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.RolUsuario;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.repositories.AsignaturaAdminRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.ProfesorAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionAsignaturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * N-01: el coordinador crea, edita, lista y elimina asignaturas y secciones, y asigna el profesor de cada sección.
 *
 * <p>Errores:
 * <ul>
 *   <li>{@link EstructuraInvalidaException}: el formulario tiene errores; vienen todos juntos.</li>
 *   <li>{@link IllegalStateException}: la operación no corresponde por el estado actual (por ejemplo, borrar una
 *       asignatura que tiene secciones). El mensaje es para el usuario.</li>
 *   <li>{@link ResponseStatusException} 404: el recurso no existe.</li>
 * </ul>
 */
@Service
public class CoordinadorAsignaturaService {
    static final int LARGO_NOMBRE = 150;
    /** El año de una sección puede estar hasta este número de años antes o después del actual. */
    static final int RANGO_ANIOS = 5;

    private final AsignaturaAdminRepository asignaturaRepository;
    private final SeccionAsignaturaRepository seccionRepository;
    private final ProfesorAsignaturaRepository profesorRepository;
    private final Clock reloj;

    @Autowired
    public CoordinadorAsignaturaService(AsignaturaAdminRepository asignaturaRepository,
                                        SeccionAsignaturaRepository seccionRepository,
                                        ProfesorAsignaturaRepository profesorRepository) {
        this(asignaturaRepository, seccionRepository, profesorRepository, Clock.systemDefaultZone());
    }

    /** Para pruebas: permite fijar el día actual. */
    public CoordinadorAsignaturaService(AsignaturaAdminRepository asignaturaRepository,
                                        SeccionAsignaturaRepository seccionRepository,
                                        ProfesorAsignaturaRepository profesorRepository,
                                        Clock reloj) {
        this.asignaturaRepository = asignaturaRepository;
        this.seccionRepository = seccionRepository;
        this.profesorRepository = profesorRepository;
        this.reloj = reloj;
    }

    // ---------- consultas ----------

    @Transactional(readOnly = true)
    public List<AsignaturaFila> listarAsignaturas() {
        Map<Long, Long> secciones = aMapa(asignaturaRepository.contarSeccionesPorAsignatura());
        Map<Long, Long> inscripciones = aMapa(asignaturaRepository.contarInscripcionesPorAsignatura());
        return asignaturaRepository.findAllByOrderByNombreAsc().stream()
                .map(a -> new AsignaturaFila(a.getIdAsignatura(), a.getNombre(), a.getCiclo(),
                        a.getNumeroLineas(),
                        secciones.getOrDefault(a.getIdAsignatura(), 0L),
                        inscripciones.getOrDefault(a.getIdAsignatura(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SeccionFila> listarSecciones() {
        Map<Long, Long> inscritos = aMapa(asignaturaRepository.contarInscripcionesPorSeccion());
        return asignaturaRepository.findSeccionesConDetalle().stream()
                .map(s -> new SeccionFila(s.getIdSeccion(), s.getAsignatura().getIdAsignatura(),
                        s.getAsignatura().getNombre(), s.getAnio(), s.getSemestre(),
                        s.getProfesor().getIdUsuario(), s.getProfesor().getNombre(),
                        inscritos.getOrDefault(s.getIdSeccion(), 0L)))
                .toList();
    }

    /** Profesores de asignatura con cuenta activa, para el selector del formulario de sección. */
    @Transactional(readOnly = true)
    public List<ProfesorOpcion> profesoresDisponibles() {
        return asignaturaRepository.findProfesoresActivos().stream()
                .map(p -> new ProfesorOpcion(p.getIdUsuario(), p.getNombre(), p.getEmail()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AsignaturaFormulario formularioDeAsignatura(Long idAsignatura) {
        Asignatura asignatura = obtenerAsignatura(idAsignatura);
        AsignaturaFormulario formulario = new AsignaturaFormulario();
        formulario.setNombre(asignatura.getNombre());
        formulario.setCiclo(asignatura.getCiclo());
        formulario.setNumeroLineas(asignatura.getNumeroLineas());
        return formulario;
    }

    @Transactional(readOnly = true)
    public SeccionFormulario formularioDeSeccion(Long idSeccion) {
        SeccionAsignatura seccion = obtenerSeccion(idSeccion);
        SeccionFormulario formulario = new SeccionFormulario();
        formulario.setIdAsignatura(seccion.getAsignatura().getIdAsignatura());
        formulario.setAnio(seccion.getAnio());
        formulario.setSemestre(seccion.getSemestre());
        formulario.setIdProfesor(seccion.getProfesor().getIdUsuario());
        return formulario;
    }

    /** ¿Tiene inscripciones? La vista lo usa para bloquear los campos que ya no se pueden cambiar. */
    @Transactional(readOnly = true)
    public boolean asignaturaTieneInscripciones(Long idAsignatura) {
        return asignaturaRepository.contarInscripcionesDeAsignatura(idAsignatura) > 0;
    }

    @Transactional(readOnly = true)
    public boolean seccionTieneInscripciones(Long idSeccion) {
        return asignaturaRepository.contarInscripcionesDeSeccion(idSeccion) > 0;
    }

    public int anioMinimo() {
        return anioActual() - RANGO_ANIOS;
    }

    public int anioMaximo() {
        return anioActual() + RANGO_ANIOS;
    }

    // ---------- asignaturas ----------

    /**
     * @return el id de la asignatura creada
     * @throws EstructuraInvalidaException el formulario tiene errores; vienen todos juntos
     */
    @Transactional
    public Long crearAsignatura(AsignaturaFormulario formulario) {
        List<String> errores = new ArrayList<>();
        String nombre = validarNombre(formulario.getNombre(), null, errores);
        Ciclo ciclo = validarCiclo(formulario.getCiclo(), errores);
        Integer lineas = validarLineas(formulario.getNumeroLineas(), errores);
        lanzarSiHayErrores(errores);

        return asignaturaRepository.save(new Asignatura(nombre, ciclo, lineas)).getIdAsignatura();
    }

    /**
     * Nombre y ciclo se pueden editar siempre; el número de líneas solo mientras no haya inscripciones.
     *
     * @throws ResponseStatusException     404 si la asignatura no existe
     * @throws EstructuraInvalidaException el formulario tiene errores; vienen todos juntos
     */
    @Transactional
    public void actualizarAsignatura(Long idAsignatura, AsignaturaFormulario formulario) {
        Asignatura asignatura = obtenerAsignatura(idAsignatura);
        List<String> errores = new ArrayList<>();
        String nombre = validarNombre(formulario.getNombre(), idAsignatura, errores);
        Ciclo ciclo = validarCiclo(formulario.getCiclo(), errores);
        Integer lineas = validarLineas(formulario.getNumeroLineas(), errores);

        if (lineas != null && !lineas.equals(asignatura.getNumeroLineas())
                && asignaturaRepository.contarInscripcionesDeAsignatura(idAsignatura) > 0) {
            errores.add("No se puede cambiar el número de líneas: la asignatura ya tiene estudiantes inscritos");
        }
        lanzarSiHayErrores(errores);

        asignatura.setNombre(nombre);
        asignatura.setCiclo(ciclo);
        asignatura.setNumeroLineas(lineas);
        asignaturaRepository.save(asignatura);
    }

    /**
     * @throws ResponseStatusException 404 si la asignatura no existe
     * @throws IllegalStateException   la asignatura todavía tiene secciones
     */
    @Transactional
    public void eliminarAsignatura(Long idAsignatura) {
        Asignatura asignatura = obtenerAsignatura(idAsignatura);
        long secciones = asignaturaRepository.contarSecciones(idAsignatura);
        if (secciones > 0) {
            throw new IllegalStateException("No se puede eliminar la asignatura: tiene " + secciones
                    + (secciones == 1 ? " sección" : " secciones") + ". Elimina primero sus secciones.");
        }
        // La asignatura borra en cascada sus pautas; sin secciones no puede haber evaluaciones aplicadas.
        asignaturaRepository.delete(asignatura);
    }

    // ---------- secciones ----------

    /**
     * @return el id de la sección creada
     * @throws EstructuraInvalidaException el formulario tiene errores; vienen todos juntos
     */
    @Transactional
    public Long crearSeccion(SeccionFormulario formulario) {
        List<String> errores = new ArrayList<>();
        Asignatura asignatura = validarAsignatura(formulario.getIdAsignatura(), errores);
        Integer anio = validarAnio(formulario.getAnio(), null, errores);
        Integer semestre = validarSemestre(formulario.getSemestre(), errores);
        ProfesorAsignatura profesor = validarProfesor(formulario.getIdProfesor(), errores);

        if (asignatura != null && anio != null && semestre != null
                && asignaturaRepository.existeSeccion(asignatura.getIdAsignatura(), anio, semestre, -1L)) {
            errores.add(mensajeDuplicada(asignatura, anio, semestre));
        }
        lanzarSiHayErrores(errores);

        return seccionRepository.save(new SeccionAsignatura(asignatura, profesor, anio, semestre)).getIdSeccion();
    }

    /**
     * El profesor se puede cambiar siempre. Asignatura, año y semestre solo mientras no haya inscripciones.
     *
     * @throws ResponseStatusException     404 si la sección no existe
     * @throws EstructuraInvalidaException el formulario tiene errores; vienen todos juntos
     */
    @Transactional
    public void actualizarSeccion(Long idSeccion, SeccionFormulario formulario) {
        SeccionAsignatura seccion = obtenerSeccion(idSeccion);
        List<String> errores = new ArrayList<>();
        Asignatura asignatura = validarAsignatura(formulario.getIdAsignatura(), errores);
        // Si el año no cambia no se revisa el rango, para poder cambiar el profesor de una sección antigua.
        Integer anio = validarAnio(formulario.getAnio(), seccion.getAnio(), errores);
        Integer semestre = validarSemestre(formulario.getSemestre(), errores);
        ProfesorAsignatura profesor = validarProfesor(formulario.getIdProfesor(), errores);

        if (asignatura != null && anio != null && semestre != null) {
            boolean cambiaPeriodo = !asignatura.getIdAsignatura().equals(seccion.getAsignatura().getIdAsignatura())
                    || !anio.equals(seccion.getAnio())
                    || !semestre.equals(seccion.getSemestre());
            if (cambiaPeriodo) {
                if (asignaturaRepository.contarInscripcionesDeSeccion(idSeccion) > 0) {
                    errores.add("No se puede cambiar la asignatura, el año ni el semestre: "
                            + "la sección ya tiene estudiantes inscritos");
                } else if (asignaturaRepository.existeSeccion(asignatura.getIdAsignatura(), anio, semestre,
                        idSeccion)) {
                    errores.add(mensajeDuplicada(asignatura, anio, semestre));
                }
            }
        }
        lanzarSiHayErrores(errores);

        seccion.setAsignatura(asignatura);
        seccion.setAnio(anio);
        seccion.setSemestre(semestre);
        seccion.setProfesor(profesor);
        seccionRepository.save(seccion);
    }

    /**
     * @throws ResponseStatusException 404 si la sección no existe
     * @throws IllegalStateException   la sección tiene estudiantes inscritos
     */
    @Transactional
    public void eliminarSeccion(Long idSeccion) {
        SeccionAsignatura seccion = obtenerSeccion(idSeccion);
        long inscritos = asignaturaRepository.contarInscripcionesDeSeccion(idSeccion);
        if (inscritos > 0) {
            throw new IllegalStateException("No se puede eliminar la sección: tiene " + inscritos
                    + (inscritos == 1 ? " estudiante inscrito." : " estudiantes inscritos."));
        }
        // Se quita también de la colección de la asignatura para que Hibernate no la vuelva a guardar en cascada.
        seccion.getAsignatura().getSecciones().remove(seccion);
        seccionRepository.delete(seccion);
    }

    // ---------- validaciones (acumulan los errores en la lista) ----------

    /** Devuelve el nombre sin espacios sobrantes, o null si es inválido. {@code idExcluida}: la que se está editando. */
    private String validarNombre(String nombre, Long idExcluida, List<String> errores) {
        if (nombre == null || nombre.isBlank()) {
            errores.add("El nombre de la asignatura es obligatorio");
            return null;
        }
        String limpio = nombre.strip();
        if (limpio.length() > LARGO_NOMBRE) {
            errores.add("El nombre no puede pasar de " + LARGO_NOMBRE + " caracteres");
            return null;
        }
        boolean repetido = asignaturaRepository.findByNombreIgnoreCase(limpio)
                .filter(otra -> !otra.getIdAsignatura().equals(idExcluida))
                .isPresent();
        if (repetido) {
            errores.add("Ya existe una asignatura con el nombre \"" + limpio + "\"");
            return null;
        }
        return limpio;
    }

    private Ciclo validarCiclo(Ciclo ciclo, List<String> errores) {
        if (ciclo == null) {
            errores.add("Elige el ciclo de la asignatura");
        }
        return ciclo;
    }

    private Integer validarLineas(Integer lineas, List<String> errores) {
        if (lineas == null) {
            errores.add("Indica el número de líneas de práctica");
            return null;
        }
        if (lineas != 1 && lineas != 2) {
            errores.add("El número de líneas debe ser 1 o 2");
            return null;
        }
        return lineas;
    }

    private Asignatura validarAsignatura(Long idAsignatura, List<String> errores) {
        if (idAsignatura == null) {
            errores.add("Elige la asignatura de la sección");
            return null;
        }
        return asignaturaRepository.findById(idAsignatura).orElseGet(() -> {
            errores.add("La asignatura elegida no existe");
            return null;
        });
    }

    /** {@code anioActualDeLaSeccion}: si se edita y el año no cambia, no se exige el rango. */
    private Integer validarAnio(Integer anio, Integer anioActualDeLaSeccion, List<String> errores) {
        if (anio == null) {
            errores.add("El año es obligatorio");
            return null;
        }
        if (anio.equals(anioActualDeLaSeccion)) {
            return anio;
        }
        if (anio < anioMinimo() || anio > anioMaximo()) {
            errores.add("El año debe estar entre " + anioMinimo() + " y " + anioMaximo());
            return null;
        }
        return anio;
    }

    private Integer validarSemestre(Integer semestre, List<String> errores) {
        if (semestre == null) {
            errores.add("El semestre es obligatorio");
            return null;
        }
        if (semestre != 1 && semestre != 2) {
            errores.add("El semestre debe ser 1 o 2");
            return null;
        }
        return semestre;
    }

    private ProfesorAsignatura validarProfesor(Long idProfesor, List<String> errores) {
        if (idProfesor == null) {
            errores.add("Elige el profesor de la sección");
            return null;
        }
        ProfesorAsignatura profesor = profesorRepository.findById(idProfesor).orElse(null);
        if (profesor == null || profesor.getRol() != RolUsuario.PROFESOR_ASIGNATURA) {
            errores.add("El profesor elegido no existe o no es profesor de asignatura");
            return null;
        }
        if (!profesor.isActivo()) {
            errores.add("La cuenta del profesor elegido está desactivada");
            return null;
        }
        return profesor;
    }

    // ---------- ayudas ----------

    private static String mensajeDuplicada(Asignatura asignatura, int anio, int semestre) {
        return "Ya existe una sección de " + asignatura.getNombre() + " para el semestre " + semestre
                + " de " + anio;
    }

    private static void lanzarSiHayErrores(List<String> errores) {
        if (!errores.isEmpty()) {
            throw new EstructuraInvalidaException(errores);
        }
    }

    private int anioActual() {
        return LocalDate.now(reloj).getYear();
    }

    private Asignatura obtenerAsignatura(Long idAsignatura) {
        return asignaturaRepository.findById(idAsignatura)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private SeccionAsignatura obtenerSeccion(Long idSeccion) {
        return asignaturaRepository.findSeccionConDetalle(idSeccion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Convierte filas [id, cantidad] en un mapa id → cantidad. */
    private static Map<Long, Long> aMapa(List<Object[]> filas) {
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] fila : filas) {
            mapa.put(((Number) fila[0]).longValue(), ((Number) fila[1]).longValue());
        }
        return mapa;
    }
}
