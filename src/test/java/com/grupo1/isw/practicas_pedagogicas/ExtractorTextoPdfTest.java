package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.services.ExtractorTextoPdf;
import com.grupo1.isw.practicas_pedagogicas.services.ExtractorTextoPdf.Resultado;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HU-31/32: lectura del texto del PDF de la planificación. Los PDF se fabrican en el propio test. */
class ExtractorTextoPdfTest {

    private final ExtractorTextoPdf extractor = new ExtractorTextoPdf();

    /** PDF con la misma línea de texto en cada página; con texto null las páginas quedan en blanco. */
    private static byte[] pdf(int paginas, String texto, boolean conContrasena) throws IOException {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int i = 0; i < paginas; i++) {
                PDPage pagina = new PDPage();
                documento.addPage(pagina);
                if (texto != null) {
                    try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
                        contenido.beginText();
                        contenido.setFont(fuente, 12);
                        contenido.newLineAtOffset(50, 700);
                        contenido.showText(texto);
                        contenido.endText();
                    }
                }
            }
            if (conContrasena) {
                documento.protect(new StandardProtectionPolicy("dueno", "usuario", new AccessPermission()));
            }
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    @Test
    @DisplayName("HU-31/32. Extrae el texto del PDF, con tildes, y cuenta sus páginas")
    void extraeElTextoYLasPaginas() throws IOException {
        Resultado resultado = extractor.extraer(pdf(2, "Objetivo de la clase: comprensión de fracciones", false));

        assertEquals(2, resultado.paginas());
        assertTrue(resultado.texto().contains("Objetivo de la clase: comprensión de fracciones"), resultado.texto());
    }

    @Test
    @DisplayName("HU-31/32. Un PDF sin texto (como un escaneo) devuelve texto vacío para que el servicio lo rechace")
    void pdfSinTexto_devuelveTextoVacio() throws IOException {
        Resultado resultado = extractor.extraer(pdf(1, null, false));

        assertEquals("", resultado.texto());
        assertEquals(1, resultado.paginas());
    }

    @Test
    @DisplayName("HU-31/32. Un PDF protegido con contraseña lanza IllegalArgument")
    void pdfConContrasena_lanzaIllegalArgument() throws IOException {
        byte[] protegido = pdf(1, "Texto secreto", true);

        assertThrows(IllegalArgumentException.class, () -> extractor.extraer(protegido));
    }

    @Test
    @DisplayName("HU-31/32. Bytes que no son un PDF lanzan IllegalArgument con un mensaje para el usuario")
    void noEsPdf_lanzaIllegalArgument() {
        byte[] basura = "%PDF-1.4 esto no es un pdf de verdad".getBytes(StandardCharsets.US_ASCII);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> extractor.extraer(basura));

        assertTrue(ex.getMessage().contains("No se pudo leer el PDF"), ex.getMessage());
    }

    @Test
    @DisplayName("HU-31/32. Un PDF con más páginas que el máximo lanza IllegalArgument")
    void demasiadasPaginas_lanzaIllegalArgument() throws IOException {
        byte[] largo = pdf(41, null, false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> extractor.extraer(largo));

        assertTrue(ex.getMessage().contains("demasiadas páginas"), ex.getMessage());
    }

    @Test
    @DisplayName("HU-31/32. Un PDF con exactamente el máximo de páginas se acepta")
    void maximoDePaginas_seAcepta() throws IOException {
        assertEquals(40, extractor.extraer(pdf(40, null, false)).paginas());
    }
}
