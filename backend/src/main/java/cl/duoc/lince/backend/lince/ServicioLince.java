package cl.duoc.lince.backend.lince;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/** Lo que el backend necesita de un servicio de la API de Southbound (el resto del JSON se ignora). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServicioLince(
        long id,
        String tipo,
        String fecha,
        String horaPickup,
        String pickup,
        String dropoff,
        String estadoServicio,
        Persona guia,
        Persona transportista,
        Asignacion asignacion
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Persona(long id, String nombre) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Asignacion(String statusPL) {}

    /** statusPL que significan que la persona ya no tiene el servicio. */
    private static final Set<String> STATUS_PL_FUERA = Set.of("DL", "CX");

    public LocalDate fechaLocal() {
        return LocalDate.parse(fecha);
    }

    public LocalTime horaPickupLocal() {
        return LocalTime.parse(horaPickup);
    }

    public String statusPL() {
        return asignacion == null ? null : asignacion.statusPL();
    }

    /** Cancelado (XX) o la asignación fue eliminada o cancelada (DL, CX). */
    public boolean estaCancelado() {
        return "XX".equals(estadoServicio) || STATUS_PL_FUERA.contains(statusPL());
    }

    /** El servicio pertenece a esta persona según la API (por su ID de transportista o de guía). */
    public boolean esDe(Long transportistaId, Long guiaId) {
        return (transportistaId != null && transportista != null && transportista.id() == transportistaId)
                || (guiaId != null && guia != null && guia.id() == guiaId);
    }
}
