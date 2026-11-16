package cl.duoc.lince.backend.parametro;

import cl.duoc.lince.backend.config.LinceProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Fuente única de las reglas vigentes (anticipación, precisión, radio por defecto, días para bloquear).
 * Hitos, bloqueos y geocercas las leen de aquí en cada solicitud, así un cambio de coordinación vale al tiro.
 */
@Service
public class ParametrosService {

    private final ParametrosRepository repositorio;
    private final LinceProperties propiedades;
    private final Clock reloj;

    public ParametrosService(ParametrosRepository repositorio, LinceProperties propiedades, Clock reloj) {
        this.repositorio = repositorio;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    /** Los vigentes; si la tabla está vacía, crea la fila con los valores de application.yml. */
    @Transactional
    public Parametros actuales() {
        return repositorio.findById(Parametros.ID_UNICO).orElseGet(() -> {
            LinceProperties.Reglas r = propiedades.reglas();
            return repositorio.save(new Parametros(r.anticipacionMinutos(), r.precisionMaximaMetros(),
                    r.radioPorDefectoMetros(), r.diasAnticipacionBloqueo()));
        });
    }

    @Transactional
    public Parametros actualizar(ParametrosSolicitud solicitud, String uid) {
        Parametros p = actuales();
        p.actualizar(solicitud, uid, Instant.now(reloj));
        return repositorio.save(p);
    }
}
