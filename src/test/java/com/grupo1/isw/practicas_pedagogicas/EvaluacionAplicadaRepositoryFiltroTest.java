package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.TipoEvaluador;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionAplicadaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * HU-03: una misma línea puede tener, para la misma pauta, una evaluación del colaborador
 * y otra del tutor. Cada rol debe ver (y editar) únicamente la suya.
 *
 * Los métodos {@code default} del repositorio se ejecutan de verdad; solo la consulta
 * {@code findByLinea} (la que iría a la base de datos) se reemplaza.
 */
class EvaluacionAplicadaRepositoryFiltroTest {

    private static final Long ID_LINEA = 1L;

    private EvaluacionAplicadaRepository repositorio;
    private EvaluacionAplicada delColaborador;
    private EvaluacionAplicada delTutor;

    @BeforeEach
    void preparar() {
        repositorio = mock(EvaluacionAplicadaRepository.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        delColaborador = new EvaluacionAplicada(null, null, TipoEvaluador.COLABORADOR, LocalDateTime.now());
        delTutor = new EvaluacionAplicada(null, null, TipoEvaluador.TUTOR, LocalDateTime.now());
        doReturn(List.of(delColaborador, delTutor)).when(repositorio).findByLinea(ID_LINEA);
    }

    @Test
    @DisplayName("1. findDelTutorByLinea devuelve solo las evaluaciones del tutor")
    void delTutor_excluyeLasDelColaborador() {
        List<EvaluacionAplicada> resultado = repositorio.findDelTutorByLinea(ID_LINEA);

        assertEquals(1, resultado.size());
        assertSame(delTutor, resultado.get(0));
    }

    @Test
    @DisplayName("2. findDelColaboradorByLinea sigue devolviendo solo las del colaborador (HU-02 intacta)")
    void delColaborador_excluyeLasDelTutor() {
        List<EvaluacionAplicada> resultado = repositorio.findDelColaboradorByLinea(ID_LINEA);

        assertEquals(1, resultado.size());
        assertSame(delColaborador, resultado.get(0));
    }

    @Test
    @DisplayName("3. findByLineaYEvaluador sin coincidencias devuelve lista vacía")
    void sinCoincidencias_listaVacia() {
        doReturn(List.of(delColaborador)).when(repositorio).findByLinea(ID_LINEA);

        assertEquals(List.of(), repositorio.findByLineaYEvaluador(ID_LINEA, TipoEvaluador.TUTOR));
    }
}
