package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.CursoDetalle;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.CursoDetalle.PautaDocumentoResumen;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.CursoDetalle.PautaEvaluacionResumen;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.CursoResumen;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.SemestreResumen;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;
import com.grupo1.isw.practicas_pedagogicas.models.PautaDocumento;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.repositories.CoordinadorConsultaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HU-37 y HU-38: el coordinador recorre todos los cursos (primero por semestre, después por curso) y decide
 * qué pautas de cada asignatura están habilitadas. No hay filtro por profesor: ve todo el sistema.
 */
@Service
public class CoordinadorCursoService {
    private final CoordinadorConsultaRepository consulta;
    private final PautaEvaluacionRepository pautaEvaluacionRepository;
    private final ActividadInscripcionService actividadService;

    public CoordinadorCursoService(CoordinadorConsultaRepository consulta,
                                   PautaEvaluacionRepository pautaEvaluacionRepository,
                                   ActividadInscripcionService actividadService) {
        this.consulta = consulta;
        this.pautaEvaluacionRepository = pautaEvaluacionRepository;
        this.actividadService = actividadService;
    }

    /** Los semestres con cursos, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<SemestreResumen> semestres() {
        return consulta.resumenPorSemestre().stream()
                .map(f -> new SemestreResumen(entero(f[0]), entero(f[1]), largo(f[2]), largo(f[3])))
                .toList();
    }

    /**
     * Los cursos de un semestre. El texto busca en el nombre de la asignatura y en el del profesor, sin importar
     * mayúsculas ni tildes; el ciclo filtra por ciclo de la práctica. Un semestre tiene pocos cursos, así que se
     * filtra en memoria.
     */
    @Transactional(readOnly = true)
    public List<CursoResumen> cursosDelSemestre(int anio, int semestre, String texto, Ciclo ciclo) {
        String buscado = TextoBusqueda.normalizar(texto);
        List<SeccionAsignatura> secciones = consulta.findDelSemestre(anio, semestre).stream()
                .filter(s -> ciclo == null || s.getAsignatura().getCiclo() == ciclo)
                .filter(s -> TextoBusqueda.contiene(s.getAsignatura().getNombre(), buscado)
                        || TextoBusqueda.contiene(s.getProfesor().getNombre(), buscado))
                .toList();
        if (secciones.isEmpty()) {
            return List.of();
        }

        Map<Long, long[]> conteos = new HashMap<>();
        for (Object[] f : consulta.contarPorSeccion(secciones.stream().map(SeccionAsignatura::getIdSeccion).toList())) {
            conteos.put(largo(f[0]), new long[]{largo(f[1]), largo(f[2])});
        }
        return secciones.stream()
                .map(s -> {
                    long[] conteo = conteos.getOrDefault(s.getIdSeccion(), new long[]{0, 0});
                    return new CursoResumen(s.getIdSeccion(), s.getAsignatura().getNombre(),
                            s.getAsignatura().getCiclo(), s.getProfesor().getNombre(), conteo[0], conteo[1]);
                })
                .toList();
    }

    /** Un curso con sus pautas y cada uno de sus estudiantes con el estado de sus actividades. */
    @Transactional(readOnly = true)
    public CursoDetalle detalle(Long idSeccion) {
        SeccionAsignatura seccion = seccion(idSeccion);
        Asignatura asignatura = seccion.getAsignatura();

        List<PautaEvaluacion> pautas = pautaEvaluacionRepository.findByAsignatura(asignatura.getIdAsignatura());
        Map<Long, long[]> estructura = new HashMap<>();
        if (!pautas.isEmpty()) {
            for (Object[] f : consulta.contarEstructuraDePautas(
                    pautas.stream().map(PautaEvaluacion::getIdPautaEvaluacion).toList())) {
                estructura.put(largo(f[0]), new long[]{largo(f[1]), largo(f[2])});
            }
        }
        List<PautaEvaluacionResumen> pautasEvaluacion = pautas.stream()
                .map(p -> {
                    long[] conteo = estructura.getOrDefault(p.getIdPautaEvaluacion(), new long[]{0, 0});
                    return new PautaEvaluacionResumen(p.getIdPautaEvaluacion(), p.getTipoLinea(),
                            p.getNumeroAplicacion(), p.getPonderacion(), (int) conteo[0], (int) conteo[1],
                            p.isHabilitada());
                })
                .toList();
        List<PautaDocumentoResumen> pautasDocumento = consulta.findPautasDocumento(asignatura.getIdAsignatura()).stream()
                .map(p -> new PautaDocumentoResumen(p.getIdPautaDocumento(), p.getTipo(), p.getNumero(),
                        p.getPonderacion(), p.isHabilitada()))
                .toList();

        return new CursoDetalle(
                seccion.getIdSeccion(),
                asignatura.getIdAsignatura(),
                asignatura.getNombre(),
                asignatura.getCiclo(),
                asignatura.getNumeroLineas(),
                seccion.getProfesor().getNombre(),
                seccion.getProfesor().getEmail(),
                seccion.getAnio(),
                seccion.getSemestre(),
                seccion.getFechaLimiteRevision(),
                pautasEvaluacion,
                pautasDocumento,
                actividadService.armar(consulta.findInscripcionesDeSeccion(idSeccion)));
    }

    /**
     * HU-38: habilita o deshabilita una pauta de evaluación. La pauta debe ser de la asignatura del curso desde el
     * que se pide. La marca vale para la asignatura completa (todos sus semestres), porque las pautas se definen
     * por asignatura.
     */
    @Transactional
    public void cambiarHabilitacionPautaEvaluacion(Long idSeccion, Long idPauta, boolean habilitada) {
        SeccionAsignatura seccion = seccion(idSeccion);
        PautaEvaluacion pauta = consulta.findPautaEvaluacion(idPauta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        verificarMismaAsignatura(seccion, pauta.getAsignatura());
        pauta.setHabilitada(habilitada);
    }

    @Transactional
    public void cambiarHabilitacionPautaDocumento(Long idSeccion, Long idPauta, boolean habilitada) {
        SeccionAsignatura seccion = seccion(idSeccion);
        PautaDocumento pauta = consulta.findPautaDocumento(idPauta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        verificarMismaAsignatura(seccion, pauta.getAsignatura());
        pauta.setHabilitada(habilitada);
    }

    private SeccionAsignatura seccion(Long idSeccion) {
        return consulta.findConAsignaturaYProfesor(idSeccion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static void verificarMismaAsignatura(SeccionAsignatura seccion, Asignatura delaPauta) {
        if (!seccion.getAsignatura().getIdAsignatura().equals(delaPauta.getIdAsignatura())) {
            throw new IllegalArgumentException("La pauta no pertenece a la asignatura de este curso.");
        }
    }

    private static int entero(Object valor) {
        return ((Number) valor).intValue();
    }

    private static long largo(Object valor) {
        return valor == null ? 0 : ((Number) valor).longValue();
    }
}
