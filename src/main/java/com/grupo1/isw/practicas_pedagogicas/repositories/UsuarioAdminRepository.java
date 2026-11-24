package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.CentroOpcion;
import com.grupo1.isw.practicas_pedagogicas.models.CentroPractica;
import com.grupo1.isw.practicas_pedagogicas.models.RolUsuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * HU-36 y HU-39: operaciones sobre las tablas por rol (estudiante, profesor_colaborador, profesor_tutor,
 * profesor_asignatura).
 *
 * <p>Usuario usa herencia JOINED: la fila de cada rol vive en su propia tabla. JPA no permite cambiar la clase
 * de una entidad ya guardada, así que crear una cuenta y cambiar su rol se hace con SQL directo sobre esas
 * tablas. Todos los métodos exigen una transacción abierta (la abre el servicio).
 */
@Repository
public class UsuarioAdminRepository {

    /** El sistema es solo para Pedagogía en Matemática: todo estudiante nuevo queda con esta carrera. */
    public static final String CARRERA_ESTUDIANTES = "Pedagogía en Matemática";

    @PersistenceContext
    private EntityManager em;

    /** Crea la fila de la tabla del rol. COORDINADOR no tiene tabla propia. */
    public void insertarSubtipo(Long idUsuario, RolUsuario rol, Long idCentro) {
        switch (rol) {
            case ESTUDIANTE -> ejecutar("INSERT INTO estudiante (id_usuario, carrera) VALUES (?1, ?2)",
                    idUsuario, CARRERA_ESTUDIANTES);
            case COLABORADOR -> ejecutar("INSERT INTO profesor_colaborador (id_usuario, id_centro) VALUES (?1, ?2)",
                    idUsuario, idCentro);
            case TUTOR -> ejecutar("INSERT INTO profesor_tutor (id_usuario) VALUES (?1)", idUsuario);
            case PROFESOR_ASIGNATURA -> ejecutar("INSERT INTO profesor_asignatura (id_usuario) VALUES (?1)", idUsuario);
            case COORDINADOR -> {
            }
        }
    }

    /** Actualiza los datos propios del rol (hoy solo el centro del colaborador). */
    public void actualizarSubtipo(Long idUsuario, RolUsuario rol, Long idCentro) {
        switch (rol) {
            case COLABORADOR -> ejecutar("UPDATE profesor_colaborador SET id_centro = ?2 WHERE id_usuario = ?1",
                    idUsuario, idCentro);
            case ESTUDIANTE, TUTOR, PROFESOR_ASIGNATURA, COORDINADOR -> {
            }
        }
    }

    /**
     * Cambia el rol: borra la fila del rol anterior, actualiza usuario.rol y crea la del rol nuevo. Antes escribe
     * lo pendiente y después vacía el contexto de persistencia, para que no quede en memoria una entidad
     * de la clase anterior.
     */
    public void reemplazarSubtipo(Long idUsuario, RolUsuario actual, RolUsuario nuevo, Long idCentro) {
        em.flush();
        eliminarSubtipo(idUsuario, actual);
        ejecutar("UPDATE usuario SET rol = ?2 WHERE id_usuario = ?1", idUsuario, nuevo.name());
        insertarSubtipo(idUsuario, nuevo, idCentro);
        em.clear();
    }

    private void eliminarSubtipo(Long idUsuario, RolUsuario rol) {
        switch (rol) {
            case ESTUDIANTE -> ejecutar("DELETE FROM estudiante WHERE id_usuario = ?1", idUsuario);
            case COLABORADOR -> ejecutar("DELETE FROM profesor_colaborador WHERE id_usuario = ?1", idUsuario);
            case TUTOR -> ejecutar("DELETE FROM profesor_tutor WHERE id_usuario = ?1", idUsuario);
            case PROFESOR_ASIGNATURA -> ejecutar("DELETE FROM profesor_asignatura WHERE id_usuario = ?1", idUsuario);
            case COORDINADOR -> {
            }
        }
    }

    /**
     * Registros que dependen del usuario por su rol actual (inscripciones, líneas, cursos que dicta, informes que
     * corrigió). Si hay alguno, cambiar el rol dejaría datos huérfanos.
     */
    public long contarRegistrosAsociados(Long idUsuario, RolUsuario rol) {
        String sql = switch (rol) {
            case ESTUDIANTE -> "SELECT COUNT(*) FROM inscripcion_asignatura WHERE id_estudiante = ?1";
            case COLABORADOR -> "SELECT COUNT(*) FROM linea_practica WHERE id_colaborador = ?1";
            case TUTOR -> "SELECT COUNT(*) FROM linea_practica WHERE id_tutor = ?1";
            case PROFESOR_ASIGNATURA -> "SELECT (SELECT COUNT(*) FROM seccion_asignatura WHERE id_profesor = ?1)"
                    + " + (SELECT COUNT(*) FROM informe_final WHERE id_corrector = ?1)";
            case COORDINADOR -> null;
        };
        if (sql == null) {
            return 0;
        }
        Object resultado = em.createNativeQuery(sql).setParameter(1, idUsuario).getSingleResult();
        return ((Number) resultado).longValue();
    }

    public Long centroDe(Long idUsuario) {
        List<Long> centros = em.createQuery(
                        "SELECT c.centro.idCentro FROM ProfesorColaborador c WHERE c.idUsuario = :id", Long.class)
                .setParameter("id", idUsuario)
                .getResultList();
        return centros.isEmpty() ? null : centros.get(0);
    }

    public List<CentroOpcion> centros() {
        return em.createQuery("SELECT c FROM CentroPractica c ORDER BY c.nombre", CentroPractica.class)
                .getResultList().stream()
                .map(c -> new CentroOpcion(c.getIdCentro(), c.getNombre(), c.getComuna()))
                .toList();
    }

    private void ejecutar(String sql, Object... parametros) {
        var consulta = em.createNativeQuery(sql);
        for (int i = 0; i < parametros.length; i++) {
            consulta.setParameter(i + 1, parametros[i]);
        }
        consulta.executeUpdate();
    }
}
