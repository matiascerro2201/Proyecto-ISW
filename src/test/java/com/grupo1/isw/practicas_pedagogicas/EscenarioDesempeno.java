package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository.EvaluacionEsperada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Datos de apoyo para las pruebas de pautas del semestre e informe de desempeño. */
final class EscenarioDesempeno {
    static final long SECCION = 1L;
    static final long INSCRIPCION = 10L;
    static final long ESTUDIANTE = 7L;
    static final long COLABORADOR = 21L;
    static final long TUTOR = 22L;

    private EscenarioDesempeno() {
    }

    static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    /** Pauta del colaborador o del tutor; puntaje null significa "sin aplicar". */
    static EvaluacionEsperada pauta(long idPauta, String evaluador, String ponderacion, String maximo, String puntaje,
                                    LocalDate limite, boolean publicada) {
        boolean tutor = "TUTOR".equals(evaluador);
        return new EvaluacionEsperada(INSCRIPCION, ESTUDIANTE, "Camila Soto", SECCION, "Práctica Pedagógica", 2026, 1,
                100L, "ESPECIALIDAD", idPauta, "Pauta " + idPauta, evaluador, 1, bd(ponderacion), limite, bd(maximo),
                puntaje == null ? null : 500L + idPauta, puntaje == null ? null : bd(puntaje),
                puntaje == null ? null : LocalDateTime.of(2026, 5, 10, 9, 0), publicada,
                tutor ? TUTOR : COLABORADOR, tutor ? "Tomás Tutor" : "Carla Colaboradora");
    }
}
