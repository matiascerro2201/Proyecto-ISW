package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.PerfilResponse;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.CentroPractica;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoInscripcion;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorColaborador;
import com.grupo1.isw.practicas_pedagogicas.models.RolUsuario;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.TipoCentro;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.models.Usuario;
import com.grupo1.isw.practicas_pedagogicas.repositories.InscripcionAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.UsuarioRepository;
import com.grupo1.isw.practicas_pedagogicas.services.PerfilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PerfilServiceTest {
    @Mock
    UsuarioRepository usuarios;
    @Mock
    InscripcionAsignaturaRepository inscripciones;
    @Mock
    LineaPracticaRepository lineas;
    @Mock
    SeccionAsignaturaRepository secciones;

    private PerfilService servicio;
    private CentroPractica centro;
    private ProfesorAsignatura profesor;

    @BeforeEach
    void preparar() {
        servicio = new PerfilService(usuarios, inscripciones, lineas, secciones);
        centro = new CentroPractica("Liceo Pedro de Valdivia", "Chillán", TipoCentro.MUNICIPAL);
        profesor = new ProfesorAsignatura();
        profesor.setIdUsuario(50L);
    }

    private Estudiante estudiante(long id, String nombre) {
        Estudiante e = new Estudiante();
        e.setIdUsuario(id);
        e.setNombre(nombre);
        e.setEmail(nombre.toLowerCase().replace(' ', '.') + "@alumnos.ubiobio.cl");
        e.setRol(RolUsuario.ESTUDIANTE);
        e.setCarrera("Pedagogía en Matemática");
        return e;
    }

    private InscripcionAsignatura inscripcion(long id, Estudiante est, String asignatura, int anio, int semestre,
                                              EstadoInscripcion estado) {
        Asignatura a = new Asignatura(asignatura, null, 1);
        SeccionAsignatura sec = new SeccionAsignatura(a, profesor, anio, semestre);
        sec.setIdSeccion(id * 10);
        InscripcionAsignatura ins = new InscripcionAsignatura(est, sec, centro);
        ins.setIdInscripcion(id);
        ins.setEstado(estado);
        return ins;
    }

    @Test
    @DisplayName("1. El estudiante ve sus cursos en curso como actuales y el resto como pasados, el más reciente primero")
    void estudianteSeparaActualesYPasados() {
        Estudiante est = estudiante(1, "Ana Soto");
        when(usuarios.findById(1L)).thenReturn(Optional.of(est));
        when(inscripciones.findByEstudianteConAsignatura(1L)).thenReturn(List.of(
                inscripcion(1, est, "Práctica Intermedia", 2026, 2, EstadoInscripcion.EN_CURSO),
                inscripcion(2, est, "Práctica Inicial", 2025, 1, EstadoInscripcion.APROBADO),
                inscripcion(3, est, "Didáctica", 2025, 2, EstadoInscripcion.REPROBADO)));

        PerfilResponse perfil = servicio.obtener(1L);

        assertEquals("Ana Soto", perfil.nombre());
        assertEquals("Estudiante", perfil.rol());
        assertEquals("Pedagogía en Matemática", perfil.carrera());
        assertTrue(perfil.tieneCursos());
        assertEquals(1, perfil.cursosActuales().size());
        assertEquals("Práctica Intermedia", perfil.cursosActuales().get(0).asignatura());
        assertEquals("2026-2", perfil.cursosActuales().get(0).periodo());
        assertEquals("Liceo Pedro de Valdivia", perfil.cursosActuales().get(0).detalle());
        assertEquals(List.of("Didáctica", "Práctica Inicial"),
                perfil.cursosPasados().stream().map(PerfilResponse.Curso::asignatura).toList());
        assertEquals("Reprobado", perfil.cursosPasados().get(0).estado());
    }

    @Test
    @DisplayName("2. Un estudiante sin inscripciones recibe ambas listas vacías")
    void estudianteSinCursos() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(estudiante(1, "Ana Soto")));
        when(inscripciones.findByEstudianteConAsignatura(1L)).thenReturn(List.of());

        PerfilResponse perfil = servicio.obtener(1L);

        assertTrue(perfil.cursosActuales().isEmpty());
        assertTrue(perfil.cursosPasados().isEmpty());
    }

    @Test
    @DisplayName("3. El colaborador ve una fila por estudiante aunque tenga varias líneas con él, y su centro")
    void colaboradorUnaFilaPorEstudiante() {
        ProfesorColaborador col = new ProfesorColaborador();
        col.setIdUsuario(20L);
        col.setNombre("Luis Rojas");
        col.setEmail("luis@liceo.cl");
        col.setRol(RolUsuario.COLABORADOR);
        col.setCentro(centro);
        when(usuarios.findById(20L)).thenReturn(Optional.of(col));

        InscripcionAsignatura ins = inscripcion(7, estudiante(1, "Ana Soto"), "Práctica Final", 2026, 2,
                EstadoInscripcion.EN_CURSO);
        when(lineas.findByColaborador(20L)).thenReturn(List.of(
                new LineaPractica(ins, col, null, TipoLinea.values()[0]),
                new LineaPractica(ins, col, null, TipoLinea.values()[TipoLinea.values().length - 1])));

        PerfilResponse perfil = servicio.obtener(20L);

        assertEquals("Profesor colaborador", perfil.rol());
        assertEquals("Liceo Pedro de Valdivia", perfil.centro());
        assertNull(perfil.carrera());
        assertEquals("Estudiante", perfil.etiquetaDetalle());
        assertEquals(1, perfil.cursosActuales().size());
        assertEquals("Ana Soto", perfil.cursosActuales().get(0).detalle());
    }

    @Test
    @DisplayName("4. El profesor de asignatura ve una sección como actual solo si algún estudiante sigue en curso")
    void profesorSeccionesVigentes() {
        Estudiante a = estudiante(1, "Ana Soto");
        Estudiante b = estudiante(2, "Beto Vera");
        profesor.setNombre("Marta Díaz");
        profesor.setEmail("marta@ubiobio.cl");
        profesor.setRol(RolUsuario.PROFESOR_ASIGNATURA);
        when(usuarios.findById(50L)).thenReturn(Optional.of(profesor));

        InscripcionAsignatura vigente1 = inscripcion(1, a, "Práctica Intermedia", 2026, 2, EstadoInscripcion.EN_CURSO);
        InscripcionAsignatura vigente2 = inscripcion(2, b, "Práctica Intermedia", 2026, 2, EstadoInscripcion.APROBADO);
        vigente2.setSeccion(vigente1.getSeccion());
        InscripcionAsignatura vieja = inscripcion(3, a, "Práctica Inicial", 2025, 1, EstadoInscripcion.APROBADO);
        when(inscripciones.findByProfesor(50L)).thenReturn(List.of(vigente1, vigente2, vieja));
        when(secciones.findDelProfesor(50L)).thenReturn(List.of(vigente1.getSeccion(), vieja.getSeccion()));

        PerfilResponse perfil = servicio.obtener(50L);

        assertEquals(1, perfil.cursosActuales().size());
        assertEquals("2 estudiantes", perfil.cursosActuales().get(0).detalle());
        assertEquals(1, perfil.cursosPasados().size());
        assertEquals("Práctica Inicial", perfil.cursosPasados().get(0).asignatura());
        assertEquals("1 estudiante", perfil.cursosPasados().get(0).detalle());
    }

    @Test
    @DisplayName("5. El coordinador no tiene cursos asociados")
    void coordinadorSinCursos() {
        Usuario coord = new Usuario("Carla Mena", "carla@ubiobio.cl", "x", RolUsuario.COORDINADOR);
        when(usuarios.findById(9L)).thenReturn(Optional.of(coord));

        PerfilResponse perfil = servicio.obtener(9L);

        assertFalse(perfil.tieneCursos());
        assertEquals("Coordinador de prácticas", perfil.rol());
        assertTrue(perfil.cursosActuales().isEmpty());
        assertTrue(perfil.cursosPasados().isEmpty());
    }

    @Test
    @DisplayName("6. Un usuario inexistente produce 404")
    void usuarioInexistente() {
        when(usuarios.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> servicio.obtener(99L));
    }
}
