package cl.duoc.lince.backend.bloqueo;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BloqueoRepository extends JpaRepository<Bloqueo, Long> {

    List<Bloqueo> findByUidOrderByFechaAsc(String uid);

    /** Para borrar los datos de una persona al aprobar su solicitud de privacidad. */
    @Transactional
    long deleteByUid(String uid);

    List<Bloqueo> findAllByOrderByFechaAsc();

    boolean existsByUidAndFecha(String uid, LocalDate fecha);
}
