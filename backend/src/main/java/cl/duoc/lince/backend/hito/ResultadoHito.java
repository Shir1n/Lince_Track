package cl.duoc.lince.backend.hito;

public enum ResultadoHito {
    /** Dentro del radio, con GPS preciso y (en el check-in) a tiempo. */
    VERIFICADO,
    /** Más lejos que el radio de la geocerca. */
    FUERA_DE_ZONA,
    /** Check-in después del límite (pickup menos la anticipación exigida). */
    TARDE,
    /** La precisión informada por el GPS supera el máximo permitido. */
    GPS_IMPRECISO,
    /** No hay geocerca activa para el pickup/dropoff del servicio. */
    SIN_GEOCERCA,
    /** Posición puntual: no se valida. */
    REGISTRADO;

    public boolean verificado() {
        return this == VERIFICADO;
    }
}
