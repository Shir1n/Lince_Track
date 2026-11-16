package cl.duoc.lince.backend.asignacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Se usa cuando el backend corre sin la cuenta de servicio de Firebase (por ejemplo, en el computador de
 * alguien sin acceso a Firebase o en los tests). Funciona como si nadie hubiera sido reemplazado.
 */
public class SinAsignaciones implements AsignacionesClient {

    @Override
    public Optional<Reemplazo> de(long servicioId) {
        return Optional.empty();
    }

    @Override
    public List<Reemplazo> recibidos(String uid, LocalDate fecha) {
        return List.of();
    }

    @Override
    public boolean activo() {
        return false;
    }
}
