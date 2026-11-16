package cl.duoc.lince.backend.bloqueo;

import cl.duoc.lince.backend.lince.ServicioLince;

import java.time.LocalDate;
import java.util.List;

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

    /** Hay un servicio vigente ese día (no se cuentan los cancelados ni los eliminados de la persona). */
    public static boolean tieneServicioAsignado(List<ServicioLince> serviciosDelDia) {
        return serviciosDelDia.stream().anyMatch(s -> !s.estaCancelado());
    }
}
