package cl.duoc.lince.backend;

import cl.duoc.lince.backend.asignacion.AsignacionesClient;
import cl.duoc.lince.backend.asignacion.Reemplazo;
import cl.duoc.lince.backend.lince.LinceClient;
import cl.duoc.lince.backend.lince.ServicioLince;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba los endpoints con la base H2 en memoria, la API de Southbound simulada (Mockito) y la hora fija:
 * 20-11-2026 a las 08:00 en Santiago. Los tokens se simulan con jwt() y los mismos claims que pone Firebase.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegracionTest {

    static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");

    // Hotel Costanera Puerto Varas (carga inicial) y un punto a unos 40 m.
    static final double LAT_HOTEL = -41.3176;
    static final double LON_HOTEL = -72.9822;
    static final double LAT_CERCA = -41.31795;

    @TestConfiguration
    static class HoraFija {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Clock.fixed(ZonedDateTime.of(2026, 11, 20, 8, 0, 0, 0, SANTIAGO).toInstant(), SANTIAGO);
        }
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    LinceClient lince;

    /** Sin configurar devuelve Optional.empty() y listas vacías: nadie ha sido reemplazado. */
    @MockitoBean
    AsignacionesClient asignaciones;

    static RequestPostProcessor conductor() {
        return jwt().jwt(j -> j.subject("uid-conductor").claim("rol", "conductor").claim("transportistaId", 37))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CONDUCTOR"));
    }

    static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject("uid-admin").claim("rol", "administrador"))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
    }

    static ServicioLince servicio(long id, long transportistaId, String estado, String statusPL) {
        return new ServicioLince(id, "EXCURSION", "2026-11-20", "08:30",
                "Hotel Costanera Puerto Varas", "Hotel Costanera Puerto Varas", estado,
                null, new ServicioLince.Persona(transportistaId, "Conductor"), new ServicioLince.Asignacion(statusPL));
    }

    static String hito(long servicioId, String tipo, double lat, double lon, String clave) {
        return """
                {"servicioId": %d, "tipo": "%s", "lat": %s, "lon": %s, "precisionMetros": 8, "claveIdempotencia": "%s"}
                """.formatted(servicioId, tipo, lat, lon, clave);
    }

    // ---------- Seguridad y Swagger ----------

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/api/geocercas")).andExpect(status().isUnauthorized());
    }

    @Test
    void saludYSwaggerNoPidenToken() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    // ---------- Geocercas ----------

    @Test
    void laCargaInicialTraeLosCuatroLugaresDePux() throws Exception {
        mvc.perform(get("/api/geocercas").with(conductor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void soloElAdministradorCreaGeocercas() throws Exception {
        String cuerpo = """
                {"nombre": "Hotel de Prueba", "lat": -41.3, "lon": -72.9}
                """;
        mvc.perform(post("/api/geocercas").with(conductor()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mvc.perform(post("/api/geocercas").with(admin()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.radioMetros").value(150));
        mvc.perform(post("/api/geocercas").with(admin()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("GEOCERCA_DUPLICADA"));
    }

    @Test
    void unRadioInvalidoDevuelveDetalleYRequestId() throws Exception {
        mvc.perform(put("/api/geocercas/1").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Hotel Costanera Puerto Varas", "lat": -41.3176, "lon": -72.9822, "radioMetros": 5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("radioMetros"))
                .andExpect(jsonPath("$.error.requestId").isNotEmpty());
    }

    // ---------- Hitos ----------

    @Test
    void checkInCercaDelHotelYATiempoQuedaVerificadoYNoSeDuplica() throws Exception {
        when(lince.servicio(1001)).thenReturn(Optional.of(servicio(1001, 37, "CF", "CF")));
        String cuerpo = hito(1001, "CHECK_IN", LAT_CERCA, LON_HOTEL, "clave-1001");

        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("VERIFICADO"))
                .andExpect(jsonPath("$.geocercaNombre").value("Hotel Costanera Puerto Varas"))
                .andExpect(jsonPath("$.radioMetros").value(150));

        // La app reenvía al recuperar conexión: misma clave → 200 y el mismo hito.
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claveIdempotencia").value("clave-1001"));
    }

    @Test
    void checkInLejosQuedaFueraDeZona() throws Exception {
        when(lince.servicio(1002)).thenReturn(Optional.of(servicio(1002, 37, "CF", "CF")));
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1002, "CHECK_IN", -41.3300, LON_HOTEL, "clave-1002")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("FUERA_DE_ZONA"));
    }

    @Test
    void unServicioAjenoSeRechaza() throws Exception {
        when(lince.servicio(1003)).thenReturn(Optional.of(servicio(1003, 99, "CF", "CF")));
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1003, "CHECK_IN", LAT_HOTEL, LON_HOTEL, "clave-1003")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unServicioCanceladoSeRechaza() throws Exception {
        when(lince.servicio(1004)).thenReturn(Optional.of(servicio(1004, 37, "XX", "CF")));
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1004, "CHECK_IN", LAT_HOTEL, LON_HOTEL, "clave-1004")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SERVICIO_CANCELADO"));
    }

    @Test
    void checkOutSinCheckInSeRechaza() throws Exception {
        when(lince.servicio(1005)).thenReturn(Optional.of(servicio(1005, 37, "CF", "CF")));
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1005, "CHECK_OUT", LAT_HOTEL, LON_HOTEL, "clave-1005")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SIN_CHECK_IN"));
    }

    @Test
    void elAdministradorVeLaUltimaPosicion() throws Exception {
        when(lince.servicio(1006)).thenReturn(Optional.of(servicio(1006, 37, "CF", "CF")));
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                .content(hito(1006, "CHECK_IN", LAT_HOTEL, LON_HOTEL, "clave-1006-a")));
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1006, "POSICION", -41.35, -73.0, "clave-1006-b")))
                .andExpect(jsonPath("$.resultado").value("REGISTRADO"));

        mvc.perform(get("/api/hitos/ultima-posicion").param("servicioId", "1006").with(conductor()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/hitos").param("servicioId", "1006").with(admin()))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ---------- Reemplazos (asignaciones en Firestore) ----------

    static RequestPostProcessor reemplazo() {
        return jwt().jwt(j -> j.subject("uid-reemplazo").claim("rol", "conductor").claim("transportistaId", 43))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CONDUCTOR"));
    }

    @Test
    void elReemplazoPuedeMarcarAunqueLaApiTraigaAOtraPersona() throws Exception {
        // La API dice transportista 37 con DL (lo eliminaron); el administrador lo reasignó a uid-reemplazo.
        when(lince.servicio(1101)).thenReturn(Optional.of(servicio(1101, 37, "CF", "DL")));
        when(asignaciones.de(1101)).thenReturn(Optional.of(new Reemplazo(1101, "uid-reemplazo", "conductor", "2026-11-20")));
        mvc.perform(post("/api/hitos").with(reemplazo()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1101, "CHECK_IN", LAT_HOTEL, LON_HOTEL, "clave-1101")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uid").value("uid-reemplazo"))
                .andExpect(jsonPath("$.resultado").value("VERIFICADO"));
    }

    @Test
    void quienFueReemplazadoYaNoPuedeMarcar() throws Exception {
        when(lince.servicio(1102)).thenReturn(Optional.of(servicio(1102, 37, "CF", "CF")));
        when(asignaciones.de(1102)).thenReturn(Optional.of(new Reemplazo(1102, "uid-reemplazo", "conductor", "2026-11-20")));
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1102, "CHECK_IN", LAT_HOTEL, LON_HOTEL, "clave-1102")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value("El servicio se reasignó a otra persona."));
    }

    @Test
    void unDiaConUnServicioRecibidoNoSePuedeBloquear() throws Exception {
        LocalDate dia = LocalDate.of(2026, 11, 30);
        when(lince.serviciosDe(eq(dia), eq(43L), any())).thenReturn(List.of());
        when(asignaciones.recibidos("uid-reemplazo", dia))
                .thenReturn(List.of(new Reemplazo(1103, "uid-reemplazo", "conductor", "2026-11-30")));
        mvc.perform(post("/api/bloqueos").with(reemplazo()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\": \"2026-11-30\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DIA_CON_SERVICIO"));
    }

    @Test
    void unDiaCuyoServicioSeReasignoSePuedeBloquear() throws Exception {
        LocalDate dia = LocalDate.of(2026, 12, 1);
        when(lince.serviciosDe(eq(dia), eq(37L), any())).thenReturn(List.of(servicio(1104, 37, "CF", "CF")));
        when(asignaciones.de(1104)).thenReturn(Optional.of(new Reemplazo(1104, "uid-reemplazo", "conductor", "2026-12-01")));
        mvc.perform(post("/api/bloqueos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\": \"2026-12-01\"}"))
                .andExpect(status().isCreated());
    }

    // ---------- Bloqueos ----------

    @Test
    void bloquearConMenosDeSieteDiasSeRechaza() throws Exception {
        mvc.perform(post("/api/bloqueos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\": \"2026-11-25\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("SIN_ANTICIPACION"));
    }

    @Test
    void bloquearUnDiaConServicioSeRechaza() throws Exception {
        when(lince.serviciosDe(eq(LocalDate.of(2026, 11, 28)), eq(37L), any()))
                .thenReturn(List.of(servicio(2001, 37, "CF", "RQ")));
        mvc.perform(post("/api/bloqueos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\": \"2026-11-28\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DIA_CON_SERVICIO"));
    }

    @Test
    void bloquearUnDiaLibreYDespuesQuitarlo() throws Exception {
        when(lince.serviciosDe(eq(LocalDate.of(2026, 11, 29)), eq(37L), any())).thenReturn(List.of());
        String respuesta = mvc.perform(post("/api/bloqueos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\": \"2026-11-29\", \"motivo\": \"Trámite personal\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fecha").value("2026-11-29"))
                .andReturn().getResponse().getContentAsString();
        String id = respuesta.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1");

        mvc.perform(post("/api/bloqueos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\": \"2026-11-29\"}"))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/bloqueos/" + id).with(conductor())).andExpect(status().isNoContent());
    }
}
