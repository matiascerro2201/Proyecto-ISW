package com.grupo1.isw.practicas_pedagogicas.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Guarda y borra archivos en la carpeta configurada en app.uploads.dir.
 * El nombre en disco lo genera siempre el sistema (UUID): el nombre que trae
 * el usuario nunca se usa, así no puede escribir fuera de la carpeta.
 */
@Component
public class AlmacenArchivos {
    private static final Logger log = LoggerFactory.getLogger(AlmacenArchivos.class);

    private final Path directorio;

    public AlmacenArchivos(@Value("${app.uploads.dir}") String directorio) {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.directorio);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de archivos: " + this.directorio, e);
        }
    }

    /** Guarda el archivo con un nombre nuevo (UUID + extensión) y devuelve ese nombre. */
    public String guardar(MultipartFile archivo, String extension) {
        String nombre = UUID.randomUUID() + "." + extension;
        try (InputStream entrada = archivo.getInputStream()) {
            Files.copy(entrada, resolver(nombre));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }
        return nombre;
    }

    /** Borra el archivo si existe. Si no se puede borrar, lo deja registrado pero no falla. */
    public void eliminar(String nombre) {
        try {
            Files.deleteIfExists(resolver(nombre));
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo {}", nombre, e);
        }
    }

    /** Ruta completa dentro de la carpeta de archivos; rechaza nombres que intenten salirse de ella. */
    public Path resolver(String nombre) {
        Path ruta = directorio.resolve(nombre).normalize();
        if (!ruta.startsWith(directorio)) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }
        return ruta;
    }
}
