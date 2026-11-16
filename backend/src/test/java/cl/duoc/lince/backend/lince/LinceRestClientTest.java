package cl.duoc.lince.backend.lince;

import cl.duoc.lince.backend.comun.ErrorLince;
import cl.duoc.lince.backend.config.LinceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParamCount;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Prueba el cliente HTTP contra una API de Southbound simulada (MockRestServiceServer), sin red. */
class LinceRestClientTest {

    static final String BASE = "https://lince.test/lince-api/";

    MockRestServiceServer servidor;
    LinceRestClient cliente;

    static LinceProperties propiedades(String clave) {
        return new LinceProperties(null, new LinceProperties.Api(BASE, clave), null);
    }

    static String servicio(long id, long transportistaId) {
        return """
                {"id": %d, "tipo": "EXCURSION", "fecha": "2026-11-20", "horaPickup": "08:30",
                 "pickup": "Hotel Costanera Puerto Varas", "dropoff": "Hotel Costanera Puerto Varas",
                 "estadoServicio": "CF", "guia": null, "transportista": {"id": %d, "nombre": "X"},
                 "asignacion": {"statusPL": "CF"}}
                """.formatted(id, transportistaId);
    }

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new LinceRestClient(propiedades("clave-de-prueba"), builder);
    }

    @Test
    void pideElServicioConLaClaveEnElHeader() {
        servidor.expect(requestTo(BASE + "api/v1/servicios/3008"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-Key", "clave-de-prueba"))
                .andRespond(withSuccess("{\"data\": " + servicio(3008, 37) + "}", MediaType.APPLICATION_JSON));

        Optional<ServicioLince> s = cliente.servicio(3008);

        assertThat(s).isPresent();
        assertThat(s.get().esDe(37L, null)).isTrue();
        servidor.verify();
    }

    @Test
    void unServicioQueNoExisteDevuelveVacio() {
        servidor.expect(requestTo(BASE + "api/v1/servicios/1")).andRespond(withResourceNotFound());
        assertThat(cliente.servicio(1)).isEmpty();
    }

    @Test
    void unErrorDeLaApiSeConvierteEnErrorLince() {
        servidor.expect(requestTo(BASE + "api/v1/servicios/2")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> cliente.servicio(2))
                .isInstanceOf(ErrorLince.class)
                .hasMessageContaining("401");
    }

    @Test
    void unaCaidaDeLaApiTambienEsErrorLince() {
        servidor.expect(requestTo(BASE + "api/v1/servicios/3")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> cliente.servicio(3)).isInstanceOf(ErrorLince.class);
    }

    @Test
    void recorreTodasLasPaginasYSoloMandaElIdQueExiste() {
        String pagina1 = "{\"data\": [" + servicio(1, 37) + "], \"meta\": {\"totalPages\": 2}}";
        String pagina2 = "{\"data\": [" + servicio(2, 37) + "], \"meta\": {\"totalPages\": 2}}";
        servidor.expect(requestToUriTemplate(BASE + "api/v1/servicios?fechaDesde={f}&fechaHasta={f}&page=1&pageSize=100&transportistaId=37",
                        "2026-11-20", "2026-11-20"))
                .andExpect(queryParamCount(5))
                .andRespond(withSuccess(pagina1, MediaType.APPLICATION_JSON));
        servidor.expect(requestToUriTemplate(BASE + "api/v1/servicios?fechaDesde={f}&fechaHasta={f}&page=2&pageSize=100&transportistaId=37",
                        "2026-11-20", "2026-11-20"))
                .andRespond(withSuccess(pagina2, MediaType.APPLICATION_JSON));

        List<ServicioLince> servicios = cliente.serviciosDe(LocalDate.of(2026, 11, 20), 37L, null);

        assertThat(servicios).extracting(ServicioLince::id).containsExactly(1L, 2L);
        servidor.verify();
    }

    @Test
    void paraUnGuiaFiltraPorGuiaId() {
        servidor.expect(queryParam("guiaId", "35"))
                .andRespond(withSuccess("{\"data\": [], \"meta\": {\"totalPages\": 0}}", MediaType.APPLICATION_JSON));
        assertThat(cliente.serviciosDe(LocalDate.of(2026, 11, 20), null, 35L)).isEmpty();
        servidor.verify();
    }

    @Test
    void unErrorAlListarEsErrorLince() {
        servidor.expect(queryParam("transportistaId", "37")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(() -> cliente.serviciosDe(LocalDate.of(2026, 11, 20), 37L, null))
                .isInstanceOf(ErrorLince.class);
    }

    @Test
    void sinClaveNoLlamaALaApi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer sinLlamadas = MockRestServiceServer.bindTo(builder).build();
        LinceRestClient sinClave = new LinceRestClient(propiedades(" "), builder);

        assertThatThrownBy(() -> sinClave.servicio(1)).isInstanceOf(ErrorLince.class).hasMessageContaining("LINCE_API_KEY");
        assertThatThrownBy(() -> sinClave.serviciosDe(LocalDate.now(), 1L, null)).isInstanceOf(ErrorLince.class);
        sinLlamadas.verify();
    }
}
