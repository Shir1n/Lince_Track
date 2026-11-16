package cl.duoc.lince.backend.bloqueo;

import cl.duoc.lince.backend.lince.ServicioLince;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Reglas de Southbound para bloquear días. Sin Spring, para probarlas solas. */
public final class ReglasBloqueo {

    private ReglasBloqueo() {
    }

    /** Primer día que se puede bloquear: con 7 días de anticipación, hoy no se puede bloquear antes de hoy+7. */
    public static LocalDate primerDiaBloqueable(LocalDate hoy, int diasAnticipacion) {
        return hoy.plusDays(diasAnticipacion);
    }

    public static boolean tieneAnticipacion(LocalDate fecha, LocalDate hoy, int diasAnticipacion) {
        return !fecha.isBefore(primerDiaBloqueable(hoy, diasAnticipacion));
    }

    /**
     * Hay un servicio vigente ese día: uno que la API le asigna y que no está cancelado ni se le reasignó a
     * otra persona, o uno que se le reasignó a ella.
     *
     * @param propios  servicios del día según la API
     * @param cedidos  ids de esos servicios que el administrador reasignó a otra persona
     * @param recibidos cuántos servicios de ese día se le reasignaron a esta persona
     */
    public static boolean tieneServicioAsignado(List<ServicioLince> propios, Set<Long> cedidos, int recibidos) {
        return recibidos > 0 || propios.stream().anyMatch(s -> !s.estaCancelado() && !cedidos.contains(s.id()));
    }
}
