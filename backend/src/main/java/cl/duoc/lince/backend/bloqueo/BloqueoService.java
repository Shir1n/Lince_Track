package cl.duoc.lince.backend.bloqueo;

import cl.duoc.lince.backend.asignacion.AsignacionesClient;
import cl.duoc.lince.backend.comun.ErrorNegocio;
import cl.duoc.lince.backend.lince.ServicioLince;
import cl.duoc.lince.backend.parametro.ParametrosService;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BloqueoService {

    private final BloqueoRepository bloqueos;
    private final LinceClient lince;
    private final AsignacionesClient asignaciones;
    private final ParametrosService parametros;
    private final LinceProperties propiedades;
    private final Clock reloj;

    public BloqueoService(BloqueoRepository bloqueos, LinceClient lince, AsignacionesClient asignaciones,
                          ParametrosService parametros, LinceProperties propiedades, Clock reloj) {
        this.parametros = parametros;
        this.bloqueos = bloqueos;
        this.lince = lince;
        this.asignaciones = asignaciones;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Transactional
    public Bloqueo crear(UsuarioActual usuario, BloqueoSolicitud s) {
        // El motivo es obligatorio (la app tampoco deja bloquear sin él): validación doble.
        if (s.motivo() == null || s.motivo().isBlank()) {
            throw new ErrorNegocio(HttpStatus.BAD_REQUEST, "MOTIVO_OBLIGATORIO", "Indica el motivo del bloqueo.");
        }
        int dias = parametros.actuales().getDiasAnticipacionBloqueo();
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
        List<ServicioLince> propios = lince.serviciosDe(s.fecha(), usuario.transportistaId(), usuario.guiaId());
        Set<Long> cedidos = propios.stream()
                .map(ServicioLince::id)
                .filter(id -> asignaciones.de(id).filter(r -> !r.uid().equals(usuario.uid())).isPresent())
                .collect(Collectors.toSet());
        int recibidos = asignaciones.recibidos(usuario.uid(), s.fecha()).size();
        if (ReglasBloqueo.tieneServicioAsignado(propios, cedidos, recibidos)) {
            throw ErrorNegocio.conflicto("DIA_CON_SERVICIO", "Ese día tienes un servicio asignado.");
        }
        return bloqueos.save(new Bloqueo(usuario.uid(), s.fecha(), s.motivo().trim(), Instant.now(reloj)));
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
