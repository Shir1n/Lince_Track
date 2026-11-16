package cl.duoc.lince.backend.asignacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Lectura de los reemplazos guardados en Firestore. Es una interfaz para reemplazarla en los tests. */
public interface AsignacionesClient {

    /** Reemplazo del servicio, si el administrador lo reasignó. */
    Optional<Reemplazo> de(long servicioId);

    /** Servicios de esa fecha que se le reasignaron a esta cuenta. */
    List<Reemplazo> recibidos(String uid, LocalDate fecha);

    /** false si el backend corre sin credenciales de Firebase y no puede ver los reemplazos. */
    boolean activo();
}
