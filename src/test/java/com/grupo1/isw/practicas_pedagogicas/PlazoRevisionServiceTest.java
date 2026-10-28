package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.PlazoRevisionResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.PlazoRevisionResponse.Estado;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoInscripcion;
import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.repositories.ProfesorAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.services.PlazoRevisionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-12: el profesor define y visualiza la fecha límite de revisión de los informes de sus secciones. */
@ExtendWith(MockitoExtension.class)
class PlazoRevisionServiceTest {

    private static final Long ID_PROFESOR = 3L;
    private static final LocalDate HOY = LocalDate.of(2026, 10, 28);

    @Mock
    private SeccionAsignaturaRepository seccionRepository;

    @Mock
    private ProfesorAsignaturaRepository profesorRepository;

    private PlazoRevisionService service;
    private ProfesorAsignatura profesor;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneOffset.UTC);
        service = new PlazoRevisionService(seccionRepository, profesorRepository, reloj);
        profesor = new ProfesorAsignatura();
        profesor.setIdUsuario(ID_PROFESOR);
    }

    private SeccionAsignatura seccion(long id, String asignatura, LocalDate fechaLimite) {
        SeccionAsignatura s = new SeccionAsignatura(new Asignatura(asignatura, Ciclo.FINAL, 2), profesor, 2026, 2);
        s.setIdSeccion(id);
        s.setFechaLimiteRevision(fechaLimite);
        return s;
    }

    private InscripcionAsignatura inscripcion(SeccionAsignatura seccion, boolean publicado, EstadoInscripcion estado) {
        InscripcionAsignatura ins = new InscripcionAsignatura(null, seccion, null);
        ins.setEstado(estado);
        if (publicado) {
            InformeFinal informe = new InformeFinal(ins, profesor);
            informe.setFechaPublicacion(LocalDateTime.of(2026, 10, 20, 9, 0));
            ins.setInformeFinal(informe);
        }
        return ins;
    }

    @Test
    @DisplayName("1. Guarda una fecha límite válida en la sección del profesor")
    void guardar_fechaValida() {
        SeccionAsignatura s = seccion(1L, "Práctica Pedagógica", null);
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(s));

        LocalDate guardada = service.guardarFechaLimite(ID_PROFESOR, 1L, "2026-11-15");

        assertEquals(LocalDate.of(2026, 11, 15), guardada);
        assertEquals(LocalDate.of(2026, 11, 15), s.getFechaLimiteRevision());
        verify(seccionRepository).save(s);
    }

    @Test
    @DisplayName("2. Con el campo vacío se quita la fecha límite")
    void guardar_vacio_quitaLaFecha() {
        SeccionAsignatura s = seccion(1L, "Práctica Pedagógica", LocalDate.of(2026, 11, 15));
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(s));

        assertNull(service.guardarFechaLimite(ID_PROFESOR, 1L, ""));
        assertNull(s.getFechaLimiteRevision());
        verify(seccionRepository).save(s);
    }

    @Test
    @DisplayName("3. Una fecha anterior a hoy se rechaza y no se guarda")
    void guardar_fechaPasada_seRechaza() {
        SeccionAsignatura s = seccion(1L, "Práctica Pedagógica", null);
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(s));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.guardarFechaLimite(ID_PROFESOR, 1L, "2026-10-27"));

        assertEquals("La fecha límite no puede ser anterior a hoy", ex.getMessage());
        verify(seccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("4. Un profesor que no dicta la sección recibe 403 y no se guarda nada")
    void guardar_seccionAjena_lanza403() {
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion(1L, "Práctica Pedagógica", null)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.guardarFechaLimite(99L, 1L, "2026-11-15"));

        assertEquals(403, ex.getStatusCode().value());
        verify(seccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. La lista va de la fecha más próxima a la sin fecha, con su estado y pendientes")
    void listar_ordenaPorUrgenciaYCalculaEstado() {
        SeccionAsignatura vencida = seccion(1L, "Práctica Inicial", HOY.minusDays(3));
        SeccionAsignatura proxima = seccion(2L, "Práctica Intermedia", HOY.plusDays(2));
        SeccionAsignatura sinFecha = seccion(3L, "Práctica Final", null);
        SeccionAsignatura completa = seccion(4L, "Práctica Profesional", HOY.plusDays(20));
        when(seccionRepository.findDelProfesor(ID_PROFESOR)).thenReturn(List.of(sinFecha, completa, proxima, vencida));
        when(profesorRepository.findInscripcionesDelProfesor(ID_PROFESOR)).thenReturn(List.of(
                inscripcion(vencida, false, EstadoInscripcion.EN_CURSO),
                inscripcion(proxima, false, EstadoInscripcion.EN_CURSO),
                inscripcion(proxima, true, EstadoInscripcion.EN_CURSO),
                inscripcion(proxima, false, EstadoInscripcion.RETIRADO),   // no cuenta
                inscripcion(completa, true, EstadoInscripcion.EN_CURSO)));

        List<PlazoRevisionResponse> plazos = service.listarPlazos(ID_PROFESOR);

        assertEquals(List.of(1L, 2L, 4L, 3L), plazos.stream().map(PlazoRevisionResponse::idSeccion).toList());

        assertEquals(Estado.VENCIDO, plazos.get(0).estado());
        assertEquals("Venció hace 3 días", plazos.get(0).textoEstado());

        assertEquals(Estado.PROXIMO, plazos.get(1).estado());
        assertEquals("Vence en 2 días", plazos.get(1).textoEstado());
        assertEquals(1, plazos.get(1).informesPendientes());
        assertEquals(2, plazos.get(1).totalEstudiantes());

        assertEquals(Estado.COMPLETADO, plazos.get(2).estado());
        assertEquals(Estado.SIN_FECHA, plazos.get(3).estado());
    }
}
