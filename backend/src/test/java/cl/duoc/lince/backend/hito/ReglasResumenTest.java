package cl.duoc.lince.backend.hito;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReglasResumenTest {

    static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");
    static final LocalDateTime PICKUP = LocalDateTime.of(2026, 11, 20, 8, 30);
    static int secuencia = 0;

    /** Hito registrado a la hora local indicada, con el pickup de las 08:30. */
    static Hito hito(String uid, long servicio, TipoHito tipo, ResultadoHito resultado, LocalDateTime hora) {
        Hito h = new Hito("c" + (++secuencia), uid, servicio, tipo, 0, 0, 5, hora.atZone(SANTIAGO).toInstant());
        h.asignarResultado(resultado);
        h.asignarContexto(uid.startsWith("g") ? "guia" : "conductor", PICKUP);
        return h;
    }

    @Test
    void cadaServicioCuentaUnaVezConSuUltimoCheckIn() {
        // Primero fuera de zona, luego se acercó y quedó verificado: cuenta como a tiempo.
        List<Hito> hitos = List.of(
                hito("c1", 1, TipoHito.CHECK_IN, ResultadoHito.FUERA_DE_ZONA, PICKUP.minusMinutes(25)),
                hito("c1", 1, TipoHito.CHECK_IN, ResultadoHito.VERIFICADO, PICKUP.minusMinutes(20)));
        ResumenRecurso r = ReglasResumen.porRecurso(hitos, SANTIAGO).getFirst();

        assertThat(r.servicios()).isEqualTo(1);
        assertThat(r.aTiempo()).isEqualTo(1);
        assertThat(r.fueraDeZona()).isZero();
        assertThat(r.intentosCheckIn()).isEqualTo(2);
        assertThat(r.puntualidad()).isEqualTo(100.0);
        assertThat(r.minutosAnticipacionPromedio()).isEqualTo(20.0);
    }

    @Test
    void separaPorPersonaYPoneAlMenosPuntualPrimero() {
        List<Hito> hitos = List.of(
                hito("c1", 1, TipoHito.CHECK_IN, ResultadoHito.VERIFICADO, PICKUP.minusMinutes(20)),
                hito("c2", 2, TipoHito.CHECK_IN, ResultadoHito.TARDE, PICKUP.minusMinutes(5)),
                hito("c2", 3, TipoHito.CHECK_IN, ResultadoHito.VERIFICADO, PICKUP.minusMinutes(30)));
        List<ResumenRecurso> recursos = ReglasResumen.porRecurso(hitos, SANTIAGO);

        assertThat(recursos).extracting(ResumenRecurso::uid).containsExactly("c2", "c1");
        ResumenRecurso c2 = recursos.getFirst();
        assertThat(c2.servicios()).isEqualTo(2);
        assertThat(c2.tarde()).isEqualTo(1);
        assertThat(c2.puntualidad()).isEqualTo(50.0);
        assertThat(c2.minutosAnticipacionPromedio()).isEqualTo(17.5);
        assertThat(c2.rol()).isEqualTo("conductor");
    }

    @Test
    void llegarDespuesDelPickupDaAnticipacionNegativa() {
        List<Hito> hitos = List.of(hito("c1", 1, TipoHito.CHECK_IN, ResultadoHito.TARDE, PICKUP.plusMinutes(10)));
        assertThat(ReglasResumen.porRecurso(hitos, SANTIAGO).getFirst().minutosAnticipacionPromedio()).isEqualTo(-10.0);
    }

    @Test
    void elTotalNoMezclaAlConductorYAlGuiaDeUnMismoTour() {
        List<Hito> hitos = List.of(
                hito("c1", 3008, TipoHito.CHECK_IN, ResultadoHito.VERIFICADO, PICKUP.minusMinutes(20)),
                hito("g1", 3008, TipoHito.CHECK_IN, ResultadoHito.GPS_IMPRECISO, PICKUP.minusMinutes(20)),
                hito("c1", 3008, TipoHito.CHECK_OUT, ResultadoHito.VERIFICADO, PICKUP.plusHours(3)));
        ResumenRecurso total = ReglasResumen.total(hitos, SANTIAGO);

        assertThat(total.uid()).isNull();
        assertThat(total.servicios()).isEqualTo(2);
        assertThat(total.aTiempo()).isEqualTo(1);
        assertThat(total.gpsImpreciso()).isEqualTo(1);
        assertThat(total.conCheckOut()).isEqualTo(1);
    }

    @Test
    void quienSoloReportaPosicionQuedaAlFinalSinPuntualidad() {
        List<Hito> hitos = List.of(
                hito("c9", 7, TipoHito.POSICION, ResultadoHito.REGISTRADO, PICKUP),
                hito("c1", 1, TipoHito.CHECK_IN, ResultadoHito.TARDE, PICKUP));
        List<ResumenRecurso> recursos = ReglasResumen.porRecurso(hitos, SANTIAGO);

        assertThat(recursos).extracting(ResumenRecurso::uid).containsExactly("c1", "c9");
        assertThat(recursos.getLast().puntualidad()).isNull();
        assertThat(recursos.getLast().minutosAnticipacionPromedio()).isNull();
    }

    @Test
    void sinHitosElTotalQuedaEnCero() {
        ResumenRecurso total = ReglasResumen.total(List.of(), SANTIAGO);
        assertThat(total.servicios()).isZero();
        assertThat(total.puntualidad()).isNull();
        assertThat(total.ultimoHito()).isNull();
    }
}
