package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaSemestreRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaSemestreRepository.EntregaFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaSemestreRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PautaSemestreRepository.PautaFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository.SeccionDatos;
import com.grupo1.isw.practicas_pedagogicas.services.EstructuraInvalidaException;
import com.grupo1.isw.practicas_pedagogicas.services.EstructuraSemestreService;
import com.grupo1.isw.practicas_pedagogicas.services.EstructuraVista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EstructuraSemestreServiceTest {
    private static final long ID_SECCION = 1L;
    private static final long ID_PROFESOR = 5L;
    private static final ZoneId ZONA = ZoneId.of("America/Santiago");

    @Mock
    SeccionEstructuraRepository secciones;
    @Mock
    EntregaSemestreRepository entregas;
    @Mock
    PautaSemestreRepository pautas;

    private EstructuraSemestreService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-03-10T15:00:00Z"), ZONA);
        servicio = new EstructuraSemestreService(secciones, entregas, pautas, reloj);
        when(secciones.buscar(ID_SECCION)).thenReturn(Optional.of(seccion(10L, false)));
    }

    private static SeccionDatos seccion(long idAsignatura, boolean iniciada) {
        return new SeccionDatos(ID_SECCION, idAsignatura, "Práctica Pedagógica", 2, 2026, 1, ID_PROFESOR,
                "Profesor", iniciada, null);
    }

    private static EntregaFila entrega(long id, String ponderacion, boolean habilitada) {
        return new EntregaFila(id, ID_SECCION, "Entrega " + id, new BigDecimal(ponderacion), habilitada, null, (int) id);
    }

    private static PautaFila pauta(long id, String ponderacion, boolean habilitada, long criterios) {
        return new PautaFila(id, ID_SECCION, "Pauta " + id, "COLABORADOR", "ESPECIALIDAD", 1,
                new BigDecimal(ponderacion), habilitada, null, criterios, BigDecimal.TEN);
    }

    @Test
    @DisplayName("1. La suma cuenta solo las evaluaciones habilitadas")
    void sumaSoloHabilitadas() {
        when(entregas.listar(ID_SECCION)).thenReturn(List.of(entrega(1, "30", true), entrega(2, "20", false)));
        when(pautas.listar(ID_SECCION)).thenReturn(List.of(pauta(1, "25", true, 3), pauta(2, "10", false, 3)));

        EstructuraVista vista = servicio.vista(ID_SECCION);

        assertEquals(0, new BigDecimal("55").compareTo(vista.suma()));
        assertFalse(vista.suma100());
        assertEquals(0, new BigDecimal("45").compareTo(vista.diferencia()));
        assertTrue(vista.editable());
    }

    @Test
    @DisplayName("2. Con 100 % exacto se puede iniciar y queda la fecha de inicio")
    void iniciaConCien() {
        when(entregas.listar(ID_SECCION)).thenReturn(List.of(entrega(1, "60", true)));
        when(pautas.listar(ID_SECCION)).thenReturn(List.of(pauta(1, "40", true, 2)));

        servicio.iniciar(ID_SECCION);

        verify(secciones).marcarIniciada(ID_SECCION, LocalDateTime.of(2026, 3, 10, 12, 0));
    }

    @Test
    @DisplayName("3. Si no suma 100 % no se inicia y el error dice cuánto suma")
    void noIniciaSiNoSumaCien() {
        when(entregas.listar(ID_SECCION)).thenReturn(List.of(entrega(1, "60", true), entrega(2, "30", true)));
        when(pautas.listar(ID_SECCION)).thenReturn(List.of());

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> servicio.iniciar(ID_SECCION));

        assertTrue(ex.getErrores().get(0).contains("90 %"));
        verify(secciones, never()).marcarIniciada(anyLong(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("4. Una evaluación deshabilitada no cuenta para llegar a 100 %")
    void deshabilitadaNoCuenta() {
        when(entregas.listar(ID_SECCION)).thenReturn(List.of(entrega(1, "70", true), entrega(2, "30", false)));
        when(pautas.listar(ID_SECCION)).thenReturn(List.of());

        assertThrows(EstructuraInvalidaException.class, () -> servicio.iniciar(ID_SECCION));
    }

    @Test
    @DisplayName("5. Sin ninguna evaluación habilitada no se puede iniciar")
    void sinEvaluaciones() {
        when(entregas.listar(ID_SECCION)).thenReturn(List.of());
        when(pautas.listar(ID_SECCION)).thenReturn(List.of());

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> servicio.iniciar(ID_SECCION));

        assertEquals(2, ex.getErrores().size());
    }

    @Test
    @DisplayName("6. Una pauta habilitada sin criterios impide iniciar")
    void pautaSinCriterios() {
        when(entregas.listar(ID_SECCION)).thenReturn(List.of(entrega(1, "50", true)));
        when(pautas.listar(ID_SECCION)).thenReturn(List.of(pauta(1, "50", true, 0)));

        EstructuraInvalidaException ex = assertThrows(EstructuraInvalidaException.class,
                () -> servicio.iniciar(ID_SECCION));

        assertEquals(1, ex.getErrores().size());
        assertTrue(ex.getErrores().get(0).contains("Pauta 1"));
    }

    @Test
    @DisplayName("7. Un semestre ya iniciado no se vuelve a iniciar")
    void noSeIniciaDosVeces() {
        when(secciones.buscar(ID_SECCION)).thenReturn(Optional.of(seccion(10L, true)));

        assertThrows(IllegalStateException.class, () -> servicio.iniciar(ID_SECCION));

        verify(secciones, never()).marcarIniciada(anyLong(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("8. Una sección que no existe da 404")
    void seccionInexistente() {
        when(secciones.buscar(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> servicio.vista(99L));
    }

    @Test
    @DisplayName("9. El profesor ve solo lo habilitado de su sección")
    void profesorVeSoloHabilitado() {
        when(entregas.listar(ID_SECCION)).thenReturn(List.of(entrega(1, "60", true), entrega(2, "10", false)));
        when(pautas.listar(ID_SECCION)).thenReturn(List.of(pauta(1, "40", true, 2), pauta(2, "5", false, 2)));

        EstructuraVista vista = servicio.vistaParaProfesor(ID_PROFESOR, ID_SECCION);

        assertEquals(1, vista.entregas().size());
        assertEquals(1, vista.pautas().size());
        assertTrue(vista.suma100());
    }

    @Test
    @DisplayName("10. Otro profesor no puede ver la estructura (403)")
    void otroProfesorNoVe() {
        assertThrows(ResponseStatusException.class, () -> servicio.vistaParaProfesor(777L, ID_SECCION));
    }

    @Test
    @DisplayName("11. Copiar en un semestre vacío copia entregas y pautas")
    void copiaEnSemestreVacio() {
        when(secciones.buscar(2L)).thenReturn(Optional.of(new SeccionDatos(2L, 10L, "Práctica Pedagógica", 2, 2025,
                2, ID_PROFESOR, "Profesor", true, null)));
        when(entregas.listar(ID_SECCION)).thenReturn(List.of());
        when(pautas.listar(ID_SECCION)).thenReturn(List.of());
        when(entregas.copiarDe(2L, ID_SECCION)).thenReturn(3);
        when(pautas.copiarDe(2L, ID_SECCION, 10L)).thenReturn(2);

        servicio.copiarDe(ID_SECCION, 2L);

        verify(entregas).copiarDe(2L, ID_SECCION);
        verify(pautas).copiarDe(2L, ID_SECCION, 10L);
    }

    @Test
    @DisplayName("12. No se copia sobre un semestre que ya tiene estructura")
    void noCopiaSobreEstructura() {
        when(secciones.buscar(2L)).thenReturn(Optional.of(seccion(10L, true)));
        when(entregas.listar(ID_SECCION)).thenReturn(List.of(entrega(1, "100", true)));
        when(pautas.listar(ID_SECCION)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> servicio.copiarDe(ID_SECCION, 2L));

        verify(entregas, never()).copiarDe(anyLong(), anyLong());
    }

    @Test
    @DisplayName("13. No se copia desde otra asignatura ni sobre un semestre iniciado")
    void reglasDeCopia() {
        when(secciones.buscar(2L)).thenReturn(Optional.of(new SeccionDatos(2L, 99L, "Otra asignatura", 1, 2025, 2,
                ID_PROFESOR, "Profesor", true, null)));
        when(entregas.listar(ID_SECCION)).thenReturn(List.of());
        when(pautas.listar(ID_SECCION)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> servicio.copiarDe(ID_SECCION, 2L));
        assertThrows(IllegalStateException.class, () -> servicio.copiarDe(ID_SECCION, ID_SECCION));

        when(secciones.buscar(ID_SECCION)).thenReturn(Optional.of(seccion(10L, true)));
        assertThrows(IllegalStateException.class, () -> servicio.copiarDe(ID_SECCION, 2L));
    }

    @Test
    @DisplayName("14. Si el semestre de origen no tiene nada que copiar, se avisa")
    void origenVacio() {
        when(secciones.buscar(2L)).thenReturn(Optional.of(seccion(10L, true)));
        when(entregas.listar(ID_SECCION)).thenReturn(List.of());
        when(pautas.listar(ID_SECCION)).thenReturn(List.of());
        when(entregas.copiarDe(2L, ID_SECCION)).thenReturn(0);
        when(pautas.copiarDe(2L, ID_SECCION, 10L)).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> servicio.copiarDe(ID_SECCION, 2L));
    }
}
