package cl.duoc.lince.backend.hito;

import cl.duoc.lince.backend.comun.ErrorNegocio;
import cl.duoc.lince.backend.comun.UsuarioActual;
import cl.duoc.lince.backend.config.LinceProperties;
import cl.duoc.lince.backend.geocerca.Geocerca;
import cl.duoc.lince.backend.geocerca.GeocercaRepository;
import cl.duoc.lince.backend.lince.LinceClient;
import cl.duoc.lince.backend.lince.ServicioLince;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Registra hitos con la validación del servidor: que el servicio sea de esa persona (según la API de Southbound),
 * que no esté cancelado, el orden check-in → posición → check-out, y el resultado de {@link ReglasHito}.
 */
@Service
public class HitoService {

    /** Resultado de registrar: el hito y si ya existía (la app reenvió la misma clave). */
    public record Registro(Hito hito, boolean yaExistia) {}

    private final HitoRepository hitos;
    private final GeocercaRepository geocercas;
    private final LinceClient lince;
    private final LinceProperties propiedades;
    private final Clock reloj;

    public HitoService(HitoRepository hitos, GeocercaRepository geocercas, LinceClient lince,
                       LinceProperties propiedades, Clock reloj) {
        this.hitos = hitos;
        this.geocercas = geocercas;
        this.lince = lince;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Transactional
    public Registro registrar(UsuarioActual usuario, HitoSolicitud s) {
        Optional<Hito> previo = hitos.findByClaveIdempotencia(s.claveIdempotencia());
        if (previo.isPresent()) {
            if (!previo.get().getUid().equals(usuario.uid())) {
                throw ErrorNegocio.conflicto("CLAVE_EN_USO", "La clave de idempotencia ya fue usada por otra cuenta.");
            }
            return new Registro(previo.get(), true);
        }

        ServicioLince servicio = lince.servicio(s.servicioId())
                .orElseThrow(() -> ErrorNegocio.noEncontrado("El servicio " + s.servicioId()));
        if (!servicio.esDe(usuario.transportistaId(), usuario.guiaId())) {
            throw ErrorNegocio.prohibido("El servicio no está asignado a tu cuenta.");
        }
        if (servicio.estaCancelado()) {
            throw ErrorNegocio.conflicto("SERVICIO_CANCELADO", "El servicio está cancelado o ya no es tuyo.");
        }
        revisarOrden(usuario.uid(), s);

        Hito hito = new Hito(s.claveIdempotencia(), usuario.uid(), s.servicioId(), s.tipo(),
                s.lat(), s.lon(), s.precisionMetros(), Instant.now(reloj));

        Double distancia = null;
        int radio = 0;
        if (s.tipo() != TipoHito.POSICION) {
            String lugar = s.tipo() == TipoHito.CHECK_IN ? servicio.pickup() : servicio.dropoff();
            Optional<Geocerca> geocerca = lugar == null
                    ? Optional.empty()
                    : geocercas.findByNombreIgnoreCaseAndActivaTrue(lugar.trim());
            if (geocerca.isPresent()) {
                Geocerca g = geocerca.get();
                radio = g.getRadioMetros();
                distancia = Distancia.metros(s.lat(), s.lon(), g.getLat(), g.getLon());
                hito.asignarGeocerca(g.getId(), g.getNombre(), radio, Math.round(distancia * 10) / 10.0);
            }
        }

        LinceProperties.Reglas reglas = propiedades.reglas();
        LocalDateTime ahora = LocalDateTime.now(reloj.withZone(reglas.zona()));
        LocalDateTime inicio = LocalDateTime.of(servicio.fechaLocal(), servicio.horaPickupLocal());
        hito.asignarResultado(ReglasHito.evaluar(s.tipo(), distancia, radio, s.precisionMetros(),
                reglas.precisionMaximaMetros(), ahora, inicio, reglas.anticipacionMinutos()));
        return new Registro(hitos.save(hito), false);
    }

    /** Un check-in verificado no se repite; posición y check-out piden un check-in antes; nada después del check-out. */
    private void revisarOrden(String uid, HitoSolicitud s) {
        long id = s.servicioId();
        if (hitos.existsByServicioIdAndUidAndTipoAndResultado(id, uid, TipoHito.CHECK_OUT, ResultadoHito.VERIFICADO)) {
            throw ErrorNegocio.conflicto("SERVICIO_TERMINADO", "El servicio ya tiene check-out.");
        }
        switch (s.tipo()) {
            case CHECK_IN -> {
                if (hitos.existsByServicioIdAndUidAndTipoAndResultado(id, uid, TipoHito.CHECK_IN, ResultadoHito.VERIFICADO)) {
                    throw ErrorNegocio.conflicto("HITO_DUPLICADO", "Ya tienes un check-in verificado en este servicio.");
                }
            }
            case CHECK_OUT, POSICION -> {
                if (!hitos.existsByServicioIdAndUidAndTipo(id, uid, TipoHito.CHECK_IN)) {
                    throw ErrorNegocio.conflicto("SIN_CHECK_IN", "Primero hay que hacer check-in.");
                }
            }
        }
    }

    public List<Hito> listar(UsuarioActual usuario, long servicioId) {
        return usuario.esAdministrador()
                ? hitos.findByServicioIdOrderByRegistradoEnAsc(servicioId)
                : hitos.findByServicioIdAndUidOrderByRegistradoEnAsc(servicioId, usuario.uid());
    }

    public Hito ultimaPosicion(long servicioId) {
        return hitos.findFirstByServicioIdOrderByRegistradoEnDesc(servicioId)
                .orElseThrow(() -> new ErrorNegocio(HttpStatus.NOT_FOUND, "SIN_POSICION",
                        "Nadie ha reportado posición en este servicio."));
    }
}
