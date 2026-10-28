package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaHallazgo;
import com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaInteraccion;
import com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.CategoriaPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.InformeAsistente;
import com.grupo1.isw.practicas_pedagogicas.models.NivelHallazgo;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.repositories.CategoriaPlanificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeAsistenteRepository;
import com.grupo1.isw.practicas_pedagogicas.services.InformeAsistenteService;
import com.grupo1.isw.practicas_pedagogicas.services.InformeAsistenteService.OpcionSeccion;
import com.grupo1.isw.practicas_pedagogicas.services.InformeAsistenteService.VistaInforme;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException.Causa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El informe del profesor: privacidad (nada con menos de 5 estudiantes), cupo diario y manejo de los fallos
 * del asistente. Los repositorios son simulados a mano para poder ejercitar el servicio real con datos reales.
 */
class InformeAsistenteServiceTest {

    private static final Long ID_PROFESOR = 3L;
    private static final Long ID_SECCION = 20L;
    private static final int MINIMO = 5;
    private static final int MAX_POR_DIA = 3;

    /** Lo que "hay en la base de datos" y lo que se le guardó. */
    private static class Datos {
        List<SeccionAsignatura> secciones = new ArrayList<>();
        long inscritos = 30;
        List<FilaPlanificacion> planificaciones = new ArrayList<>();
        List<FilaInteraccion> interacciones = new ArrayList<>();
        List<FilaHallazgo> hallazgos = new ArrayList<>();
        List<FilaEvaluacion> evaluaciones = new ArrayList<>();
        Optional<InformeAsistente> ultimo = Optional.empty();
        long generadosHoy = 0;
        LocalDateTime desdeConsultado;
        List<InformeAsistente> guardados = new ArrayList<>();
    }

    private static class ClienteGuion implements ChatbotClient {
        Respuesta respuesta = new Respuesta("Análisis redactado", List.of());
        RuntimeException error;
        Solicitud ultima;
        int llamadas;

        @Override
        public Respuesta responder(Solicitud solicitud) {
            llamadas++;
            ultima = solicitud;
            if (error != null) {
                throw error;
            }
            return respuesta;
        }
    }

    private Datos datos;
    private ClienteGuion cliente;
    private InformeAsistenteService service;

    @BeforeEach
    void armarServicio() {
        datos = new Datos();
        cliente = new ClienteGuion();
        datos.secciones.add(seccion(ID_SECCION, 2026, 2));

        InformeAsistenteRepository informes = (InformeAsistenteRepository) Proxy.newProxyInstance(
                InformeAsistenteRepository.class.getClassLoader(), new Class[]{InformeAsistenteRepository.class},
                (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "findSeccionesDelProfesor" -> datos.secciones;
                    case "contarEstudiantesDeLaSeccion" -> datos.inscritos;
                    case "filasPlanificaciones" -> datos.planificaciones;
                    case "filasInteracciones" -> datos.interacciones;
                    case "filasHallazgos" -> datos.hallazgos;
                    case "filasEvaluaciones" -> datos.evaluaciones;
                    case "findFirstBySeccion_IdSeccionOrderByFechaGeneracionDescIdInformeDesc" -> datos.ultimo;
                    case "countBySeccion_IdSeccionAndFechaGeneracionGreaterThanEqual" -> {
                        datos.desdeConsultado = (LocalDateTime) args[1];
                        yield datos.generadosHoy;
                    }
                    case "save" -> {
                        datos.guardados.add((InformeAsistente) args[0]);
                        yield args[0];
                    }
                    default -> throw new UnsupportedOperationException(metodo.getName());
                });

        CategoriaPlanificacionRepository categorias = (CategoriaPlanificacionRepository) Proxy.newProxyInstance(
                CategoriaPlanificacionRepository.class.getClassLoader(),
                new Class[]{CategoriaPlanificacionRepository.class},
                (proxy, metodo, args) -> {
                    if (metodo.getName().equals("findByActivaTrueOrderByOrdenAsc")) {
                        return List.of(
                                new CategoriaPlanificacion("OBJETIVOS", "Objetivos de aprendizaje", "descripción", 1),
                                new CategoriaPlanificacion("EVALUACION", "Evaluación del aprendizaje", "descripción", 2),
                                new CategoriaPlanificacion("CIERRE", "Cierre de la clase", "descripción", 3));
                    }
                    throw new UnsupportedOperationException(metodo.getName());
                });

        service = new InformeAsistenteService(informes, categorias, cliente, MINIMO, MAX_POR_DIA);
    }

    // ------------------------------------------------------------------ ayudas

    private SeccionAsignatura seccion(Long id, int anio, int semestre) {
        Asignatura asignatura = new Asignatura();
        asignatura.setNombre("Práctica Pedagógica");
        SeccionAsignatura seccion = new SeccionAsignatura(asignatura, null, anio, semestre);
        seccion.setIdSeccion(id);
        return seccion;
    }

    /** Cada estudiante tiene una planificación enviada y una consulta, con tres valoraciones. */
    private void conEstudiantes(int cantidad) {
        for (int i = 1; i <= cantidad; i++) {
            long plan = i;
            datos.planificaciones.add(new FilaPlanificacion(plan, 10L + i, EstadoPlanificacion.ENVIADA));
            datos.interacciones.add(new FilaInteraccion(plan, 10L + i, plan, LocalDateTime.of(2026, 10, i, 10, 0)));
            LocalDateTime fecha = LocalDateTime.of(2026, 10, i, 10, 0);
            datos.hallazgos.add(new FilaHallazgo(plan, plan, fecha, "OBJETIVOS", NivelHallazgo.DEBIL, "No son medibles"));
            datos.hallazgos.add(new FilaHallazgo(plan, plan, fecha, "EVALUACION", NivelHallazgo.DEBIL, null));
            datos.hallazgos.add(new FilaHallazgo(plan, plan, fecha, "CIERRE", NivelHallazgo.ADECUADO, null));
        }
    }

    private ChatbotNoDisponibleException fallaAlGenerar() {
        return assertThrows(ChatbotNoDisponibleException.class, () -> service.generarAnalisis(ID_PROFESOR, ID_SECCION));
    }

    // ------------------------------------------------------------ listar secciones

    @Test
    @DisplayName("HU-26. Lista las secciones del profesor con la asignatura y el período")
    void listarSecciones() {
        datos.secciones.add(seccion(21L, 2026, 1));

        List<OpcionSeccion> opciones = service.listarSecciones(ID_PROFESOR);

        assertEquals(2, opciones.size());
        assertEquals(ID_SECCION, opciones.get(0).idSeccion());
        assertEquals("Práctica Pedagógica", opciones.get(0).asignatura());
        assertEquals("2026-2", opciones.get(0).periodo());
        assertEquals("2026-1", opciones.get(1).periodo());
    }

    // ----------------------------------------------------------- obtener el informe

    @Test
    @DisplayName("HU-26. Con estudiantes suficientes, el informe trae las cifras")
    void conDatosSuficientes() {
        conEstudiantes(5);

        VistaInforme vista = service.obtenerInforme(ID_PROFESOR, ID_SECCION);

        assertTrue(vista.suficienteDatos());
        assertEquals(5, vista.estudiantesConAsistente());
        assertEquals(MINIMO, vista.minimoEstudiantes());
        assertEquals(30, vista.resumen().totalEstudiantes());
        assertEquals(5, vista.resumen().planificacionesRevisadas());
        assertNull(vista.analisis());
        assertEquals("2026-2", vista.seccion().periodo());
    }

    @Test
    @DisplayName("HU-26. Con menos del mínimo de estudiantes no se expone ninguna cifra")
    void conPocosEstudiantes_noExponeCifras() {
        conEstudiantes(4);

        VistaInforme vista = service.obtenerInforme(ID_PROFESOR, ID_SECCION);

        assertFalse(vista.suficienteDatos());
        assertNull(vista.resumen());
        assertEquals(4, vista.estudiantesConAsistente());
        assertEquals(MINIMO, vista.minimoEstudiantes());
    }

    @Test
    @DisplayName("HU-26. Sin ningún uso del asistente tampoco hay informe")
    void sinUsoDelAsistente() {
        VistaInforme vista = service.obtenerInforme(ID_PROFESOR, ID_SECCION);

        assertFalse(vista.suficienteDatos());
        assertNull(vista.resumen());
        assertEquals(0, vista.estudiantesConAsistente());
    }

    @Test
    @DisplayName("HU-26. El informe muestra el último análisis guardado, con la base con que se generó")
    void muestraElUltimoAnalisis() {
        conEstudiantes(5);
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 27, 19, 40);
        datos.ultimo = Optional.of(new InformeAsistente(datos.secciones.get(0), fecha, "Texto previo", 24, 61));

        VistaInforme vista = service.obtenerInforme(ID_PROFESOR, ID_SECCION);

        assertEquals("Texto previo", vista.analisis().contenido());
        assertEquals(fecha, vista.analisis().fecha());
        assertEquals(24, vista.analisis().numEstudiantes());
        assertEquals(61, vista.analisis().numPlanificaciones());
    }

    @Test
    @DisplayName("HU-26. Informa cuántos análisis quedan hoy, sin bajar de cero")
    void analisisRestantesHoy() {
        conEstudiantes(5);

        assertEquals(3, service.obtenerInforme(ID_PROFESOR, ID_SECCION).analisisRestantesHoy());
        datos.generadosHoy = 1;
        assertEquals(2, service.obtenerInforme(ID_PROFESOR, ID_SECCION).analisisRestantesHoy());
        datos.generadosHoy = 9;
        assertEquals(0, service.obtenerInforme(ID_PROFESOR, ID_SECCION).analisisRestantesHoy());
    }

    @Test
    @DisplayName("HU-26. Una sección que no dicta el profesor, o que no existe, da el mismo 404")
    void seccionAjenaOInexistente() {
        ResponseStatusException ajena = assertThrows(ResponseStatusException.class,
                () -> service.obtenerInforme(ID_PROFESOR, 99L));

        assertEquals(404, ajena.getStatusCode().value());

        datos.secciones.clear();
        ResponseStatusException sinSecciones = assertThrows(ResponseStatusException.class,
                () -> service.obtenerInforme(ID_PROFESOR, ID_SECCION));
        assertEquals(404, sinSecciones.getStatusCode().value());
    }

    // ---------------------------------------------------------- generar el análisis

    @Test
    @DisplayName("HU-26. Genera el análisis: llama una vez al asistente con las cifras y lo guarda")
    void generaYGuardaElAnalisis() {
        conEstudiantes(5);
        cliente.respuesta = new ChatbotClient.Respuesta("  Análisis redactado  ", List.of());
        LocalDateTime antes = LocalDateTime.now();

        service.generarAnalisis(ID_PROFESOR, ID_SECCION);

        assertEquals(1, cliente.llamadas);
        assertTrue(cliente.ultima.categorias().isEmpty());
        assertTrue(cliente.ultima.historial().isEmpty());
        assertTrue(cliente.ultima.instrucciones().contains("Estudiantes inscritos: 30."));
        assertTrue(cliente.ultima.instrucciones().contains("Estudiantes que usaron el asistente: 5"));

        assertEquals(1, datos.guardados.size());
        InformeAsistente guardado = datos.guardados.get(0);
        assertEquals("Análisis redactado", guardado.getContenido());
        assertSame(datos.secciones.get(0), guardado.getSeccion());
        assertEquals(5, guardado.getNumEstudiantes());
        assertEquals(5, guardado.getNumPlanificaciones());
        assertFalse(guardado.getFechaGeneracion().isBefore(antes));
    }

    @Test
    @DisplayName("HU-26. El cupo diario se cuenta desde la medianoche")
    void cupoDesdeLaMedianoche() {
        conEstudiantes(5);

        service.generarAnalisis(ID_PROFESOR, ID_SECCION);

        assertEquals(LocalTime.MIDNIGHT, datos.desdeConsultado.toLocalTime());
    }

    @Test
    @DisplayName("HU-26. Con pocos estudiantes no se genera nada y no se envía nada al proveedor")
    void conPocosEstudiantes_noLlamaAlProveedor() {
        conEstudiantes(4);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.generarAnalisis(ID_PROFESOR, ID_SECCION));

        assertTrue(ex.getMessage().contains("al menos 5 estudiantes"));
        assertTrue(ex.getMessage().contains("hay 4"));
        assertEquals(0, cliente.llamadas);
        assertTrue(datos.guardados.isEmpty());
    }

    @Test
    @DisplayName("HU-26. En el mínimo exacto sí se genera")
    void enElMinimoExacto() {
        conEstudiantes(MINIMO);

        service.generarAnalisis(ID_PROFESOR, ID_SECCION);

        assertEquals(1, cliente.llamadas);
    }

    @Test
    @DisplayName("HU-26. Al llegar al máximo diario no se genera otro análisis ni se consulta al proveedor")
    void limiteDiario() {
        conEstudiantes(5);
        datos.generadosHoy = MAX_POR_DIA;

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.generarAnalisis(ID_PROFESOR, ID_SECCION));

        assertTrue(ex.getMessage().contains(MAX_POR_DIA + " análisis"));
        assertEquals(0, cliente.llamadas);
        assertTrue(datos.guardados.isEmpty());
    }

    @Test
    @DisplayName("HU-26. Con un análisis menos que el máximo diario todavía se puede generar")
    void aunHayCupo() {
        conEstudiantes(5);
        datos.generadosHoy = MAX_POR_DIA - 1;

        service.generarAnalisis(ID_PROFESOR, ID_SECCION);

        assertEquals(1, datos.guardados.size());
    }

    @Test
    @DisplayName("HU-26. No se genera el análisis de una sección ajena")
    void generarEnSeccionAjena() {
        conEstudiantes(5);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.generarAnalisis(ID_PROFESOR, 99L));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals(0, cliente.llamadas);
        assertTrue(datos.guardados.isEmpty());
    }

    // ----------------------------------------------------- fallos del asistente

    @Test
    @DisplayName("HU-26. Si el asistente falla, se propaga con su causa y no se guarda nada")
    void fallosDelAsistente() {
        conEstudiantes(5);

        for (Causa causa : Causa.values()) {
            cliente.error = new ChatbotNoDisponibleException(causa, "x");

            assertEquals(causa, fallaAlGenerar().getCausa(), "causa " + causa);
        }
        assertTrue(datos.guardados.isEmpty());
    }

    @Test
    @DisplayName("HU-26. Un error inesperado del cliente se envuelve como ERROR y no se guarda nada")
    void errorInesperado() {
        conEstudiantes(5);
        cliente.error = new IllegalArgumentException("fallo interno");

        ChatbotNoDisponibleException ex = fallaAlGenerar();

        assertEquals(Causa.ERROR, ex.getCausa());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(datos.guardados.isEmpty());
    }

    @Test
    @DisplayName("HU-26. Un análisis en blanco cuenta como fallo y no se guarda")
    void analisisEnBlanco() {
        conEstudiantes(5);
        cliente.respuesta = new ChatbotClient.Respuesta("   ", List.of());

        assertEquals(Causa.ERROR, fallaAlGenerar().getCausa());
        assertTrue(datos.guardados.isEmpty());
    }

    @Test
    @DisplayName("HU-26. Un análisis enorme se recorta al tamaño máximo")
    void analisisEnorme() {
        conEstudiantes(5);
        cliente.respuesta = new ChatbotClient.Respuesta("y".repeat(InformeAsistenteService.MAX_CARACTERES_ANALISIS + 50), List.of());

        service.generarAnalisis(ID_PROFESOR, ID_SECCION);

        assertEquals(InformeAsistenteService.MAX_CARACTERES_ANALISIS, datos.guardados.get(0).getContenido().length());
    }
}
