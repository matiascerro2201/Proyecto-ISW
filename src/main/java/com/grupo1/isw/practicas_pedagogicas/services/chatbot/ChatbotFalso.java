package com.grupo1.isw.practicas_pedagogicas.services.chatbot;

import com.grupo1.isw.practicas_pedagogicas.models.NivelHallazgo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Bot de prueba: responde con un texto fijo, no usa internet ni clave, y no consume ninguna cuota.
 * Está activo por defecto; para usar otro proveedor se cambia chatbot.proveedor.
 *
 * <p>Sirve para desarrollar y probar todo lo demás (validaciones, guardado, pantallas, informes):
 * <ul>
 *   <li>Valora todos los aspectos como DEBIL en la primera consulta y va dando por ADECUADOS uno más
 *       por cada intercambio previo, para ver cómo se comporta un estudiante que mejora.</li>
 *   <li>Escribir {@value #FALLA_LIMITE}, {@value #FALLA_TIEMPO} o {@value #FALLA_ERROR} en el mensaje
 *       provoca la falla correspondiente, para probar cómo se muestran los errores.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "chatbot.proveedor", havingValue = "falso", matchIfMissing = true)
public class ChatbotFalso implements ChatbotClient {

    public static final String FALLA_LIMITE = "[falla-limite]";
    public static final String FALLA_TIEMPO = "[falla-tiempo]";
    public static final String FALLA_ERROR = "[falla-error]";

    @Override
    public Respuesta responder(Solicitud solicitud) {
        String mensaje = solicitud.mensaje().toLowerCase(Locale.ROOT);
        if (mensaje.contains(FALLA_LIMITE)) {
            throw new ChatbotNoDisponibleException(
                    ChatbotNoDisponibleException.Causa.LIMITE_DEL_PROVEEDOR, "Falla simulada: límite del proveedor");
        }
        if (mensaje.contains(FALLA_TIEMPO)) {
            throw new ChatbotNoDisponibleException(
                    ChatbotNoDisponibleException.Causa.TIEMPO_AGOTADO, "Falla simulada: tiempo agotado");
        }
        if (mensaje.contains(FALLA_ERROR)) {
            throw new ChatbotNoDisponibleException(
                    ChatbotNoDisponibleException.Causa.ERROR, "Falla simulada: error del proveedor");
        }

        long intercambiosPrevios = solicitud.historial().stream()
                .filter(m -> m.rol() == Rol.ESTUDIANTE)
                .count();

        List<String> categorias = solicitud.categorias();
        List<Hallazgo> hallazgos = IntStream.range(0, categorias.size())
                .mapToObj(i -> hallazgoDePrueba(categorias.get(i), i < intercambiosPrevios))
                .toList();

        String texto = "[Asistente de prueba] Respuesta simulada n.º " + (intercambiosPrevios + 1) + ". "
                + "Con el asistente real recibirías aquí consejos sobre tu planificación. "
                + "Para probar errores, escribe " + FALLA_LIMITE + ", " + FALLA_TIEMPO + " o " + FALLA_ERROR
                + " en tu mensaje.";

        return new Respuesta(texto, hallazgos);
    }

    private Hallazgo hallazgoDePrueba(String categoria, boolean adecuado) {
        return adecuado
                ? new Hallazgo(categoria, NivelHallazgo.ADECUADO, "Aspecto adecuado (valoración simulada).")
                : new Hallazgo(categoria, NivelHallazgo.DEBIL, "Aspecto por mejorar (valoración simulada).");
    }
}
