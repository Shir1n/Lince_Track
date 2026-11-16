package cl.duoc.lince.backend.asignacion;

import cl.duoc.lince.backend.comun.UsuarioActual;
import cl.duoc.lince.backend.lince.ServicioLince;

import java.util.Optional;

/**
 * Quién tiene un servicio, igual que en la app (ReglasAsignacion): el reemplazo manda sobre la persona que
 * trae la API. Sin Spring ni Firebase, para probarla sola.
 */
public final class ReglasDueno {

    private ReglasDueno() {
    }

    public static boolean esDueno(ServicioLince servicio, Optional<Reemplazo> reemplazo, UsuarioActual usuario) {
        return reemplazo
                .map(r -> r.uid().equals(usuario.uid()))
                .orElseGet(() -> servicio.esDe(usuario.transportistaId(), usuario.guiaId()));
    }

    /**
     * Con reemplazo solo lo bloquea la cancelación del servicio (XX): el DL o CX de la API es de la persona
     * anterior, que suele ser justamente el motivo del reemplazo.
     */
    public static boolean estaCerrado(ServicioLince servicio, Optional<Reemplazo> reemplazo) {
        return reemplazo.isPresent() ? "XX".equals(servicio.estadoServicio()) : servicio.estaCancelado();
    }
}
