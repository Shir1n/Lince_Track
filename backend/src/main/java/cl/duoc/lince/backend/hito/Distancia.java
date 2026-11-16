package cl.duoc.lince.backend.hito;

/** Distancia entre dos coordenadas con la fórmula de Haversine (la misma idea que usa la app). */
public final class Distancia {

    private static final double RADIO_TIERRA_METROS = 6_371_000;

    private Distancia() {
    }

    public static double metros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RADIO_TIERRA_METROS * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
