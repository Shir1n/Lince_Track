package cl.duoc.lince.backend.privacidad;

import cl.duoc.lince.backend.bloqueo.BloqueoRepository;
import cl.duoc.lince.backend.hito.HitoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrivacidadService {

    private static final Logger log = LoggerFactory.getLogger(PrivacidadService.class);

    private final HitoRepository hitos;
    private final BloqueoRepository bloqueos;

    public PrivacidadService(HitoRepository hitos, BloqueoRepository bloqueos) {
        this.hitos = hitos;
        this.bloqueos = bloqueos;
    }

    public DatosPersona datosDe(String uid) {
        return new DatosPersona(uid, hitos.findByUidOrderByRegistradoEnAsc(uid), bloqueos.findByUidOrderByFechaAsc(uid));
    }

    @Transactional
    public DatosBorrados borrar(String uid) {
        long cantidadHitos = hitos.deleteByUid(uid);
        long cantidadBloqueos = bloqueos.deleteByUid(uid);
        // Sin coordenadas ni nombres en el log: solo cuántos registros se borraron.
        log.info("Datos borrados por solicitud de privacidad: {} hitos y {} bloqueos", cantidadHitos, cantidadBloqueos);
        return new DatosBorrados(uid, cantidadHitos, cantidadBloqueos);
    }
}
