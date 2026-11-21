package cl.duoc.lince.backend.hito;

import java.time.Instant;

/**
 * Cómo le fue a una persona (o al total) con sus check-in en un rango de fechas.
 * Cada servicio cuenta una vez, con el resultado de su último check-in (si reintentó, vale el último).
 *
 * @param uid                          cuenta de Firebase; null en el total
 * @param rol                          "conductor" o "guia"; null en el total
 * @param servicios                    servicios con al menos un check-in
 * @param aTiempo                      último check-in VERIFICADO
 * @param tarde                        último check-in TARDE
 * @param fueraDeZona                  último check-in FUERA_DE_ZONA
 * @param gpsImpreciso                 último check-in GPS_IMPRECISO
 * @param sinGeocerca                  último check-in SIN_GEOCERCA
 * @param intentosCheckIn              todos los check-in, contando reintentos
 * @param conCheckOut                  servicios que además tienen check-out
 * @param puntualidad                  porcentaje de servicios a tiempo (0 a 100); null si no hay servicios
 * @param minutosAnticipacionPromedio  cuántos minutos antes del pickup llegó en promedio (negativo = después)
 * @param ultimoHito                   el hito más reciente de la persona
 */
public record ResumenRecurso(
        String uid,
        String rol,
        int servicios,
        int aTiempo,
        int tarde,
        int fueraDeZona,
        int gpsImpreciso,
        int sinGeocerca,
        int intentosCheckIn,
        int conCheckOut,
        Double puntualidad,
        Double minutosAnticipacionPromedio,
        Instant ultimoHito
) {}
