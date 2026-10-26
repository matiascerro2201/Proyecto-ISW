package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.services.AlmacenArchivos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlmacenArchivosTest {

    private static final byte[] PDF = "%PDF-1.4 contenido de prueba".getBytes(StandardCharsets.US_ASCII);
    private static final String PATRON_UUID_PDF =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.pdf";

    @TempDir
    Path carpeta;

    private AlmacenArchivos almacen;

    @BeforeEach
    void crearAlmacen() {
        almacen = new AlmacenArchivos(carpeta.toString());
    }

    private MockMultipartFile pdf(String nombreOriginal) {
        return new MockMultipartFile("archivo", nombreOriginal, "application/pdf", PDF);
    }

    @Test
    @DisplayName("1. Crea la carpeta de archivos si no existe")
    void creaLaCarpetaSiNoExiste() {
        Path nueva = carpeta.resolve("no/existe/todavia");

        new AlmacenArchivos(nueva.toString());

        assertTrue(Files.isDirectory(nueva));
    }

    @Test
    @DisplayName("2. Guarda con nombre UUID y nunca con el nombre original")
    void guardar_usaNombreUuidYNuncaElOriginal() throws IOException {
        String nombre = almacen.guardar(pdf("../../mi informe secreto.pdf"), "pdf");

        assertTrue(nombre.matches(PATRON_UUID_PDF), "debe ser un UUID con extensión .pdf, pero fue: " + nombre);
        assertFalse(nombre.contains("informe"));
        assertArrayEquals(PDF, Files.readAllBytes(carpeta.resolve(nombre)));
    }

    @Test
    @DisplayName("3. Dos subidas del mismo archivo reciben nombres distintos")
    void guardar_dosVecesDaNombresDistintos() {
        String primero = almacen.guardar(pdf("informe.pdf"), "pdf");
        String segundo = almacen.guardar(pdf("informe.pdf"), "pdf");

        assertNotEquals(primero, segundo);
    }

    @ParameterizedTest(name = "4. Rechaza salirse de la carpeta: [{0}]")
    @ValueSource(strings = {"../fuera.txt", "../../etc/passwd", "sub/../../fuera.pdf", "/etc/passwd"})
    void resolver_rechazaSalirseDeLaCarpeta(String nombre) {
        assertThrows(IllegalArgumentException.class, () -> almacen.resolver(nombre));
    }

    @Test
    @DisplayName("5. Un nombre normal se resuelve dentro de la carpeta")
    void resolver_aceptaNombreNormal() {
        Path esperado = carpeta.toAbsolutePath().normalize().resolve("abc.pdf");

        assertEquals(esperado, almacen.resolver("abc.pdf"));
    }

    @Test
    @DisplayName("6. Elimina el archivo")
    void eliminar_borraElArchivo() {
        String nombre = almacen.guardar(pdf("informe.pdf"), "pdf");
        assertTrue(Files.exists(carpeta.resolve(nombre)));

        almacen.eliminar(nombre);

        assertFalse(Files.exists(carpeta.resolve(nombre)));
    }

    @Test
    @DisplayName("7. Eliminar un archivo que no existe no falla")
    void eliminar_archivoInexistente_noFalla() {
        assertDoesNotThrow(() -> almacen.eliminar("no-existe.pdf"));
    }

    @Test
    @DisplayName("8. No elimina archivos fuera de su carpeta")
    void eliminar_noSaleDeLaCarpeta() throws IOException {
        Path secreto = Files.write(carpeta.resolve("secreto.txt"), PDF);
        AlmacenArchivos almacenEnSubcarpeta = new AlmacenArchivos(carpeta.resolve("subidas").toString());

        assertThrows(IllegalArgumentException.class, () -> almacenEnSubcarpeta.eliminar("../secreto.txt"));

        assertTrue(Files.exists(secreto), "el archivo de fuera de la carpeta no debe borrarse");
    }
}
