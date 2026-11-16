package cl.duoc.lince.backend.hito;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DistanciaTest {

    @Test
    void mismoPuntoEsCero() {
        assertThat(Distancia.metros(-41.3176, -72.9822, -41.3176, -72.9822)).isZero();
    }

    @Test
    void unaMilesimaDeGradoDeLatitudSonUnos111Metros() {
        assertThat(Distancia.metros(-41.3176, -72.9822, -41.3186, -72.9822)).isCloseTo(111.2, within(0.5));
    }

    @Test
    void puertoVarasAlAeropuertoSonUnos15Km() {
        double metros = Distancia.metros(-41.3192, -72.9858, -41.4386, -73.0939);
        assertThat(metros).isBetween(15_000.0, 17_000.0);
    }
}
