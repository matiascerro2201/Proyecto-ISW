package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.InformeRevisionResponse;
import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeFinalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * HU-11: el profesor de la asignatura revisa la calificación sugerida por el sistema, la ajusta si
 * corresponde y recién entonces publica el informe final para el estudiante.
 *
 * <p>La calificación sugerida nunca se modifica: el ajuste del profesor se guarda en
 * {@code calificacionFinal}, de modo que siempre quede registro de qué propuso el sistema.
 */
@Service
public class RevisionInformeService {
    /** Escala de notas chilena. La columna es decimal(3,1): un solo decimal. */
    static final BigDecimal NOTA_MINIMA = new BigDecimal("1.0");
    static final BigDecimal NOTA_MAXIMA = new BigDecimal("7.0");
    private static final int LARGO_MAXIMO_TEXTO = 10;

    private final InformeFinalRepository informeFinalRepository;

    public RevisionInformeService(InformeFinalRepository informeFinalRepository) {
        this.informeFinalRepository = informeFinalRepository;
    }

    /** Informes del profesor: primero los que aún no publica, luego los publicados; más recientes arriba. */
    @Transactional(readOnly = true)
    public List<InformeRevisionResponse> listarInformes(Long idProfesor) {
        return informeFinalRepository.findParaRevisionDelProfesor(idProfesor).stream()
                .map(InformeRevisionResponse::desde)
                // sort estable: dentro de cada grupo se conserva el orden por fecha de la consulta
                .sorted(Comparator.comparing(InformeRevisionResponse::publicado))
                .toList();
    }

    /** @throws ResponseStatusException 404 si el informe no existe, 403 si no es del profesor */
    @Transactional(readOnly = true)
    public InformeRevisionResponse obtenerInforme(Long idProfesor, Long idInforme) {
        return InformeRevisionResponse.desde(obtenerInformeDelProfesor(idProfesor, idInforme));
    }

    /**
     * Fija la calificación final del informe (puede ser la misma que sugirió el sistema) y lo marca como
     * validado por el profesor. Se puede repetir mientras el informe no esté publicado.
     *
     * @throws IllegalArgumentException la nota está vacía, no es un número, tiene más de un decimal
     *                                  o queda fuera de 1,0 a 7,0
     * @throws IllegalStateException    el informe ya fue publicado
     * @throws ResponseStatusException  404 si el informe no existe, 403 si no es del profesor
     */
    @Transactional
    public InformeRevisionResponse guardarCalificacionFinal(Long idProfesor, Long idInforme,
                                                            String calificacionTexto) {
        InformeFinal informe = obtenerInformeDelProfesor(idProfesor, idInforme);
        exigirNoPublicado(informe);

        informe.setCalificacionFinal(parsearNota(calificacionTexto));
        informe.setFechaValidacion(LocalDateTime.now());

        informeFinalRepository.save(informe);
        return InformeRevisionResponse.desde(informe);
    }

    /**
     * Publica el informe: desde este momento el estudiante lo ve y la nota ya no se puede cambiar.
     *
     * @throws IllegalStateException   el informe ya fue publicado, o el profesor aún no fijó la nota final
     * @throws ResponseStatusException 404 si el informe no existe, 403 si no es del profesor
     */
    @Transactional
    public InformeRevisionResponse publicar(Long idProfesor, Long idInforme) {
        InformeFinal informe = obtenerInformeDelProfesor(idProfesor, idInforme);
        exigirNoPublicado(informe);

        if (informe.getCalificacionFinal() == null || informe.getFechaValidacion() == null) {
            throw new IllegalStateException(
                    "Guarda la calificación final antes de publicar el informe");
        }

        informe.setFechaPublicacion(LocalDateTime.now());

        informeFinalRepository.save(informe);
        return InformeRevisionResponse.desde(informe);
    }

    private InformeFinal obtenerInformeDelProfesor(Long idProfesor, Long idInforme) {
        InformeFinal informe = informeFinalRepository.findByIdParaProfesor(idInforme)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "El informe final no existe"));

        if (!esDelProfesor(informe, idProfesor)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No tienes permiso para revisar este informe");
        }
        return informe;
    }

    private boolean esDelProfesor(InformeFinal informe, Long idProfesor) {
        Long idProfesorSeccion = informe.getInscripcion().getSeccion().getProfesor().getIdUsuario();
        Long idCorrector = informe.getCorrector().getIdUsuario();
        return Objects.equals(idProfesorSeccion, idProfesor) || Objects.equals(idCorrector, idProfesor);
    }

    private void exigirNoPublicado(InformeFinal informe) {
        if (informe.getFechaPublicacion() != null) {
            throw new IllegalStateException(
                    "El informe ya fue publicado y su calificación no se puede modificar");
        }
    }

    /** Acepta coma o punto decimal, igual que el resto de formularios de notas del sistema. */
    private BigDecimal parsearNota(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Ingresa la calificación final");
        }

        String limpio = texto.strip().replace(',', '.');
        BigDecimal nota;
        try {
            if (limpio.length() > LARGO_MAXIMO_TEXTO) {
                throw new NumberFormatException("texto demasiado largo");
            }
            nota = new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La calificación debe ser un número, por ejemplo 5,5");
        }

        if (nota.compareTo(NOTA_MINIMA) < 0 || nota.compareTo(NOTA_MAXIMA) > 0) {
            throw new IllegalArgumentException("La calificación debe estar entre 1,0 y 7,0");
        }
        // "5,50" se acepta (el cero sobra); "5,55" se rechaza en vez de redondearse en silencio.
        if (nota.stripTrailingZeros().scale() > 1) {
            throw new IllegalArgumentException("La calificación admite solo un decimal");
        }
        return nota.setScale(1, RoundingMode.UNNECESSARY);
    }
}
