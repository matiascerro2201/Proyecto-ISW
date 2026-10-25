package com.grupo1.isw.practicas_pedagogicas.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** HU-10: reglas numéricas del informe (sin base de datos): nota de 1,0 a 7,0 y promedio ponderado.
 *  La exigencia es el % de logro que equivale al 4,0 (60 por defecto, configurable en app.informe.exigencia). */
@Component
public class CalculadoraNota {
    public static final BigDecimal NOTA_MINIMA = new BigDecimal("1.0");
    public static final BigDecimal NOTA_APROBACION = new BigDecimal("4.0");
    public static final BigDecimal NOTA_MAXIMA = new BigDecimal("7.0");

    private static final BigDecimal TRES = new BigDecimal("3");
    private static final BigDecimal CIEN = new BigDecimal("100");
    /** Decimales con que se calcula antes de redondear la nota que se muestra o se guarda. */
    private static final int ESCALA_INTERNA = 6;
    /** informe_final.calificacion_sugerida es DECIMAL(3,1). */
    private static final int DECIMALES_NOTA = 1;

    /** Una nota (sin redondear) con el peso que tiene en la calificación final. */
    public record ItemNota(BigDecimal nota, BigDecimal ponderacion) {
    }

    private final BigDecimal exigenciaPorcentaje;
    /** La misma exigencia como fracción (0,60), que es como se usa en las cuentas. */
    private final BigDecimal exigencia;

    public CalculadoraNota(@Value("${app.informe.exigencia:60}") BigDecimal exigenciaPorcentaje) {
        if (exigenciaPorcentaje == null
                || exigenciaPorcentaje.signum() <= 0
                || exigenciaPorcentaje.compareTo(CIEN) >= 0) {
            throw new IllegalArgumentException("La exigencia debe ser mayor que 0 y menor que 100");
        }
        this.exigenciaPorcentaje = exigenciaPorcentaje;
        this.exigencia = exigenciaPorcentaje.divide(CIEN, ESCALA_INTERNA, RoundingMode.HALF_UP);
    }

    /** Exigencia en porcentaje (60 para un 60 %), para mostrarla en el informe. */
    public BigDecimal getExigenciaPorcentaje() {
        return exigenciaPorcentaje;
    }

    /** Fracción de logro (0 a 1) = obtenido / máximo, acotada a 1 si el dato viene fuera de rango. */
    public BigDecimal logro(BigDecimal obtenido, BigDecimal maximo) {
        if (obtenido == null || maximo == null) {
            throw new IllegalArgumentException("Faltan el puntaje obtenido o el máximo");
        }
        if (maximo.signum() <= 0) {
            throw new IllegalArgumentException("El puntaje máximo debe ser mayor que cero");
        }
        BigDecimal fraccion = obtenido.divide(maximo, ESCALA_INTERNA, RoundingMode.HALF_UP);
        return acotar(fraccion, BigDecimal.ZERO, BigDecimal.ONE);
    }

    /** Nota sin redondear: lineal de 1,0 a 4,0 hasta la exigencia y de 4,0 a 7,0 hasta el 100 %. */
    public BigDecimal notaDesdeLogro(BigDecimal logro) {
        if (logro == null) {
            throw new IllegalArgumentException("Falta el logro");
        }
        BigDecimal l = acotar(logro, BigDecimal.ZERO, BigDecimal.ONE);
        if (l.compareTo(exigencia) >= 0) {
            BigDecimal tramo = l.subtract(exigencia)
                    .divide(BigDecimal.ONE.subtract(exigencia), ESCALA_INTERNA, RoundingMode.HALF_UP);
            return NOTA_APROBACION.add(TRES.multiply(tramo));
        }
        BigDecimal tramo = l.divide(exigencia, ESCALA_INTERNA, RoundingMode.HALF_UP);
        return NOTA_MINIMA.add(TRES.multiply(tramo));
    }

    /** Promedio ponderado normalizado por la suma de pesos, redondeado a 1 decimal (1,0 a 7,0).
     *  Lanza IllegalArgumentException si no hay ítems o los pesos son negativos o suman cero. */
    public BigDecimal promedioPonderado(List<ItemNota> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("No hay notas que promediar");
        }
        BigDecimal suma = BigDecimal.ZERO;
        BigDecimal pesos = BigDecimal.ZERO;
        for (ItemNota item : items) {
            if (item.nota() == null || item.ponderacion() == null) {
                throw new IllegalArgumentException("Hay un ítem sin nota o sin ponderación");
            }
            if (item.ponderacion().signum() < 0) {
                throw new IllegalArgumentException("Las ponderaciones no pueden ser negativas");
            }
            suma = suma.add(item.nota().multiply(item.ponderacion()));
            pesos = pesos.add(item.ponderacion());
        }
        if (pesos.signum() <= 0) {
            throw new IllegalArgumentException("La suma de las ponderaciones debe ser mayor que cero");
        }
        return redondear(suma.divide(pesos, ESCALA_INTERNA, RoundingMode.HALF_UP));
    }

    /** Deja la nota entre 1,0 y 7,0 con un decimal (HALF_UP: 6,25 pasa a 6,3). */
    public BigDecimal redondear(BigDecimal nota) {
        return acotar(nota, NOTA_MINIMA, NOTA_MAXIMA).setScale(DECIMALES_NOTA, RoundingMode.HALF_UP);
    }

    private static BigDecimal acotar(BigDecimal valor, BigDecimal minimo, BigDecimal maximo) {
        if (valor.compareTo(minimo) < 0) {
            return minimo;
        }
        if (valor.compareTo(maximo) > 0) {
            return maximo;
        }
        return valor;
    }
}
