package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.inicio.FuncionInicio;
import com.grupo1.isw.practicas_pedagogicas.services.FuncionesDelSistema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La lista de funciones que ven la barra superior y la página de inicio. */
class FuncionesDelSistemaTest {

    @ParameterizedTest(name = "1. El rol {0} tiene funciones completas, con ruta absoluta y sin repetir")
    @ValueSource(strings = {"ESTUDIANTE", "COLABORADOR", "TUTOR", "PROFESOR_ASIGNATURA"})
    void cadaRolTieneFuncionesValidas(String rol) {
        List<FuncionInicio> funciones = FuncionesDelSistema.paraRol(rol);

        assertFalse(funciones.isEmpty());
        for (FuncionInicio funcion : funciones) {
            assertFalse(funcion.titulo().isBlank());
            assertFalse(funcion.etiqueta().isBlank());
            assertFalse(funcion.descripcion().isBlank());
            assertTrue(funcion.enlace().startsWith("/"), funcion.enlace());
        }
        assertEquals(funciones.size(), funciones.stream().map(FuncionInicio::enlace).distinct().count());
    }

    @Test
    @DisplayName("2. Cada rol ve solo lo suyo: el estudiante no recibe las rutas del profesor ni al revés")
    void cadaRolVeSoloLoSuyo() {
        assertTrue(FuncionesDelSistema.paraRol("ESTUDIANTE").stream()
                .noneMatch(f -> f.enlace().startsWith("/profesor") || f.enlace().startsWith("/colaborador")));
        assertTrue(FuncionesDelSistema.paraRol("PROFESOR_ASIGNATURA").stream()
                .allMatch(f -> f.enlace().startsWith("/profesor")));
        assertTrue(FuncionesDelSistema.paraRol("TUTOR").stream()
                .allMatch(f -> f.enlace().startsWith("/tutor")));
    }

    @Test
    @DisplayName("3. Un rol desconocido o nulo no recibe ninguna función")
    void rolDesconocido() {
        assertTrue(FuncionesDelSistema.paraRol("INVITADO").isEmpty());
        assertTrue(FuncionesDelSistema.paraRol(null).isEmpty());
    }
}
