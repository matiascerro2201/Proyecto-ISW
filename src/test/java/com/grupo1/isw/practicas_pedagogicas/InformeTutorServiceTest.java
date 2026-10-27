package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.InformeTutorResponse;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeFinalRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import com.grupo1.isw.practicas_pedagogicas.services.InformeTutorService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-14: el profesor supervisor (tutor) consulta los informes finales de sus estudiantes. */
@ExtendWith(MockitoExtension.class)
class InformeTutorServiceTest {

    private static final Long ID_TUTOR = 6L;
    private static final Long ID_INFORME = 1L;
    private static final Long ID_INSCRIPCION = 10L;

    @Mock
    private InformeFinalRepository informeFinalRepository;

    @Mock
    private LineaPracticaRepository lineaPracticaRepository;

    @InjectMocks
    private InformeTutorService service;

    // ---------- helpers ----------

    private static InformeFinal informe(Long id, Long idInscripcion, String nombreEstudiante, boolean publicado) {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(4L);
        estudiante.setNombre(nombreEstudiante);

        ProfesorAsignatura profesor = new ProfesorAsignatura();
        profesor.setIdUsuario(3L);

        Asignatura asignatura = new Asignatura("Práctica Pedagógica", Ciclo.FINAL, 2);
        SeccionAsignatura seccion = new SeccionAsignatura(asignatura, profesor, 2026, 2);
        InscripcionAsignatura inscripcion = new InscripcionAsignatura(estudiante, seccion, null);
        inscripcion.setIdInscripcion(idInscripcion);

        InformeFinal informe = new InformeFinal(inscripcion, profesor);
        informe.setIdInforme(id);
        informe.setCalificacionSugerida(new BigDecimal("6.2"));
        informe.setCalificacionFinal(new BigDecimal("6.5"));
        informe.setFechaGeneracion(LocalDateTime.of(2026, 10, 20, 9, 0));
        informe.setFechaValidacion(LocalDateTime.of(2026, 10, 21, 15, 30));
        if (publicado) {
            informe.setFechaPublicacion(LocalDateTime.of(2026, 10, 22, 9, 0));
        }
        return informe;
    }

    private void dadoQueElTutorSupervisaLaInscripcion(boolean supervisa) {
        when(lineaPracticaRepository.existsByInscripcion_IdInscripcionAndTutor_IdUsuario(ID_INSCRIPCION, ID_TUTOR))
                .thenReturn(supervisa);
    }

    private void dadoUnInforme(InformeFinal informe) {
        when(informeFinalRepository.findByIdConPropietario(ID_INFORME)).thenReturn(Optional.of(informe));
    }

    // ---------- listar ----------

    @Test
    @DisplayName("1. Lista los informes publicados de sus estudiantes con nombre, asignatura y nota final")
    void listar_devuelveLosInformesDeSusEstudiantes() {
        when(informeFinalRepository.findPublicadosDelTutor(ID_TUTOR))
                .thenReturn(List.of(informe(1L, 10L, "Camila Soto", true), informe(2L, 11L, "Diego Pérez", true)));

        List<InformeTutorResponse> resultado = service.listar(ID_TUTOR);

        assertEquals(2, resultado.size());
        assertEquals("Camila Soto", resultado.get(0).nombreEstudiante());
        assertEquals("Práctica Pedagógica", resultado.get(0).nombreAsignatura());
        assertEquals(0, new BigDecimal("6.5").compareTo(resultado.get(0).calificacionFinal()));
        assertEquals("Diego Pérez", resultado.get(1).nombreEstudiante());
    }

    @Test
    @DisplayName("2. La lista no repite un informe aunque el estudiante tenga dos líneas con el mismo tutor")
    void listar_noRepiteInformes() {
        InformeFinal informe = informe(1L, 10L, "Camila Soto", true);
        // una inscripción con 2 líneas del mismo tutor podría devolver el mismo informe dos veces
        when(informeFinalRepository.findPublicadosDelTutor(ID_TUTOR)).thenReturn(List.of(informe, informe));

        List<InformeTutorResponse> resultado = service.listar(ID_TUTOR);

        assertEquals(1, resultado.size());
        assertEquals(Long.valueOf(1L), resultado.get(0).idInforme());
    }

    @Test
    @DisplayName("3. Un tutor sin informes publicados recibe una lista vacía")
    void listar_sinInformes_listaVacia() {
        when(informeFinalRepository.findPublicadosDelTutor(ID_TUTOR)).thenReturn(List.of());

        assertTrue(service.listar(ID_TUTOR).isEmpty());
    }

    // ---------- obtener ----------

    @Test
    @DisplayName("4. Informe inexistente: 404, sin consultar a quién supervisa")
    void obtener_inexistente_lanza404() {
        when(informeFinalRepository.findByIdConPropietario(ID_INFORME)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtener(ID_TUTOR, ID_INFORME));

        assertEquals(404, ex.getStatusCode().value());
        verify(lineaPracticaRepository, never())
                .existsByInscripcion_IdInscripcionAndTutor_IdUsuario(any(), any());
    }

    @Test
    @DisplayName("5. Estudiante de otro tutor: 403")
    void obtener_deOtroTutor_lanza403() {
        dadoUnInforme(informe(ID_INFORME, ID_INSCRIPCION, "Camila Soto", true));
        dadoQueElTutorSupervisaLaInscripcion(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtener(ID_TUTOR, ID_INFORME));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("6. Informe ajeno y sin publicar: 403, no 404 (nadie deduce si el de otro estudiante ya se publicó)")
    void obtener_ajenoSinPublicar_lanza403() {
        dadoUnInforme(informe(ID_INFORME, ID_INSCRIPCION, "Camila Soto", false));
        dadoQueElTutorSupervisaLaInscripcion(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtener(ID_TUTOR, ID_INFORME));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("7. Informe propio sin publicar: 404 (solo se ven los publicados)")
    void obtener_propioSinPublicar_lanza404() {
        dadoUnInforme(informe(ID_INFORME, ID_INSCRIPCION, "Camila Soto", false));
        dadoQueElTutorSupervisaLaInscripcion(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtener(ID_TUTOR, ID_INFORME));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("8. Informe propio y publicado: devuelve estudiante, asignatura, nota final y fechas")
    void obtener_propioPublicado_devuelveElInforme() {
        dadoUnInforme(informe(ID_INFORME, ID_INSCRIPCION, "Camila Soto", true));
        dadoQueElTutorSupervisaLaInscripcion(true);

        InformeTutorResponse resultado = service.obtener(ID_TUTOR, ID_INFORME);

        assertEquals(ID_INFORME, resultado.idInforme());
        assertEquals("Camila Soto", resultado.nombreEstudiante());
        assertEquals("Práctica Pedagógica", resultado.nombreAsignatura());
        assertEquals(2026, resultado.anio());
        assertEquals(2, resultado.semestre());
        assertEquals(0, new BigDecimal("6.5").compareTo(resultado.calificacionFinal()));
        assertEquals(LocalDateTime.of(2026, 10, 20, 9, 0), resultado.fechaGeneracion());
        assertEquals(LocalDateTime.of(2026, 10, 21, 15, 30), resultado.fechaValidacion());
        assertEquals(LocalDateTime.of(2026, 10, 22, 9, 0), resultado.fechaPublicacion());
    }
}
