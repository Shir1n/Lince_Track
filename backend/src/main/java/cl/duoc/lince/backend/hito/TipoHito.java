package cl.duoc.lince.backend.hito;

public enum TipoHito {
    /** Llegada al pickup: se valida geocerca, precisión y hora. */
    CHECK_IN,
    /** Término en el dropoff: se valida geocerca y precisión. */
    CHECK_OUT,
    /** Posición puntual durante un servicio en curso, para que coordinación vea la última (sin rastreo continuo). */
    POSICION
}
