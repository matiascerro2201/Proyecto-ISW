package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.services.ArchivosPdf;
import com.grupo1.isw.practicas_pedagogicas.services.EntradaNota;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntradaNotaTest {

    @Test
    @DisplayName("1. Acepta notas de 1,0 a 7,0 con coma o punto")
    void notasValidas() {
        List<String> errores = new ArrayList<>();

        assertEquals(new BigDecimal("5.5"), EntradaNota.leer("5,5", errores));
        assertEquals(new BigDecimal("1.0"), EntradaNota.leer("1", errores));
        assertEquals(new BigDecimal("7.0"), EntradaNota.leer(" 7.0 ", errores));
        assertTrue(errores.isEmpty());
    }

    @Test
    @DisplayName("2. Rechaza notas vacías, no numéricas, fuera de rango o con más de un decimal")
    void notasInvalidas() {
        for (String invalida : new String[]{null, "", "  ", "abc", "0.9", "7.1", "8", "-3", "5.55"}) {
            List<String> errores = new ArrayList<>();

            assertNull(EntradaNota.leer(invalida, errores), "debería rechazar '" + invalida + "'");
            assertEquals(1, errores.size());
        }
    }

    @Test
    @DisplayName("3. El nombre de descarga no lleva tildes ni caracteres raros")
    void nombreDeDescarga() {
        assertEquals("Informe-de-practica-1.pdf", ArchivosPdf.nombreDeDescarga("Informe de práctica 1"));
        assertEquals("Informe_Camila-Rojas.pdf", ArchivosPdf.nombreDeDescarga("Informe", "Camila Rojas"));
        assertEquals("entrega.pdf", ArchivosPdf.nombreDeDescarga("???", null));
    }

    private static MultipartFile archivo(String nombre, long tamano, byte[] contenido, boolean vacio) throws Exception {
        MultipartFile archivo = mock(MultipartFile.class);
        when(archivo.isEmpty()).thenReturn(vacio);
        when(archivo.getSize()).thenReturn(tamano);
        when(archivo.getOriginalFilename()).thenReturn(nombre);
        when(archivo.getInputStream()).thenReturn(new ByteArrayInputStream(contenido));
        return archivo;
    }

    @Test
    @DisplayName("4. Un PDF válido pasa la validación")
    void pdfValido() throws Exception {
        ArchivosPdf.validar(archivo("tarea.PDF", 1000, "%PDF-1.7 contenido".getBytes(), false));
    }

    @Test
    @DisplayName("5. No pasan: sin archivo, vacío, muy grande, con otra extensión o sin cabecera PDF")
    void pdfInvalido() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> ArchivosPdf.validar(null));
        assertThrows(IllegalArgumentException.class,
                () -> ArchivosPdf.validar(archivo("a.pdf", 0, new byte[0], true)));
        assertThrows(IllegalArgumentException.class,
                () -> ArchivosPdf.validar(archivo("a.pdf", ArchivosPdf.TAMANO_MAXIMO_BYTES + 1, "%PDF-".getBytes(), false)));
        assertThrows(IllegalArgumentException.class,
                () -> ArchivosPdf.validar(archivo("a.docx", 10, "%PDF-".getBytes(), false)));
        assertThrows(IllegalArgumentException.class,
                () -> ArchivosPdf.validar(archivo("a.pdf", 10, "texto".getBytes(), false)));
    }
}
