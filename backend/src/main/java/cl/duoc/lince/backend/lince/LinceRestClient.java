package cl.duoc.lince.backend.lince;

import cl.duoc.lince.backend.comun.ErrorLince;
import cl.duoc.lince.backend.config.LinceProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Cliente HTTP de la API de Southbound con el header X-API-Key (la clave viene de LINCE_API_KEY). */
@Component
public class LinceRestClient implements LinceClient {

    private static final int PAGE_SIZE_MAX = 100;
    private static final int MAX_PAGINAS = 5;

    private final RestClient http;
    private final boolean tieneClave;

    @Autowired
    public LinceRestClient(LinceProperties propiedades) {
        this(propiedades, RestClient.builder());
    }

    /** Los tests pasan un builder enlazado a MockRestServiceServer para simular la API. */
    LinceRestClient(LinceProperties propiedades, RestClient.Builder builder) {
        this.tieneClave = propiedades.api().tieneClave();
        this.http = builder
                .baseUrl(propiedades.api().urlBase())
                .defaultHeader("X-API-Key", tieneClave ? propiedades.api().clave() : "")
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Respuesta<T>(T data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Meta(int totalPages) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Pagina<T>(List<T> data, Meta meta) {}

    @Override
    public Optional<ServicioLince> servicio(long id) {
        revisarClave();
        try {
            Respuesta<ServicioLince> r = http.get()
                    .uri("api/v1/servicios/{id}", id)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return Optional.ofNullable(r == null ? null : r.data());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) return Optional.empty();
            throw new ErrorLince("La API de Southbound respondió " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new ErrorLince("No se pudo consultar la API de Southbound.", e);
        }
    }

    @Override
    public List<ServicioLince> serviciosDe(LocalDate fecha, Long transportistaId, Long guiaId) {
        revisarClave();
        List<ServicioLince> resultado = new ArrayList<>();
        try {
            for (int pagina = 1; pagina <= MAX_PAGINAS; pagina++) {
                int p = pagina;
                Pagina<ServicioLince> r = http.get()
                        .uri(u -> {
                            u.path("api/v1/servicios")
                                    .queryParam("fechaDesde", fecha)
                                    .queryParam("fechaHasta", fecha)
                                    .queryParam("page", p)
                                    .queryParam("pageSize", PAGE_SIZE_MAX);
                            // La API rechaza parámetros vacíos: solo se manda el ID que existe.
                            if (transportistaId != null) u.queryParam("transportistaId", transportistaId);
                            if (guiaId != null) u.queryParam("guiaId", guiaId);
                            return u.build();
                        })
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});
                if (r == null || r.data() == null) break;
                resultado.addAll(r.data());
                if (r.meta() == null || pagina >= r.meta().totalPages()) break;
            }
            return resultado;
        } catch (RestClientException e) {
            throw new ErrorLince("No se pudo consultar la API de Southbound.", e);
        }
    }

    private void revisarClave() {
        if (!tieneClave) {
            throw new ErrorLince("Falta LINCE_API_KEY: el backend no puede consultar la API de Southbound.", null);
        }
    }
}
