package cl.duoc.lince.backend.hito;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Arma el resumen por recurso a partir de los hitos. Sin Spring ni base de datos, para probarla sola. */
public final class ReglasResumen {

    private ReglasResumen() {
    }

    /** Una fila por persona, ordenadas de menos a más puntual (las sin servicios al final). */
    public static List<ResumenRecurso> porRecurso(List<Hito> hitos, ZoneId zona) {
        Map<String, List<Hito>> porUid = hitos.stream()
                .collect(Collectors.groupingBy(Hito::getUid, LinkedHashMap::new, Collectors.toList()));
        return porUid.values().stream()
                .map(propios -> resumir(propios, propios.getFirst().getUid(), rolDe(propios), zona))
                .sorted(Comparator.comparing(ResumenRecurso::puntualidad, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ResumenRecurso::uid))
                .toList();
    }

    /** Todas las personas juntas. */
    public static ResumenRecurso total(List<Hito> hitos, ZoneId zona) {
        return resumir(hitos, null, null, zona);
    }

    /**
     * Cada servicio de cada persona cuenta una vez, con su último check-in.
     * La clave es uid + servicio, para que el total no mezcle al conductor y al guía de un mismo tour.
     */
    static ResumenRecurso resumir(List<Hito> hitos, String uid, String rol, ZoneId zona) {
        Function<Hito, String> clave = h -> h.getUid() + "#" + h.getServicioId();
        Map<String, Hito> ultimoCheckIn = new HashMap<>();
        Set<String> conCheckOut = new HashSet<>();
        int intentos = 0;
        Instant ultimo = null;

        for (Hito h : hitos) {
            if (ultimo == null || h.getRegistradoEn().isAfter(ultimo)) ultimo = h.getRegistradoEn();
            switch (h.getTipo()) {
                case CHECK_IN -> {
                    intentos++;
                    ultimoCheckIn.merge(clave.apply(h), h,
                            (a, b) -> b.getRegistradoEn().isAfter(a.getRegistradoEn()) ? b : a);
                }
                case CHECK_OUT -> conCheckOut.add(clave.apply(h));
                case POSICION -> { }
            }
        }

        Map<ResultadoHito, Long> porResultado = ultimoCheckIn.values().stream()
                .collect(Collectors.groupingBy(Hito::getResultado, Collectors.counting()));
        int servicios = ultimoCheckIn.size();
        int aTiempo = contar(porResultado, ResultadoHito.VERIFICADO);
        long cerrados = ultimoCheckIn.keySet().stream().filter(conCheckOut::contains).count();

        return new ResumenRecurso(
                uid,
                rol,
                servicios,
                aTiempo,
                contar(porResultado, ResultadoHito.TARDE),
                contar(porResultado, ResultadoHito.FUERA_DE_ZONA),
                contar(porResultado, ResultadoHito.GPS_IMPRECISO),
                contar(porResultado, ResultadoHito.SIN_GEOCERCA),
                intentos,
                (int) cerrados,
                servicios == 0 ? null : redondear(100.0 * aTiempo / servicios),
                promedioAnticipacion(ultimoCheckIn.values(), zona),
                ultimo);
    }

    /** Minutos entre la llegada y el pickup: positivo si llegó antes. Null si ningún hito tiene hora de pickup. */
    static Double promedioAnticipacion(Iterable<Hito> checkIns, ZoneId zona) {
        double suma = 0;
        int n = 0;
        for (Hito h : checkIns) {
            LocalDateTime inicio = h.getInicioServicio();
            if (inicio == null) continue;
            LocalDateTime llegada = LocalDateTime.ofInstant(h.getRegistradoEn(), zona);
            suma += Duration.between(llegada, inicio).toSeconds() / 60.0;
            n++;
        }
        return n == 0 ? null : redondear(suma / n);
    }

    private static String rolDe(List<Hito> hitos) {
        return hitos.stream().map(Hito::getRol).filter(r -> r != null).reduce((a, b) -> b).orElse(null);
    }

    private static int contar(Map<ResultadoHito, Long> porResultado, ResultadoHito r) {
        return porResultado.getOrDefault(r, 0L).intValue();
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
