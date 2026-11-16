package cl.duoc.lince.backend.hito;

import java.time.LocalDateTime;

/**
 * Decide el resultado de un hito. Sin Spring ni base de datos, para probarla sola.
 * Orden: sin geocerca → GPS impreciso → fuera de zona → tarde (solo check-in).
 */
public final class ReglasHito {

    private ReglasHito() {
    }

    /**
     * @param distanciaMetros     distancia al centro de la geocerca, o null si no hay geocerca
     * @param radioMetros         radio de la geocerca
     * @param precisionMetros     precisión que informó el GPS del teléfono
     * @param precisionMaxima     sobre este valor el GPS se considera impreciso
     * @param ahora               hora del servidor (America/Santiago)
     * @param inicio              fecha y hora de pickup del servicio
     * @param anticipacionMinutos minutos antes del pickup en que hay que estar (15)
     */
    public static ResultadoHito evaluar(
            TipoHito tipo,
            Double distanciaMetros,
            int radioMetros,
            double precisionMetros,
            int precisionMaxima,
            LocalDateTime ahora,
            LocalDateTime inicio,
            int anticipacionMinutos
    ) {
        if (tipo == TipoHito.POSICION) return ResultadoHito.REGISTRADO;
        if (distanciaMetros == null) return ResultadoHito.SIN_GEOCERCA;
        if (precisionMetros > precisionMaxima) return ResultadoHito.GPS_IMPRECISO;
        if (distanciaMetros > radioMetros) return ResultadoHito.FUERA_DE_ZONA;
        if (tipo == TipoHito.CHECK_IN && ahora.isAfter(limiteLlegada(inicio, anticipacionMinutos))) {
            return ResultadoHito.TARDE;
        }
        return ResultadoHito.VERIFICADO;
    }

    /** Última hora válida para el check-in: pickup menos la anticipación. */
    public static LocalDateTime limiteLlegada(LocalDateTime inicio, int anticipacionMinutos) {
        return inicio.minusMinutes(anticipacionMinutos);
    }
}
