package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.EntregaFormulario;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaSemestreRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaSemestreRepository.EntregaFila;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionEstructuraRepository.SeccionDatos;
import com.grupo1.isw.practicas_pedagogicas.services.CoordinadorEntregaService;
import com.grupo1.isw.practicas_pedagogicas.services.EstructuraInvalidaException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoordinadorEntregaServiceTest {
    private static final long ID_SECCION = 1L;

    @Mock
    EntregaSemestreRepository entregas;
    @Mock
    SeccionEstructuraRepository secciones;

    private CoordinadorEntregaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new CoordinadorEntregaService(entregas, secciones);
        when(secciones.buscar(ID_SECCION)).thenReturn(Optional.of(seccion(false)));
        when(entregas.siguienteOrden(ID_SECCION)).thenReturn(3);
    }

    private static SeccionDatos seccion(boolean iniciada) {
        return new SeccionDatos(ID_SECCION, 10L, "Práctica Pedagógica", 2, 2026, 1, 5L, "Profesor", iniciada, null);
    }

    private static EntregaFila entrega(long id, long idSeccion) {
        return new EntregaFila(id, idSeccion, "Informe 1", new BigDecimal("20.00"), true, null, 1);
    }

    @Test
    @DisplayName("1. Crea la entrega con nombre limpio, ponderación con coma y habilitada")
    void creaEntrega() {
        when(entregas.insertar(ID_SECCION, "Informe 1", new BigDecimal("12.5"), true, 3)).thenReturn(7L);

        long id = servicio.crear(ID_SECCION, new EntregaFormulario("  Informe 1  ", "12,5"));

        assertEquals(7L, id);
    }

    @Test
    @DisplayName("2. El nombre es obligatorio y no puede repetirse en el semestre")
    void nombreObligatorioYUnico() {
        when(entregas.existeNombre(ID_SECCION, "Informe 1", null)).thenReturn(true);

        EstructuraInvalidaException vacio = assertThrows(EstructuraInvalidaException.class,
                () -> servicio.crear(ID_SECCION, new EntregaFormulario("  ", "10")));
        EstructuraInvalidaException repetido = assertThrows(EstructuraInvalidaException.class,
                () -> servicio.crear(ID_SECCION, new EntregaFormulario("Informe 1", "10")));

        assertTrue(vacio.getErrores().get(0).contains("obligatorio"));
        assertTrue(repetido.getErrores().get(0).contains("Ya hay una entrega"));
        verify(entregas, never()).insertar(anyLong(), anyString(), any(BigDecimal.class), anyBoolean(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("3. La ponderación debe estar entre 0 (sin incluir) y 100 y ser numérica")
    void ponderacionInvalida() {
        for (String invalida : new String[]{"", "abc", "0", "-5", "100.5", "10.123"}) {
            assertThrows(EstructuraInvalidaException.class,
                    () -> servicio.crear(ID_SECCION, new EntregaFormulario("Informe 1", invalida)),
                    "la ponderación '" + invalida + "' debería rechazarse");
        }
    }

    @Test
    @DisplayName("4. Con el semestre iniciado no se crea, edita, deshabilita ni elimina")
    void estructuraCongelada() {
        when(secciones.buscar(ID_SECCION)).thenReturn(Optional.of(seccion(true)));

        assertThrows(IllegalStateException.class, () -> servicio.crear(ID_SECCION, new EntregaFormulario("A", "10")));
        assertThrows(IllegalStateException.class, () -> servicio.actualizar(ID_SECCION, 1L, new EntregaFormulario("A", "10")));
        assertThrows(IllegalStateException.class, () -> servicio.cambiarHabilitada(ID_SECCION, 1L, false));
        assertThrows(IllegalStateException.class, () -> servicio.eliminar(ID_SECCION, 1L));
    }

    @Test
    @DisplayName("5. Una entrega de otra sección se trata como inexistente")
    void entregaDeOtraSeccion() {
        when(entregas.buscar(4L)).thenReturn(Optional.of(entrega(4L, 99L)));

        assertThrows(ResponseStatusException.class, () -> servicio.eliminar(ID_SECCION, 4L));
        assertThrows(ResponseStatusException.class, () -> servicio.formularioDe(ID_SECCION, 4L));
    }

    @Test
    @DisplayName("6. No se elimina una entrega que ya tiene trabajos de estudiantes")
    void noEliminaConTrabajos() {
        when(entregas.buscar(4L)).thenReturn(Optional.of(entrega(4L, ID_SECCION)));
        when(entregas.tieneEntregasDeEstudiantes(4L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> servicio.eliminar(ID_SECCION, 4L));

        verify(entregas, never()).eliminar(anyLong());
    }

    @Test
    @DisplayName("7. Deshabilitar una entrega solo cambia su marca")
    void deshabilita() {
        when(entregas.buscar(4L)).thenReturn(Optional.of(entrega(4L, ID_SECCION)));

        servicio.cambiarHabilitada(ID_SECCION, 4L, false);

        verify(entregas).cambiarHabilitada(4L, false);
    }

    @Test
    @DisplayName("8. El formulario de edición muestra la ponderación sin ceros sobrantes")
    void formularioDeEdicion() {
        when(entregas.buscar(4L)).thenReturn(Optional.of(entrega(4L, ID_SECCION)));

        EntregaFormulario formulario = servicio.formularioDe(ID_SECCION, 4L);

        assertEquals("20", formulario.getPonderacion());
        assertEquals("Informe 1", formulario.getNombre());
    }
}
