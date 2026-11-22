package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository.ContextoEntrega;
import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository.EntregaDelEstudiante;
import com.grupo1.isw.practicas_pedagogicas.services.AlmacenArchivos;
import com.grupo1.isw.practicas_pedagogicas.services.EntregaEstudianteService;
import com.grupo1.isw.practicas_pedagogicas.services.EntregaEstudianteService.GrupoEntregas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntregaEstudianteServiceTest {
    private static final long ESTUDIANTE = 7L;
    private static final long INSCRIPCION = 10L;
    private static final long ENTREGA = 20L;
    private static final LocalDate HOY = LocalDate.of(2026, 5, 15);

    @Mock
    EntregaEstudianteRepository repositorio;
    @Mock
    AlmacenArchivos almacen;

    private EntregaEstudianteService servicio;

    @BeforeEach
    void preparar() {
        // 15 de mayo de 2026, mediodía en Chile
        Clock reloj = Clock.fixed(Instant.parse("2026-05-15T16:00:00Z"), ZoneId.of("America/Santiago"));
        servicio = new EntregaEstudianteService(repositorio, almacen, reloj);
        when(almacen.guardar(org.mockito.ArgumentMatchers.<MultipartFile>any(), anyString())).thenReturn("nuevo.pdf");
        usar(contexto(true, "EN_CURSO", HOY.plusDays(3), null, null, null));
    }

    private void usar(ContextoEntrega contexto) {
        when(repositorio.contexto(INSCRIPCION, ENTREGA)).thenReturn(Optional.of(contexto));
    }

    private static ContextoEntrega contexto(boolean iniciada, String estado, LocalDate limite, Long idRegistro,
                                            String archivo, BigDecimal nota) {
        return new ContextoEntrega(INSCRIPCION, ESTUDIANTE, estado, 5L, iniciada, 5L, "Informe 1", true, limite,
                idRegistro, archivo, null, nota, null, false, false);
    }

    private static MultipartFile pdf() throws Exception {
        MultipartFile archivo = mock(MultipartFile.class);
        when(archivo.isEmpty()).thenReturn(false);
        when(archivo.getSize()).thenReturn(1000L);
        when(archivo.getOriginalFilename()).thenReturn("informe.pdf");
        when(archivo.getInputStream()).thenReturn(new ByteArrayInputStream("%PDF-1.4".getBytes()));
        return archivo;
    }

    @Test
    @DisplayName("1. Sube el PDF por primera vez antes de la fecha límite")
    void subePrimeraVez() throws Exception {
        boolean reemplazo = servicio.subir(ESTUDIANTE, INSCRIPCION, ENTREGA, pdf());

        assertFalse(reemplazo);
        verify(repositorio).insertarArchivo(INSCRIPCION, ENTREGA, "nuevo.pdf", LocalDateTime.of(2026, 5, 15, 12, 0));
    }

    @Test
    @DisplayName("2. Reemplaza el PDF anterior y borra el archivo viejo del disco")
    void reemplaza() throws Exception {
        usar(contexto(true, "EN_CURSO", HOY.plusDays(3), 99L, "viejo.pdf", null));

        boolean reemplazo = servicio.subir(ESTUDIANTE, INSCRIPCION, ENTREGA, pdf());

        assertTrue(reemplazo);
        verify(repositorio).reemplazarArchivo(99L, "nuevo.pdf", LocalDateTime.of(2026, 5, 15, 12, 0));
        verify(almacen).eliminar("viejo.pdf");
    }

    @Test
    @DisplayName("3. El mismo día de la fecha límite todavía se puede entregar")
    void ultimoDia() throws Exception {
        usar(contexto(true, "EN_CURSO", HOY, null, null, null));

        servicio.subir(ESTUDIANTE, INSCRIPCION, ENTREGA, pdf());

        verify(repositorio).insertarArchivo(INSCRIPCION, ENTREGA, "nuevo.pdf", LocalDateTime.of(2026, 5, 15, 12, 0));
    }

    @Test
    @DisplayName("4. Un día después de la fecha límite no se puede subir ni reemplazar")
    void plazoVencido() throws Exception {
        usar(contexto(true, "EN_CURSO", HOY.minusDays(1), 99L, "viejo.pdf", null));

        assertThrows(IllegalStateException.class, () -> servicio.subir(ESTUDIANTE, INSCRIPCION, ENTREGA, pdf()));

        verify(almacen, never()).guardar(org.mockito.ArgumentMatchers.<MultipartFile>any(), anyString());
        verify(almacen, never()).eliminar(anyString());
    }

    @Test
    @DisplayName("5. Sin fecha límite el plazo no vence")
    void sinFechaLimite() throws Exception {
        usar(contexto(true, "EN_CURSO", null, null, null, null));

        servicio.subir(ESTUDIANTE, INSCRIPCION, ENTREGA, pdf());

        verify(repositorio).insertarArchivo(INSCRIPCION, ENTREGA, "nuevo.pdf", LocalDateTime.of(2026, 5, 15, 12, 0));
    }

    @Test
    @DisplayName("6. No se entrega en un semestre sin iniciar, con inscripción cerrada ni con la entrega ya calificada")
    void otrasRestricciones() throws Exception {
        usar(contexto(false, "EN_CURSO", HOY.plusDays(3), null, null, null));
        assertThrows(IllegalStateException.class, () -> servicio.subir(ESTUDIANTE, INSCRIPCION, ENTREGA, pdf()));

        usar(contexto(true, "RETIRADO", HOY.plusDays(3), null, null, null));
        assertThrows(IllegalStateException.class, () -> servicio.subir(ESTUDIANTE, INSCRIPCION, ENTREGA, pdf()));

        usar(contexto(true, "EN_CURSO", HOY.plusDays(3), 99L, "viejo.pdf", new BigDecimal("5.0")));
        assertThrows(IllegalStateException.class, () -> servicio.subir(ESTUDIANTE, INSCRIPCION, ENTREGA, pdf()));

        verify(repositorio, never()).insertarArchivo(anyLong(), anyLong(), anyString(),
                org.mockito.ArgumentMatchers.<LocalDateTime>any());
    }

    @Test
    @DisplayName("7. Otro estudiante no puede entregar ni descargar (403) y una entrega ajena da 404")
    void controlDeAcceso() throws Exception {
        ResponseStatusException ajeno = assertThrows(ResponseStatusException.class,
                () -> servicio.subir(999L, INSCRIPCION, ENTREGA, pdf()));
        assertThrows(ResponseStatusException.class, () -> servicio.descargar(999L, INSCRIPCION, ENTREGA));

        usar(new ContextoEntrega(INSCRIPCION, ESTUDIANTE, "EN_CURSO", 5L, true, 6L, "Otra", true, null, null, null,
                null, null, null, false, false));
        assertThrows(ResponseStatusException.class, () -> servicio.subir(ESTUDIANTE, INSCRIPCION, ENTREGA, pdf()));

        usar(new ContextoEntrega(INSCRIPCION, ESTUDIANTE, "EN_CURSO", 5L, true, 5L, "Deshabilitada", false, null,
                null, null, null, null, null, false, false));
        assertThrows(ResponseStatusException.class, () -> servicio.subir(ESTUDIANTE, INSCRIPCION, ENTREGA, pdf()));
        assertTrue(ajeno != null);
    }

    @Test
    @DisplayName("8. Un archivo que no es PDF se rechaza y no se guarda nada")
    void noEsPdf() throws Exception {
        MultipartFile texto = mock(MultipartFile.class);
        when(texto.isEmpty()).thenReturn(false);
        when(texto.getSize()).thenReturn(10L);
        when(texto.getOriginalFilename()).thenReturn("nota.txt");

        assertThrows(IllegalArgumentException.class, () -> servicio.subir(ESTUDIANTE, INSCRIPCION, ENTREGA, texto));

        verify(almacen, never()).guardar(org.mockito.ArgumentMatchers.<MultipartFile>any(), anyString());
    }

    @Test
    @DisplayName("9. Si falla el guardado en la base, el archivo nuevo se borra del disco")
    void fallaLaBase() throws Exception {
        doThrow(new RuntimeException("base caída")).when(repositorio)
                .insertarArchivo(INSCRIPCION, ENTREGA, "nuevo.pdf", LocalDateTime.of(2026, 5, 15, 12, 0));

        assertThrows(RuntimeException.class, () -> servicio.subir(ESTUDIANTE, INSCRIPCION, ENTREGA, pdf()));

        verify(almacen).eliminar("nuevo.pdf");
    }

    private static EntregaDelEstudiante fila(long idEntrega, LocalDate limite, boolean archivo, BigDecimal nota,
                                             boolean publicada, String estadoInscripcion) {
        return new EntregaDelEstudiante(INSCRIPCION, estadoInscripcion, 5L, "Práctica", 2026, 1, idEntrega,
                "Entrega " + idEntrega, new BigDecimal("25.00"), limite, archivo || nota != null ? 1L : null, archivo,
                archivo ? LocalDateTime.of(2026, 5, 1, 10, 0) : null, nota, null, false, publicada);
    }

    @Test
    @DisplayName("10. La lista marca entregada, no entregada y pendiente, y cuándo se puede subir")
    void listaEstados() {
        when(repositorio.delEstudiante(ESTUDIANTE)).thenReturn(List.of(
                fila(1, HOY.plusDays(2), true, null, false, "EN_CURSO"),
                fila(2, HOY.minusDays(1), false, null, false, "EN_CURSO"),
                fila(3, HOY.plusDays(5), false, null, false, "EN_CURSO"),
                fila(4, null, true, new BigDecimal("6.0"), true, "EN_CURSO")));

        List<GrupoEntregas> grupos = servicio.listar(ESTUDIANTE);

        assertEquals(1, grupos.size());
        var entregas = grupos.get(0).entregas();
        assertEquals(EntregaEstudianteService.ENTREGADA, entregas.get(0).estado());
        assertTrue(entregas.get(0).puedeSubir());
        assertEquals(EntregaEstudianteService.NO_ENTREGADA, entregas.get(1).estado());
        assertFalse(entregas.get(1).puedeSubir());
        assertEquals(EntregaEstudianteService.PENDIENTE, entregas.get(2).estado());
        assertTrue(entregas.get(2).puedeSubir());
        assertFalse(entregas.get(3).puedeSubir(), "una entrega calificada no se reemplaza");
        assertTrue(entregas.get(3).notaVisible());
    }

    @Test
    @DisplayName("11. La nota solo se ve cuando el profesor la publicó")
    void notaOculta() {
        when(repositorio.delEstudiante(ESTUDIANTE)).thenReturn(List.of(
                fila(1, HOY.minusDays(1), true, new BigDecimal("6.0"), false, "EN_CURSO")));

        assertFalse(servicio.listar(ESTUDIANTE).get(0).entregas().get(0).notaVisible());
    }

    @Test
    @DisplayName("12. El plazo vence al terminar el día de la fecha límite")
    void reglaDelPlazo() {
        assertFalse(EntregaEstudianteService.plazoVencido(null, HOY));
        assertFalse(EntregaEstudianteService.plazoVencido(HOY, HOY));
        assertTrue(EntregaEstudianteService.plazoVencido(HOY.minusDays(1), HOY));
    }
}
