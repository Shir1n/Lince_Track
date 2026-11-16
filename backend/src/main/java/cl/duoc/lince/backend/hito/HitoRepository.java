package cl.duoc.lince.backend.hito;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HitoRepository extends JpaRepository<Hito, Long> {

    Optional<Hito> findByClaveIdempotencia(String clave);

    List<Hito> findByServicioIdOrderByRegistradoEnAsc(long servicioId);

    List<Hito> findByServicioIdAndUidOrderByRegistradoEnAsc(long servicioId, String uid);

    boolean existsByServicioIdAndUidAndTipoAndResultado(long servicioId, String uid, TipoHito tipo, ResultadoHito resultado);

    boolean existsByServicioIdAndUidAndTipo(long servicioId, String uid, TipoHito tipo);

    Optional<Hito> findFirstByServicioIdOrderByRegistradoEnDesc(long servicioId);
}
