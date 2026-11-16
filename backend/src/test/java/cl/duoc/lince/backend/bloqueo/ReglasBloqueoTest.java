package cl.duoc.lince.backend.bloqueo;

import cl.duoc.lince.backend.lince.ServicioLince;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReglasBloqueoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 13);

    static ServicioLince servicio(String estadoServicio, String statusPL) {
        return new ServicioLince(1, "TRANSFER", "2026-11-20", "08:30", "A", "B", estadoServicio,
                null, new ServicioLince.Persona(37, "X"), new ServicioLince.Asignacion(statusPL));
    }

    @Test
    void conSieteDiasDeAnticipacionSePuede() {
        assertThat(ReglasBloqueo.tieneAnticipacion(HOY.plusDays(7), HOY, 7)).isTrue();
    }

    @Test
    void conSeisDiasNoSePuede() {
        assertThat(ReglasBloqueo.tieneAnticipacion(HOY.plusDays(6), HOY, 7)).isFalse();
    }

    @Test
    void unServicioVigenteImpideBloquear() {
        assertThat(ReglasBloqueo.tieneServicioAsignado(List.of(servicio("CF", "RQ")))).isTrue();
    }

    @Test
    void losCanceladosYEliminadosNoCuentan() {
        assertThat(ReglasBloqueo.tieneServicioAsignado(List.of(
                servicio("XX", "CF"), servicio("CF", "DL"), servicio("CF", "CX")))).isFalse();
    }

    @Test
    void sinServiciosSePuede() {
        assertThat(ReglasBloqueo.tieneServicioAsignado(List.of())).isFalse();
    }
}
