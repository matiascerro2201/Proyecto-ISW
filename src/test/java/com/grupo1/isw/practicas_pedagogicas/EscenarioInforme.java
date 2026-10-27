package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Criterio;
import com.grupo1.isw.practicas_pedagogicas.models.DetalleCriterio;
import com.grupo1.isw.practicas_pedagogicas.models.Dimension;
import com.grupo1.isw.practicas_pedagogicas.models.DocumentoEstudiante;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaDocumento;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorTutor;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.TipoDocumento;
import com.grupo1.isw.practicas_pedagogicas.models.TipoEvaluador;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.repositories.CriterioRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.DetalleCriterioRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.DimensionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.DocumentoEstudianteRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionAplicadaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeFinalRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.ProfesorAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaDocumentoRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaEvaluacionRepository;
import com.grupo1.isw.practicas_pedagogicas.services.CalculadoraNota;
import com.grupo1.isw.practicas_pedagogicas.services.ConstructorInforme;
import com.grupo1.isw.practicas_pedagogicas.services.GeneracionInformeService;
import com.grupo1.isw.practicas_pedagogicas.services.PautaCargador;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** HU-10: asignatura con datos en memoria y repositorios falsos (Proxy) para probar el informe sin base de datos.
 *  Pautas 50 %/50 % (máx. 30 y 20), entregas 30/30/40 %, profesor 3, Camila (inscripción 1) y Diego (inscripción 2). */
final class EscenarioInforme {
    static final long ID_PROFESOR = 3L;
    static final long ID_INSCRIPCION_CAMILA = 1L;
    static final long ID_INSCRIPCION_DIEGO = 2L;

    final Asignatura asignatura = new Asignatura();
    final ProfesorAsignatura profesor = new ProfesorAsignatura();
    final SeccionAsignatura seccion;
    final InscripcionAsignatura camila;
    final InscripcionAsignatura diego;

    final PautaEvaluacion pauta1;
    final PautaEvaluacion pauta2;
    final Criterio c1;
    final Criterio c2;
    final Criterio c3;
    final Criterio c4;
    final Criterio c5;
    final PautaDocumento pautaInforme;
    final PautaDocumento pautaProducto;
    final PautaDocumento pautaProyecto;

    final List<EvaluacionAplicada> evaluaciones = new ArrayList<>();
    final List<DetalleCriterio> detalles = new ArrayList<>();
    final List<DocumentoEstudiante> documentos = new ArrayList<>();
    /** "Tabla" informe_final, por id de inscripción. */
    final Map<Long, InformeFinal> informes = new HashMap<>();
    private long siguienteIdInforme = 1;

    private final List<Dimension> dimensiones = new ArrayList<>();
    private final List<Criterio> criterios = new ArrayList<>();
    private final BigDecimal exigencia;

    EscenarioInforme() {
        this("60");
    }

    EscenarioInforme(String exigenciaPorcentaje) {
        this.exigencia = bd(exigenciaPorcentaje);

        asignatura.setIdAsignatura(1L);
        asignatura.setNombre("Práctica Pedagógica");
        profesor.setIdUsuario(ID_PROFESOR);
        seccion = new SeccionAsignatura(asignatura, profesor, 2026, 2);

        Estudiante estudianteCamila = new Estudiante();
        estudianteCamila.setIdUsuario(4L);
        estudianteCamila.setNombre("Camila Soto");
        Estudiante estudianteDiego = new Estudiante();
        estudianteDiego.setIdUsuario(5L);
        estudianteDiego.setNombre("Diego Pérez");
        camila = new InscripcionAsignatura(estudianteCamila, seccion, null);
        camila.setIdInscripcion(ID_INSCRIPCION_CAMILA);
        diego = new InscripcionAsignatura(estudianteDiego, seccion, null);
        diego.setIdInscripcion(ID_INSCRIPCION_DIEGO);

        pauta1 = pauta(1L, TipoLinea.ESPECIALIDAD, "50");
        pauta2 = pauta(2L, TipoLinea.ORIENTACION, "50");
        Dimension preparacion = dimension(10L, pauta1, "Preparación de la enseñanza", 1);
        Dimension ambiente = dimension(11L, pauta1, "Ambiente de aula", 2);
        Dimension orientacion = dimension(12L, pauta2, "Orientación", 1);
        c1 = criterio(100L, preparacion, "Planifica con objetivos claros", 1);
        c2 = criterio(101L, preparacion, "Domina el contenido", 2);
        c3 = criterio(102L, ambiente, "Mantiene un clima de respeto", 1);
        c4 = criterio(103L, orientacion, "Acompaña a los estudiantes", 1);
        c5 = criterio(104L, orientacion, "Coordina con apoderados", 2);

        pautaInforme = pautaDocumento(1L, "30", TipoDocumento.INFORME);
        pautaProducto = pautaDocumento(2L, "30", TipoDocumento.PRODUCTO);
        pautaProyecto = pautaDocumento(3L, "40", TipoDocumento.PROYECTO_INTERVENCION);
    }

    // ------------------------------------------------------------------ datos de los estudiantes

    /** Camila lo tiene todo: pautas al 80 % y 90 %, entregas 70/85/90; promedio ponderado 5,78125 -> 5,8
     *  (notas 5,5 / 6,25 / 4,75 / 5,875 / 6,25 con pesos 50 / 50 / 30 / 30 / 40). */
    void camilaCompleta() {
        evaluar(camila, TipoLinea.ESPECIALIDAD, pauta1, 1L, LocalDateTime.of(2026, 10, 1, 10, 0),
                new Object[][]{{c1, "8", "Objetivos bien secuenciados"}, {c2, "9", null}, {c3, "7", "Algunas interrupciones"}});
        evaluar(camila, TipoLinea.ORIENTACION, pauta2, 2L, LocalDateTime.of(2026, 10, 2, 11, 0),
                new Object[][]{{c4, "10", "Muy comprometida"}, {c5, "8", ""}});
        entregar(camila, pautaInforme, 1L, "70", "Falta profundizar", LocalDateTime.of(2026, 10, 5, 10, 15));
        entregar(camila, pautaProducto, 2L, "85", "Buen producto", LocalDateTime.of(2026, 10, 6, 16, 40));
        entregar(camila, pautaProyecto, 3L, "90", null, LocalDateTime.of(2026, 10, 7, 9, 0));
    }

    /** Diego solo tiene la evaluación de especialidad (15/30): le falta la de orientación y todas las entregas. */
    void diegoSoloEspecialidad() {
        evaluar(diego, TipoLinea.ESPECIALIDAD, pauta1, 3L, LocalDateTime.of(2026, 10, 3, 9, 0),
                new Object[][]{{c1, "5", null}, {c2, "5", null}, {c3, "5", null}});
    }

    /** Evaluación del profesor colaborador. */
    EvaluacionAplicada evaluar(InscripcionAsignatura inscripcion, TipoLinea tipo, PautaEvaluacion pauta, long idEvaluacion,
                               LocalDateTime fecha, Object[][] puntajes) {
        return evaluar(inscripcion, tipo, pauta, TipoEvaluador.COLABORADOR, idEvaluacion, fecha, puntajes);
    }

    EvaluacionAplicada evaluar(InscripcionAsignatura inscripcion, TipoLinea tipo, PautaEvaluacion pauta,
                               TipoEvaluador evaluador, long idEvaluacion, LocalDateTime fecha, Object[][] puntajes) {
        EvaluacionAplicada evaluacion = new EvaluacionAplicada();
        evaluacion.setIdEvaluacion(idEvaluacion);
        evaluacion.setLinea(linea(inscripcion, tipo));
        evaluacion.setPautaEvaluacion(pauta);
        evaluacion.setTipoEvaluador(evaluador);
        evaluacion.setFecha(fecha);
        evaluaciones.add(evaluacion);
        for (Object[] fila : puntajes) {
            DetalleCriterio detalle = new DetalleCriterio(evaluacion, (Criterio) fila[0], bd((String) fila[1]));
            detalle.setTranscripcionComentario((String) fila[2]);
            detalles.add(detalle);
        }
        return evaluacion;
    }

    /** La línea de práctica de ese tipo; se crea (y queda en la inscripción) la primera vez que se pide. */
    LineaPractica linea(InscripcionAsignatura inscripcion, TipoLinea tipo) {
        for (LineaPractica existente : inscripcion.getLineas()) {
            if (existente.getTipo() == tipo) {
                return existente;
            }
        }
        LineaPractica nueva = new LineaPractica(inscripcion, null, null, tipo);
        inscripcion.getLineas().add(nueva);
        return nueva;
    }

    /** Asigna un profesor supervisor (tutor) a la línea: desde entonces su evaluación también es obligatoria. */
    void asignarTutor(InscripcionAsignatura inscripcion, TipoLinea tipo) {
        ProfesorTutor tutor = new ProfesorTutor();
        tutor.setIdUsuario(6L);
        linea(inscripcion, tipo).setTutor(tutor);
    }

    void entregar(InscripcionAsignatura inscripcion, PautaDocumento pauta, long idDocumento, String puntaje,
                  String comentarios, LocalDateTime fechaSubida) {
        DocumentoEstudiante documento = new DocumentoEstudiante(inscripcion, pauta, "archivo.pdf", fechaSubida);
        documento.setIdDocumento(idDocumento);
        documento.setPuntajeObtenido(puntaje == null ? null : bd(puntaje));
        documento.setComentariosEvaluador(comentarios);
        documentos.add(documento);
    }

    /** El colaborador corrige un puntaje después de que el informe ya se generó. */
    void cambiarPuntaje(EvaluacionAplicada evaluacion, Criterio criterio, String nuevoPuntaje) {
        for (DetalleCriterio detalle : detalles) {
            if (detalle.getEvaluacion() == evaluacion && detalle.getCriterio() == criterio) {
                detalle.setPuntajeObtenido(bd(nuevoPuntaje));
            }
        }
    }

    // ------------------------------------------------------------------ servicios con repositorios falsos

    ConstructorInforme constructor() {
        PautaEvaluacionRepository pautas = repositorio(PautaEvaluacionRepository.class, Map.of(
                "findByAsignatura", a -> List.of(pauta1, pauta2)));
        DimensionRepository dimensionesRepo = repositorio(DimensionRepository.class, Map.of(
                "findByPautas", a -> dimensiones.stream()
                        .filter(d -> ids(a[0]).contains(d.getPautaEvaluacion().getIdPautaEvaluacion()))
                        .toList()));
        CriterioRepository criteriosRepo = repositorio(CriterioRepository.class, Map.of(
                "findByDimensiones", a -> criterios.stream()
                        .filter(c -> ids(a[0]).contains(c.getDimension().getIdDimension()))
                        .toList()));
        EvaluacionAplicadaRepository evaluacionesRepo = repositorio(EvaluacionAplicadaRepository.class, Map.of(
                "findByInscripcion", a -> evaluaciones.stream()
                        .filter(e -> e.getLinea().getInscripcion().getIdInscripcion().equals(a[0]))
                        .toList()));
        DetalleCriterioRepository detallesRepo = repositorio(DetalleCriterioRepository.class, Map.of(
                "findByEvaluaciones", a -> detalles.stream()
                        .filter(d -> ids(a[0]).contains(d.getEvaluacion().getIdEvaluacion()))
                        .toList()));
        PautaDocumentoRepository pautasDocumentoRepo = repositorio(PautaDocumentoRepository.class, Map.of(
                "findByAsignatura_IdAsignaturaOrderByTipoAscNumeroAsc",
                a -> List.of(pautaInforme, pautaProducto, pautaProyecto)));
        DocumentoEstudianteRepository documentosRepo = repositorio(DocumentoEstudianteRepository.class, Map.of(
                "findByInscripcion_IdInscripcion", a -> documentos.stream()
                        .filter(d -> d.getInscripcion().getIdInscripcion().equals(a[0]))
                        .toList()));

        return new ConstructorInforme(pautas, new PautaCargador(dimensionesRepo, criteriosRepo), evaluacionesRepo,
                detallesRepo, pautasDocumentoRepo, documentosRepo, new CalculadoraNota(exigencia), bd("100"));
    }

    GeneracionInformeService servicio() {
        ProfesorAsignaturaRepository profesores = repositorio(ProfesorAsignaturaRepository.class, Map.of(
                "findInscripcionesDelProfesor", a -> {
                    camila.setInformeFinal(informes.get(ID_INSCRIPCION_CAMILA));
                    diego.setInformeFinal(informes.get(ID_INSCRIPCION_DIEGO));
                    return List.of(camila, diego);
                },
                "findInscripcionConProfesor", a -> {
                    InscripcionAsignatura inscripcion = ID_INSCRIPCION_CAMILA == (Long) a[0] ? camila
                            : ID_INSCRIPCION_DIEGO == (Long) a[0] ? diego : null;
                    if (inscripcion != null) {
                        inscripcion.setInformeFinal(informes.get(inscripcion.getIdInscripcion()));
                    }
                    return Optional.ofNullable(inscripcion);
                },
                "findInformeConProfesor", a -> informes.values().stream()
                        .filter(i -> i.getIdInforme().equals(a[0]))
                        .findFirst()));
        InformeFinalRepository informesRepo = repositorio(InformeFinalRepository.class, Map.of(
                "saveAndFlush", a -> {
                    InformeFinal informe = (InformeFinal) a[0];
                    if (informe.getIdInforme() == null) {
                        informe.setIdInforme(siguienteIdInforme++);
                    }
                    informes.put(informe.getInscripcion().getIdInscripcion(), informe);
                    return informe;
                }));
        return new GeneracionInformeService(profesores, informesRepo, constructor());
    }

    // ------------------------------------------------------------------ construcción de entidades

    static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    private PautaEvaluacion pauta(long id, TipoLinea tipo, String ponderacion) {
        PautaEvaluacion pauta = new PautaEvaluacion(asignatura, tipo, 1, bd(ponderacion));
        pauta.setIdPautaEvaluacion(id);
        return pauta;
    }

    private Dimension dimension(long id, PautaEvaluacion pauta, String nombre, int orden) {
        Dimension dimension = new Dimension(pauta, nombre, orden);
        dimension.setIdDimension(id);
        dimensiones.add(dimension);
        return dimension;
    }

    private Criterio criterio(long id, Dimension dimension, String descripcion, int orden) {
        Criterio criterio = new Criterio(dimension, descripcion, bd("10"), orden);
        criterio.setIdCriterio(id);
        criterios.add(criterio);
        return criterio;
    }

    private PautaDocumento pautaDocumento(long id, String ponderacion, TipoDocumento tipo) {
        PautaDocumento pauta = new PautaDocumento(asignatura, bd(ponderacion), tipo, 1);
        pauta.setIdPautaDocumento(id);
        return pauta;
    }

    @SuppressWarnings("unchecked")
    private static Collection<Long> ids(Object argumento) {
        return (Collection<Long>) argumento;
    }

    /** Repositorio falso: responde los métodos listados y lanza UnsupportedOperationException con cualquier otro. */
    @SuppressWarnings("unchecked")
    private static <T> T repositorio(Class<T> tipo, Map<String, Function<Object[], Object>> metodos) {
        return (T) Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, (proxy, metodo, args) -> {
            switch (metodo.getName()) {
                case "toString" -> {
                    return "Fake" + tipo.getSimpleName();
                }
                case "hashCode" -> {
                    return System.identityHashCode(proxy);
                }
                case "equals" -> {
                    return proxy == args[0];
                }
                default -> {
                    Function<Object[], Object> respuesta = metodos.get(metodo.getName());
                    if (respuesta == null) {
                        throw new UnsupportedOperationException(
                                tipo.getSimpleName() + "." + metodo.getName() + " no está simulado en el escenario");
                    }
                    return respuesta.apply(args);
                }
            }
        });
    }
}
