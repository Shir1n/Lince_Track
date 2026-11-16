package cl.duoc.lince.backend.asignacion;

import cl.duoc.lince.backend.comun.UsuarioActual;
import cl.duoc.lince.backend.lince.ServicioLince;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReglasDuenoTest {

    static final UsuarioActual CARLOS = new UsuarioActual("uid-carlos", "conductor", 37L, null);
    static final UsuarioActual PATRICIO = new UsuarioActual("uid-patricio", "conductor", 43L, null);
    static final Optional<Reemplazo> A_PATRICIO =
            Optional.of(new Reemplazo(3008, "uid-patricio", "conductor", "2026-11-20"));

    static ServicioLince deCarlos(String estadoServicio, String statusPL) {
        return new ServicioLince(3008, "EXCURSION", "2026-11-20", "08:30", "A", "B", estadoServicio,
                null, new ServicioLince.Persona(37, "Carlos"), new ServicioLince.Asignacion(statusPL));
    }

    @Test
    void sinReemplazoEsDeQuienTraeLaApi() {
        assertThat(ReglasDueno.esDueno(deCarlos("CF", "CF"), Optional.empty(), CARLOS)).isTrue();
        assertThat(ReglasDueno.esDueno(deCarlos("CF", "CF"), Optional.empty(), PATRICIO)).isFalse();
    }

    @Test
    void conReemplazoEsSoloDelReemplazo() {
        assertThat(ReglasDueno.esDueno(deCarlos("CF", "CF"), A_PATRICIO, PATRICIO)).isTrue();
        assertThat(ReglasDueno.esDueno(deCarlos("CF", "CF"), A_PATRICIO, CARLOS)).isFalse();
    }

    @Test
    void elDlDeLaPersonaAnteriorNoCierraElServicioDelReemplazo() {
        assertThat(ReglasDueno.estaCerrado(deCarlos("CF", "DL"), A_PATRICIO)).isFalse();
        assertThat(ReglasDueno.estaCerrado(deCarlos("CF", "DL"), Optional.empty())).isTrue();
    }

    @Test
    void unServicioCanceladoQuedaCerradoConOSinReemplazo() {
        assertThat(ReglasDueno.estaCerrado(deCarlos("XX", "CF"), A_PATRICIO)).isTrue();
        assertThat(ReglasDueno.estaCerrado(deCarlos("XX", "CF"), Optional.empty())).isTrue();
    }
}
