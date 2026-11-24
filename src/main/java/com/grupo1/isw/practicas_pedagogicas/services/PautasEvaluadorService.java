package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository.EvaluacionEsperada;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * N-16: "mis pautas" del profesor colaborador o del tutor, agrupadas por estudiante, con la fecha límite y el estado
 * de cada una. Las fechas nunca bloquean: una pauta atrasada se puede aplicar igual.
 */
@Service
public class PautasEvaluadorService {
    public static final String APLICADA = "APLICADA";
    public static final String PENDIENTE = "PENDIENTE";
    public static final String ATRASADA = "ATRASADA";

    public record PautaVista(EvaluacionEsperada datos, String estado, Long diasRestantes, String enlace) {
        public boolean aplicada() {
            return APLICADA.equals(estado);
        }
    }

    public record EstudianteVista(Long idInscripcion, String estudiante, String asignatura, String periodo,
                                  String tipoLinea, List<PautaVista> pautas) {
        public long pendientes() {
            return pautas.stream().filter(p -> !p.aplicada()).count();
        }
    }

    private final EvaluacionesEsperadasRepository repositorio;
    private final Clock reloj;

    @Autowired
    public PautasEvaluadorService(EvaluacionesEsperadasRepository repositorio) {
        this(repositorio, Clock.systemDefaultZone());
    }

    public PautasEvaluadorService(EvaluacionesEsperadasRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /**
     * @param evaluador "COLABORADOR" o "TUTOR": decide qué pautas se listan y a qué pantalla de evaluación se enlaza
     */
    @Transactional(readOnly = true)
    public List<EstudianteVista> listar(long idUsuario, String evaluador) {
        String rol = "TUTOR".equals(evaluador) ? "TUTOR" : "COLABORADOR";
        LocalDate hoy = LocalDate.now(reloj);
        Map<String, List<PautaVista>> porLinea = new LinkedHashMap<>();
        Map<String, EvaluacionEsperada> primera = new LinkedHashMap<>();
        for (EvaluacionEsperada e : repositorio.delEvaluador(idUsuario, rol)) {
            String clave = e.idInscripcion() + "-" + e.idLinea();
            primera.putIfAbsent(clave, e);
            porLinea.computeIfAbsent(clave, k -> new ArrayList<>()).add(vista(e, hoy, rol));
        }
        List<EstudianteVista> resultado = new ArrayList<>();
        porLinea.forEach((clave, pautas) -> {
            EvaluacionEsperada e = primera.get(clave);
            resultado.add(new EstudianteVista(e.idInscripcion(), e.estudiante(), e.asignatura(), e.periodo(),
                    e.etiquetaLinea(), pautas));
        });
        return resultado;
    }

    static PautaVista vista(EvaluacionEsperada e, LocalDate hoy, String rol) {
        String estado;
        if (e.aplicada()) {
            estado = APLICADA;
        } else if (e.fechaLimite() != null && e.fechaLimite().isBefore(hoy)) {
            estado = ATRASADA;
        } else {
            estado = PENDIENTE;
        }
        Long dias = e.fechaLimite() == null ? null : ChronoUnit.DAYS.between(hoy, e.fechaLimite());
        String base = "TUTOR".equals(rol) ? "/tutor/lineas/" : "/colaborador/lineas/";
        return new PautaVista(e, estado, dias, base + e.idLinea() + "/pautas/" + e.idPauta());
    }
}
