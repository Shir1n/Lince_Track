package cl.duoc.lince.backend.geocerca;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GeocercaRepository extends JpaRepository<Geocerca, Long> {

    Optional<Geocerca> findByNombreIgnoreCaseAndActivaTrue(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);
}
