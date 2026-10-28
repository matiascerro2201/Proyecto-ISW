package com.grupo1.isw.practicas_pedagogicas.services.informe;

import java.util.List;

/**
 * Las cifras del informe de una sección, ya calculadas.
 *
 * @param totalEstudiantes          estudiantes inscritos en la sección
 * @param estudiantesConAsistente   estudiantes que le hicieron al menos una consulta
 * @param planificacionesRevisadas  planificaciones con al menos una consulta (borradores y enviadas)
 * @param consultas                 consultas al asistente en total
 * @param planificacionesEnviadas   planificaciones que ya se enviaron (no borradores)
 * @param enviadasConAsistente      de las enviadas, las que pasaron por el asistente antes de enviarse
 * @param planificacionesValoradas  planificaciones con al menos una valoración del asistente: es la base
 *                                  de los porcentajes por aspecto
 * @param aspectos                  los aspectos, de más a menos débil en la primera revisión
 * @param aprobacion                aprobación en la primera evaluación, con y sin consulta previa
 */
public record ResumenInforme(int totalEstudiantes,
                             int estudiantesConAsistente,
                             int planificacionesRevisadas,
                             int consultas,
                             int planificacionesEnviadas,
                             int enviadasConAsistente,
                             int planificacionesValoradas,
                             List<Aspecto> aspectos,
                             Aprobacion aprobacion) {

    public ResumenInforme {
        aspectos = List.copyOf(aspectos);
    }

    /** Un aspecto tal como lo muestra la pantalla: porcentajes y cuánto cambió entre la primera y la última revisión. */
    public record AspectoVista(String nombre, int pctPrimera, String detallePrimera,
                               int pctUltima, String detalleUltima, String cambio) {
    }

    public int pctEstudiantesConAsistente() {
        return porcentaje(estudiantesConAsistente, totalEstudiantes);
    }

    public int pctEnviadasConAsistente() {
        return porcentaje(enviadasConAsistente, planificacionesEnviadas);
    }

    /** Consultas por planificación revisada, con una decimal y coma ("3,1"). */
    public String consultasPorPlanificacion() {
        if (planificacionesRevisadas == 0) {
            return "0";
        }
        long decimas = Math.round(10.0 * consultas / planificacionesRevisadas);
        return (decimas / 10) + "," + (decimas % 10);
    }

    public List<AspectoVista> aspectosVista() {
        return aspectos.stream().map(a -> {
            int primera = porcentaje(a.debilesPrimera(), planificacionesValoradas);
            int ultima = porcentaje(a.debilesUltima(), planificacionesValoradas);
            return new AspectoVista(a.nombre(),
                    primera, a.debilesPrimera() + " de " + planificacionesValoradas,
                    ultima, a.debilesUltima() + " de " + planificacionesValoradas,
                    cambio(ultima - primera));
        }).toList();
    }

    private static String cambio(int puntos) {
        if (puntos == 0) {
            return "0 pts";
        }
        return (puntos < 0 ? "\u2212" + (-puntos) : "+" + puntos) + " pts";
    }

    /**
     * Un aspecto de la planificación.
     *
     * @param debilesPrimera planificaciones en que la primera valoración lo marcó como débil
     * @param debilesUltima  planificaciones en que la última valoración lo marcó como débil
     * @param ejemplos       hasta tres observaciones del asistente sobre este aspecto (primera revisión)
     */
    public record Aspecto(String codigo, String nombre, int debilesPrimera, int debilesUltima,
                          List<String> ejemplos) {
        public Aspecto {
            ejemplos = List.copyOf(ejemplos);
        }
    }

    /** Planificaciones evaluadas, según si el estudiante consultó al asistente antes de su primera evaluación. */
    public record Aprobacion(int evaluadasConAsistente, int aprobadasConAsistente,
                             int evaluadasSinAsistente, int aprobadasSinAsistente) {

        public int pctConAsistente() {
            return porcentaje(aprobadasConAsistente, evaluadasConAsistente);
        }

        public int pctSinAsistente() {
            return porcentaje(aprobadasSinAsistente, evaluadasSinAsistente);
        }
    }

    /** Porcentaje entero redondeado; 0 si no hay base. */
    public static int porcentaje(int parte, int total) {
        return total == 0 ? 0 : (int) Math.round(100.0 * parte / total);
    }
}
