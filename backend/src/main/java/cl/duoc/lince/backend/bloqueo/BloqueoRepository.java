package cl.duoc.lince.backend.bloqueo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BloqueoRepository extends JpaRepository<Bloqueo, Long> {

    List<Bloqueo> findByUidOrderByFechaAsc(String uid);

    List<Bloqueo> findAllByOrderByFechaAsc();

    boolean existsByUidAndFecha(String uid, LocalDate fecha);
}
