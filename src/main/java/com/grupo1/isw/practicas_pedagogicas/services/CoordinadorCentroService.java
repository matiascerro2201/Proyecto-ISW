package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.CentroFila;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.CentroFormulario;
import com.grupo1.isw.practicas_pedagogicas.models.CentroPractica;
import com.grupo1.isw.practicas_pedagogicas.models.TipoCentro;
import com.grupo1.isw.practicas_pedagogicas.repositories.CentroPracticaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * El coordinador crea, edita, lista y elimina los centros de práctica. Sin esta pantalla no había forma de
 * registrar un centro nuevo, y sin centros no se puede crear un profesor colaborador ni inscribir a un estudiante.
 *
 * <p>Errores:
 * <ul>
 *   <li>{@link IllegalArgumentException}: el formulario tiene un error; el mensaje es para el usuario.</li>
 *   <li>{@link IllegalStateException}: la operación no corresponde (borrar un centro que ya está en uso).</li>
 *   <li>{@link ResponseStatusException} 404: el centro no existe.</li>
 * </ul>
 */
@Service
public class CoordinadorCentroService {
    static final int LARGO_NOMBRE = 200;
    static final int LARGO_COMUNA = 100;

    private final CentroPracticaRepository centroRepository;

    public CoordinadorCentroService(CentroPracticaRepository centroRepository) {
        this.centroRepository = centroRepository;
    }

    @Transactional(readOnly = true)
    public List<CentroFila> listar() {
        return centroRepository.findAllByOrderByNombreAsc().stream()
                .map(c -> new CentroFila(c.getIdCentro(), c.getNombre(), c.getComuna(), c.getTipo(),
                        centroRepository.contarColaboradores(c.getIdCentro()),
                        centroRepository.contarInscripciones(c.getIdCentro())))
                .toList();
    }

    /** El formulario de edición con los datos actuales. */
    @Transactional(readOnly = true)
    public CentroFormulario formularioDe(Long idCentro) {
        CentroPractica centro = obtener(idCentro);
        CentroFormulario formulario = new CentroFormulario();
        formulario.setNombre(centro.getNombre());
        formulario.setComuna(centro.getComuna());
        formulario.setTipo(centro.getTipo());
        return formulario;
    }

    @Transactional
    public Long crear(CentroFormulario formulario) {
        String nombre = nombreValido(formulario.getNombre());
        String comuna = comunaValida(formulario.getComuna());
        TipoCentro tipo = tipoValido(formulario.getTipo());
        validarQueNoSeRepita(null, nombre, comuna);

        CentroPractica centro = centroRepository.save(new CentroPractica(nombre, comuna, tipo));
        return centro.getIdCentro();
    }

    @Transactional
    public void actualizar(Long idCentro, CentroFormulario formulario) {
        CentroPractica centro = obtener(idCentro);
        String nombre = nombreValido(formulario.getNombre());
        String comuna = comunaValida(formulario.getComuna());
        TipoCentro tipo = tipoValido(formulario.getTipo());
        validarQueNoSeRepita(idCentro, nombre, comuna);

        centro.setNombre(nombre);
        centro.setComuna(comuna);
        centro.setTipo(tipo);
        centroRepository.save(centro);
    }

    /** Solo se borra un centro que nadie usa; si tiene colaboradores o inscripciones, el mensaje dice cuáles. */
    @Transactional
    public void eliminar(Long idCentro) {
        CentroPractica centro = obtener(idCentro);
        long colaboradores = centroRepository.contarColaboradores(idCentro);
        long inscripciones = centroRepository.contarInscripciones(idCentro);
        if (colaboradores > 0 || inscripciones > 0) {
            throw new IllegalStateException("No se puede eliminar \"" + centro.getNombre() + "\": lo usan "
                    + colaboradores + (colaboradores == 1 ? " profesor colaborador" : " profesores colaboradores")
                    + " y " + inscripciones + (inscripciones == 1 ? " inscripción" : " inscripciones") + ".");
        }
        centroRepository.delete(centro);
    }

    private CentroPractica obtener(Long idCentro) {
        return centroRepository.findById(idCentro)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Dos centros no pueden tener el mismo nombre en la misma comuna (sin importar mayúsculas ni tildes). */
    private void validarQueNoSeRepita(Long idPropio, String nombre, String comuna) {
        String nombreBuscado = TextoBusqueda.normalizar(nombre);
        String comunaBuscada = TextoBusqueda.normalizar(comuna);
        boolean repetido = centroRepository.findAllByOrderByNombreAsc().stream()
                .filter(otro -> !otro.getIdCentro().equals(idPropio))
                .anyMatch(otro -> TextoBusqueda.normalizar(otro.getNombre()).equals(nombreBuscado)
                        && TextoBusqueda.normalizar(otro.getComuna()).equals(comunaBuscada));
        if (repetido) {
            throw new IllegalArgumentException("Ya existe un centro con ese nombre en esa comuna.");
        }
    }

    private static String nombreValido(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Escribe el nombre del centro.");
        }
        String limpio = nombre.strip();
        if (limpio.length() > LARGO_NOMBRE) {
            throw new IllegalArgumentException("El nombre no puede pasar de " + LARGO_NOMBRE + " caracteres.");
        }
        return limpio;
    }

    /** La comuna es opcional: devuelve null si no se escribió. */
    private static String comunaValida(String comuna) {
        if (comuna == null || comuna.isBlank()) {
            return null;
        }
        String limpia = comuna.strip();
        if (limpia.length() > LARGO_COMUNA) {
            throw new IllegalArgumentException("La comuna no puede pasar de " + LARGO_COMUNA + " caracteres.");
        }
        return limpia;
    }

    private static TipoCentro tipoValido(TipoCentro tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("Elige el tipo de centro.");
        }
        return tipo;
    }
}
