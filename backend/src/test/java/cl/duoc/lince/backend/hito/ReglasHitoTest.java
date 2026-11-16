package cl.duoc.lince.backend.hito;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReglasHitoTest {

    private static final LocalDateTime PICKUP = LocalDateTime.of(2026, 11, 20, 8, 30);
    private static final int RADIO = 150;
    private static final int PRECISION_MAX = 50;
    private static final int ANTICIPACION = 15;

    private ResultadoHito checkIn(Double distancia, double precision, LocalDateTime ahora) {
        return ReglasHito.evaluar(TipoHito.CHECK_IN, distancia, RADIO, precision, PRECISION_MAX, ahora, PICKUP, ANTICIPACION);
    }

    @Test
    void dentroDelRadioYConTiempoQuedaVerificado() {
        assertThat(checkIn(80.0, 10, PICKUP.minusMinutes(20))).isEqualTo(ResultadoHito.VERIFICADO);
    }

    @Test
    void justoEnElLimiteDeQuinceMinutosVale() {
        assertThat(checkIn(80.0, 10, PICKUP.minusMinutes(15))).isEqualTo(ResultadoHito.VERIFICADO);
    }

    @Test
    void unMinutoDespuesDelLimiteEsTarde() {
        assertThat(checkIn(80.0, 10, PICKUP.minusMinutes(14))).isEqualTo(ResultadoHito.TARDE);
    }

    @Test
    void justoEnElBordeDelRadioVale() {
        assertThat(checkIn(150.0, 10, PICKUP.minusMinutes(30))).isEqualTo(ResultadoHito.VERIFICADO);
    }

    @Test
    void fueraDelRadioEsFueraDeZonaAunqueLlegueATiempo() {
        assertThat(checkIn(150.1, 10, PICKUP.minusMinutes(30))).isEqualTo(ResultadoHito.FUERA_DE_ZONA);
    }

    @Test
    void gpsImprecisoGanaAFueraDeZona() {
        assertThat(checkIn(900.0, 51, PICKUP.minusMinutes(30))).isEqualTo(ResultadoHito.GPS_IMPRECISO);
    }

    @Test
    void sinGeocercaNoSePuedeVerificar() {
        assertThat(checkIn(null, 5, PICKUP.minusMinutes(30))).isEqualTo(ResultadoHito.SIN_GEOCERCA);
    }

    @Test
    void elCheckOutNoRevisaLaHora() {
        ResultadoHito r = ReglasHito.evaluar(TipoHito.CHECK_OUT, 40.0, RADIO, 10, PRECISION_MAX,
                PICKUP.plusHours(4), PICKUP, ANTICIPACION);
        assertThat(r).isEqualTo(ResultadoHito.VERIFICADO);
    }

    @Test
    void laPosicionSoloSeRegistra() {
        ResultadoHito r = ReglasHito.evaluar(TipoHito.POSICION, null, 0, 500, PRECISION_MAX,
                PICKUP, PICKUP, ANTICIPACION);
        assertThat(r).isEqualTo(ResultadoHito.REGISTRADO);
    }

    @Test
    void elRadioEsParametroDeLaRegla() {
        ResultadoHito r = ReglasHito.evaluar(TipoHito.CHECK_IN, 120.0, 100, 10, PRECISION_MAX,
                PICKUP.minusMinutes(30), PICKUP, ANTICIPACION);
        assertThat(r).isEqualTo(ResultadoHito.FUERA_DE_ZONA);
    }
}
