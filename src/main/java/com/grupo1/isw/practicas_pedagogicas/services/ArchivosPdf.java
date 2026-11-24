package com.grupo1.isw.practicas_pedagogicas.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

/** Reglas comunes para los PDF que suben los estudiantes (mismas que usa DocumentoService). */
public final class ArchivosPdf {
    public static final String EXTENSION = "pdf";
    public static final long TAMANO_MAXIMO_BYTES = 20L * 1024 * 1024;

    private ArchivosPdf() {
    }

    /** @throws IllegalArgumentException si falta el archivo, es muy grande o no es un PDF */
    public static void validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar un archivo");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo de 20 MB");
        }
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !nombre.toLowerCase(Locale.ROOT).endsWith("." + EXTENSION)) {
            throw new IllegalArgumentException("Solo se permiten archivos PDF");
        }
        if (!empiezaComoPdf(archivo)) {
            throw new IllegalArgumentException("El archivo no es un PDF válido");
        }
    }

    /** La extensión se puede falsificar renombrando el archivo; la cabecera "%PDF-" es más difícil de fingir. */
    private static boolean empiezaComoPdf(MultipartFile archivo) {
        byte[] esperado = "%PDF-".getBytes(StandardCharsets.US_ASCII);
        try (InputStream entrada = archivo.getInputStream()) {
            byte[] inicio = entrada.readNBytes(esperado.length);
            return java.util.Arrays.equals(inicio, esperado);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo", e);
        }
    }

    /** Nombre de descarga legible y seguro: sin tildes ni caracteres raros, terminado en .pdf. */
    public static String nombreDeDescarga(String... partes) {
        StringBuilder nombre = new StringBuilder();
        for (String parte : partes) {
            if (parte == null || parte.isBlank()) {
                continue;
            }
            String sinTildes = Normalizer.normalize(parte, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
            String limpio = sinTildes.trim().replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("^-+|-+$", "");
            if (limpio.isEmpty()) {
                continue;
            }
            if (nombre.length() > 0) {
                nombre.append('_');
            }
            nombre.append(limpio);
        }
        return (nombre.length() == 0 ? "entrega" : nombre) + "." + EXTENSION;
    }
}
