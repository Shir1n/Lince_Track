package cl.duoc.lince.backend.lince;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Lectura de la API de Southbound. Es una interfaz para poder reemplazarla en los tests. */
public interface LinceClient {

    Optional<ServicioLince> servicio(long id);

    /** Servicios de esa fecha asignados a la persona (por transportistaId o guiaId; el que no sea null). */
    List<ServicioLince> serviciosDe(LocalDate fecha, Long transportistaId, Long guiaId);
}
