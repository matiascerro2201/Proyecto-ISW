package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.CentroFila;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.CentroFormulario;
import com.grupo1.isw.practicas_pedagogicas.models.CentroPractica;
import com.grupo1.isw.practicas_pedagogicas.models.TipoCentro;
import com.grupo1.isw.practicas_pedagogicas.repositories.CentroPracticaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoordinadorCentroServiceTest {
    @Mock CentroPracticaRepository centroRepository;
    @InjectMocks CoordinadorCentroService service;

    private static CentroPractica centro(Long id, String nombre, String comuna, TipoCentro tipo) {
        CentroPractica c = new CentroPractica(nombre, comuna, tipo);
        c.setIdCentro(id);
        return c;
    }

    private static CentroFormulario formulario(String nombre, String comuna, TipoCentro tipo) {
        CentroFormulario f = new CentroFormulario();
        f.setNombre(nombre);
        f.setComuna(comuna);
        f.setTipo(tipo);
        return f;
    }

    @Test
    void creaUnCentroConLosTextosSinEspaciosSobrantes() {
        when(centroRepository.findAllByOrderByNombreAsc()).thenReturn(List.of());
        when(centroRepository.save(any(CentroPractica.class))).thenAnswer(i -> {
            CentroPractica c = i.getArgument(0);
            c.setIdCentro(5L);
            return c;
        });

        Long id = service.crear(formulario("  Liceo Los Aromos ", " La Florida ", TipoCentro.MUNICIPAL));

        assertEquals(5L, id);
        verify(centroRepository).save(any(CentroPractica.class));
    }

    @Test
    void laComunaEsOpcional() {
        when(centroRepository.findAllByOrderByNombreAsc()).thenReturn(List.of());
        when(centroRepository.save(any(CentroPractica.class))).thenAnswer(i -> i.getArgument(0));

        service.crear(formulario("Liceo Sin Comuna", "   ", TipoCentro.PARTICULAR));

        org.mockito.ArgumentCaptor<CentroPractica> guardado = org.mockito.ArgumentCaptor.forClass(CentroPractica.class);
        verify(centroRepository).save(guardado.capture());
        assertNull(guardado.getValue().getComuna());
    }

    @Test
    void noCreaSinNombre() {
        assertThrows(IllegalArgumentException.class, () -> service.crear(formulario("  ", "Chillán", TipoCentro.MUNICIPAL)));
        verify(centroRepository, never()).save(any());
    }

    @Test
    void noCreaSinTipo() {
        assertThrows(IllegalArgumentException.class, () -> service.crear(formulario("Liceo", "Chillán", null)));
        verify(centroRepository, never()).save(any());
    }

    @Test
    void noCreaConUnNombreDemasiadoLargo() {
        String largo = "x".repeat(CoordinadorCentroService.LARGO_NOMBRE + 1);
        assertThrows(IllegalArgumentException.class, () -> service.crear(formulario(largo, "Chillán", TipoCentro.MUNICIPAL)));
    }

    @Test
    void noCreaUnCentroRepetidoEnLaMismaComunaSinImportarTildesNiMayusculas() {
        when(centroRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(centro(1L, "Liceo Los Aromos", "Chillán", TipoCentro.MUNICIPAL)));

        assertThrows(IllegalArgumentException.class,
                () -> service.crear(formulario("LICEO LOS AROMOS", "chillan", TipoCentro.SUBVENCIONADO)));
        verify(centroRepository, never()).save(any());
    }

    @Test
    void permiteElMismoNombreEnOtraComuna() {
        when(centroRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(centro(1L, "Liceo Los Aromos", "Chillán", TipoCentro.MUNICIPAL)));
        when(centroRepository.save(any(CentroPractica.class))).thenAnswer(i -> i.getArgument(0));

        service.crear(formulario("Liceo Los Aromos", "La Florida", TipoCentro.MUNICIPAL));

        verify(centroRepository).save(any(CentroPractica.class));
    }

    @Test
    void alEditarElCentroNoCuentaComoRepetidoConsigoMismo() {
        CentroPractica existente = centro(1L, "Liceo Los Aromos", "Chillán", TipoCentro.MUNICIPAL);
        when(centroRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(centroRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(existente));

        service.actualizar(1L, formulario("Liceo Los Aromos", "Chillán", TipoCentro.SUBVENCIONADO));

        assertEquals(TipoCentro.SUBVENCIONADO, existente.getTipo());
        verify(centroRepository).save(existente);
    }

    @Test
    void alEditarNoPuedeQuedarIgualAOtroCentro() {
        CentroPractica existente = centro(1L, "Liceo Los Aromos", "Chillán", TipoCentro.MUNICIPAL);
        CentroPractica otro = centro(2L, "Colegio Los Robles", "Chillán", TipoCentro.PARTICULAR);
        when(centroRepository.findById(2L)).thenReturn(Optional.of(otro));
        when(centroRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(existente, otro));

        assertThrows(IllegalArgumentException.class,
                () -> service.actualizar(2L, formulario("Liceo Los Aromos", "Chillán", TipoCentro.PARTICULAR)));
        verify(centroRepository, never()).save(any());
    }

    @Test
    void editarUnCentroInexistenteDa404() {
        when(centroRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> service.actualizar(9L, formulario("Liceo", "Chillán", TipoCentro.MUNICIPAL)));
    }

    @Test
    void noEliminaUnCentroQueTieneColaboradoresOInscripciones() {
        when(centroRepository.findById(1L)).thenReturn(Optional.of(centro(1L, "Liceo", "Chillán", TipoCentro.MUNICIPAL)));
        when(centroRepository.contarColaboradores(1L)).thenReturn(1L);
        when(centroRepository.contarInscripciones(1L)).thenReturn(0L);

        assertThrows(IllegalStateException.class, () -> service.eliminar(1L));
        verify(centroRepository, never()).delete(any());
    }

    @Test
    void eliminaUnCentroSinUso() {
        CentroPractica libre = centro(1L, "Liceo", "Chillán", TipoCentro.MUNICIPAL);
        when(centroRepository.findById(1L)).thenReturn(Optional.of(libre));
        when(centroRepository.contarColaboradores(1L)).thenReturn(0L);
        when(centroRepository.contarInscripciones(1L)).thenReturn(0L);

        service.eliminar(1L);

        verify(centroRepository).delete(libre);
    }

    @Test
    void elListadoIndicaQueCentrosSePuedenEliminar() {
        when(centroRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(
                centro(1L, "Liceo A", "Chillán", TipoCentro.MUNICIPAL),
                centro(2L, "Liceo B", null, TipoCentro.EDUCACION_ADULTOS)));
        when(centroRepository.contarColaboradores(1L)).thenReturn(2L);
        when(centroRepository.contarInscripciones(1L)).thenReturn(3L);
        when(centroRepository.contarColaboradores(2L)).thenReturn(0L);
        when(centroRepository.contarInscripciones(2L)).thenReturn(0L);

        List<CentroFila> filas = service.listar();

        assertEquals(2, filas.size());
        assertFalse(filas.get(0).puedeEliminar());
        assertTrue(filas.get(1).puedeEliminar());
        assertEquals("—", filas.get(1).comunaEtiqueta());
    }
}
