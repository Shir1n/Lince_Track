package cl.duoc.lince.backend.hito;

import java.time.Instant;

/** Hitos ya validados para sembrar la base en pruebas de otros paquetes (el resultado solo se asigna aquí). */
public final class HitosDePrueba {

    private HitosDePrueba() {
    }

    public static Hito verificado(String clave, String uid, long servicioId, double lat, double lon, Instant registradoEn) {
        Hito hito = new Hito(clave, uid, servicioId, TipoHito.CHECK_IN, lat, lon, 8, registradoEn);
        hito.asignarResultado(ResultadoHito.VERIFICADO);
        return hito;
    }
}
