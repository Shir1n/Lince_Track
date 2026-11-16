package cl.duoc.lince.backend.bloqueo;

import cl.duoc.lince.backend.comun.ErrorNegocio;
import cl.duoc.lince.backend.comun.UsuarioActual;
import cl.duoc.lince.backend.config.LinceProperties;
import cl.duoc.lince.backend.lince.LinceClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class BloqueoService {

    private final BloqueoRepository bloqueos;
    private final LinceClient lince;
    private final LinceProperties propiedades;
    private final Clock reloj;

    public BloqueoService(BloqueoRepository bloqueos, LinceClient lince, LinceProperties propiedades, Clock reloj) {
        this.bloqueos = bloqueos;
        this.lince = lince;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Transactional
    public Bloqueo crear(UsuarioActual usuario, BloqueoSolicitud s) {
        int dias = propiedades.reglas().diasAnticipacionBloqueo();
        LocalDate hoy = LocalDate.now(reloj.withZone(propiedades.reglas().zona()));
        if (!ReglasBloqueo.tieneAnticipacion(s.fecha(), hoy, dias)) {
            throw new ErrorNegocio(HttpStatus.UNPROCESSABLE_CONTENT, "SIN_ANTICIPACION",
                    "Solo se puede bloquear desde el " + ReglasBloqueo.primerDiaBloqueable(hoy, dias) + ".");
        }
        if (bloqueos.existsByUidAndFecha(usuario.uid(), s.fecha())) {
            throw ErrorNegocio.conflicto("YA_BLOQUEADO", "Ese día ya está bloqueado.");
        }
        if (usuario.transportistaId() == null && usuario.guiaId() == null) {
            throw ErrorNegocio.prohibido("Tu cuenta no tiene ID de la API: no se puede revisar tu agenda.");
        }
        if (ReglasBloqueo.tieneServicioAsignado(
                lince.serviciosDe(s.fecha(), usuario.transportistaId(), usuario.guiaId()))) {
            throw ErrorNegocio.conflicto("DIA_CON_SERVICIO", "Ese día tienes un servicio asignado.");
        }
        String motivo = s.motivo() == null || s.motivo().isBlank() ? null : s.motivo().trim();
        return bloqueos.save(new Bloqueo(usuario.uid(), s.fecha(), motivo, Instant.now(reloj)));
    }

    public List<Bloqueo> listar(UsuarioActual usuario) {
        return usuario.esAdministrador()
                ? bloqueos.findAllByOrderByFechaAsc()
                : bloqueos.findByUidOrderByFechaAsc(usuario.uid());
    }

    @Transactional
    public void eliminar(UsuarioActual usuario, long id) {
        Bloqueo b = bloqueos.findById(id).orElseThrow(() -> ErrorNegocio.noEncontrado("El bloqueo"));
        if (!usuario.esAdministrador() && !b.getUid().equals(usuario.uid())) {
            throw ErrorNegocio.prohibido("Ese bloqueo no es tuyo.");
        }
        bloqueos.delete(b);
    }
}
