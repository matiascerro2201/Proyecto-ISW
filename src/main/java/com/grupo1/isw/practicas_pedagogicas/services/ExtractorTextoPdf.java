package com.grupo1.isw.practicas_pedagogicas.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Lee el texto de un PDF con Apache PDFBox. No guarda nada: solo valida y extrae. */
@Component
public class ExtractorTextoPdf {
    /** La planificación en formato A tiene 2 páginas; este tope evita PDFs enormes que cuesten mucho procesar. */
    static final int MAX_PAGINAS = 40;

    public record Resultado(String texto, int paginas) {
    }

    /**
     * @throws IllegalArgumentException si el PDF está dañado, protegido con contraseña o tiene demasiadas páginas
     */
    public Resultado extraer(byte[] contenido) {
        try (PDDocument documento = Loader.loadPDF(contenido)) {
            if (documento.isEncrypted()) {
                throw new IllegalArgumentException("El PDF está protegido con contraseña; sube una copia sin protección");
            }
            int paginas = documento.getNumberOfPages();
            if (paginas > MAX_PAGINAS) {
                throw new IllegalArgumentException("El PDF tiene demasiadas páginas (máximo " + MAX_PAGINAS + ")");
            }
            PDFTextStripper lector = new PDFTextStripper();
            lector.setSortByPosition(true); // respeta el orden de lectura en las tablas del formato
            return new Resultado(lector.getText(documento).trim(), paginas);
        } catch (IOException e) {
            // Incluye PDFs dañados y los que piden contraseña para abrirse.
            throw new IllegalArgumentException("No se pudo leer el PDF; revisa que no esté dañado ni protegido");
        }
    }
}
