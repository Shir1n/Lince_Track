package cl.duoc.lince.backend.hito;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface HitoRepository extends JpaRepository<Hito, Long> {

    Optional<Hito> findByClaveIdempotencia(String clave);

    /** Para descargar los datos de una persona (Ley 21.719). */
    List<Hito> findByUidOrderByRegistradoEnAsc(String uid);

    /** Para borrar los datos de una persona al aprobar su solicitud. */
    @Transactional
    long deleteByUid(String uid);

    List<Hito> findByServicioIdOrderByRegistradoEnAsc(long servicioId);

    List<Hito> findByServicioIdAndUidOrderByRegistradoEnAsc(long servicioId, String uid);

    boolean existsByServicioIdAndUidAndTipoAndResultado(long servicioId, String uid, TipoHito tipo, ResultadoHito resultado);

    boolean existsByServicioIdAndUidAndTipo(long servicioId, String uid, TipoHito tipo);

    Optional<Hito> findFirstByServicioIdOrderByRegistradoEnDesc(long servicioId);

    /** Hitos registrados en [desde, hasta) — para el resumen por recurso. */
    List<Hito> findByRegistradoEnGreaterThanEqualAndRegistradoEnLessThanOrderByRegistradoEnAsc(Instant desde, Instant hasta);
}
