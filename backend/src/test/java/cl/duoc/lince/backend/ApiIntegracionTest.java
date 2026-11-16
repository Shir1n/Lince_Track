package cl.duoc.lince.backend;

import cl.duoc.lince.backend.asignacion.AsignacionesClient;
import cl.duoc.lince.backend.asignacion.Reemplazo;
import cl.duoc.lince.backend.comun.ErrorFirestore;
import cl.duoc.lince.backend.comun.ErrorLince;
import cl.duoc.lince.backend.lince.LinceClient;
import cl.duoc.lince.backend.lince.ServicioLince;
import cl.duoc.lince.backend.parametro.ParametrosRepository;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.doThrow;
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

    // ---------- Parámetros ----------

    @Autowired
    ParametrosRepository parametros;

    /** Vuelve a los valores de application.yml para no afectar a las demás pruebas. */
    @AfterEach
    void restaurarParametros() {
        parametros.deleteAll();
    }

    static String reglas(int anticipacion, int precision, int radio, int dias) {
        return """
                {"anticipacionMinutos": %d, "precisionMaximaMetros": %d, "radioPorDefectoMetros": %d,
                 "diasAnticipacionBloqueo": %d}
                """.formatted(anticipacion, precision, radio, dias);
    }

    @Test
    void losParametrosPartenConLosValoresDelArchivo() throws Exception {
        mvc.perform(get("/api/parametros").with(conductor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anticipacionMinutos").value(15))
                .andExpect(jsonPath("$.precisionMaximaMetros").value(50))
                .andExpect(jsonPath("$.radioPorDefectoMetros").value(150))
                .andExpect(jsonPath("$.diasAnticipacionBloqueo").value(7));
    }

    @Test
    void soloElAdministradorCambiaLosParametrosYSeValidan() throws Exception {
        mvc.perform(put("/api/parametros").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content(reglas(30, 50, 150, 7)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/parametros").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(reglas(30, 2, 150, 7)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("precisionMaximaMetros"));
        mvc.perform(put("/api/parametros").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(reglas(30, 50, 150, 7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anticipacionMinutos").value(30))
                .andExpect(jsonPath("$.actualizadoPor").value("uid-admin"));
    }

    @Test
    void subirLaAnticipacionVuelveTardeUnCheckInQueAntesEraValido() throws Exception {
        // Son las 08:00 y el pickup es a las 08:30: con 15 min vale; con 31 min ya es tarde.
        when(lince.servicio(1201)).thenReturn(Optional.of(servicio(1201, 37, "CF", "CF")));
        mvc.perform(put("/api/parametros").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content(reglas(31, 50, 150, 7)));
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1201, "CHECK_IN", LAT_HOTEL, LON_HOTEL, "clave-1201")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("TARDE"));
    }

    @Test
    void bajarLaPrecisionMaximaVuelveImprecisoUnGpsQueAntesServia() throws Exception {
        // El hito de prueba informa 8 m de precisión.
        when(lince.servicio(1202)).thenReturn(Optional.of(servicio(1202, 37, "CF", "CF")));
        mvc.perform(put("/api/parametros").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content(reglas(15, 5, 150, 7)));
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1202, "CHECK_IN", LAT_HOTEL, LON_HOTEL, "clave-1202")))
                .andExpect(jsonPath("$.resultado").value("GPS_IMPRECISO"));
    }

    @Test
    void conMenosDiasDeAnticipacionSePuedeBloquearAntes() throws Exception {
        // Hoy es 20-11: con 7 días el 25-11 se rechaza; con 3 días se acepta.
        LocalDate dia = LocalDate.of(2026, 11, 25);
        when(lince.serviciosDe(eq(dia), eq(37L), any())).thenReturn(List.of());
        mvc.perform(put("/api/parametros").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content(reglas(15, 50, 150, 3)));
        mvc.perform(post("/api/bloqueos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\": \"2026-11-25\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void unaGeocercaNuevaSinRadioUsaElRadioPorDefectoVigente() throws Exception {
        mvc.perform(put("/api/parametros").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content(reglas(15, 50, 100, 7)));
        mvc.perform(post("/api/geocercas").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Hotel Radio Nuevo\", \"lat\": -41.31, \"lon\": -72.98}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.radioMetros").value(100));
    }

    // ---------- Resumen por recurso ----------

    @Test
    void elResumenCuentaLosCheckInDeCadaPersona() throws Exception {
        // Otra cuenta (transportista 55) para no mezclarse con los hitos de las demás pruebas.
        RequestPostProcessor otro = jwt().jwt(j -> j.subject("uid-resumen").claim("rol", "conductor").claim("transportistaId", 55))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CONDUCTOR"));
        when(lince.servicio(1301)).thenReturn(Optional.of(servicio(1301, 55, "CF", "CF")));
        when(lince.servicio(1302)).thenReturn(Optional.of(servicio(1302, 55, "CF", "CF")));
        mvc.perform(post("/api/hitos").with(otro).contentType(MediaType.APPLICATION_JSON)
                .content(hito(1301, "CHECK_IN", LAT_CERCA, LON_HOTEL, "clave-1301")));
        mvc.perform(post("/api/hitos").with(otro).contentType(MediaType.APPLICATION_JSON)
                .content(hito(1302, "CHECK_IN", -41.3300, LON_HOTEL, "clave-1302")));

        mvc.perform(get("/api/hitos/resumen").param("desde", "2026-11-20").param("hasta", "2026-11-20").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-11-20"))
                .andExpect(jsonPath("$.recursos[?(@.uid == 'uid-resumen')].servicios").value(2))
                .andExpect(jsonPath("$.recursos[?(@.uid == 'uid-resumen')].aTiempo").value(1))
                .andExpect(jsonPath("$.recursos[?(@.uid == 'uid-resumen')].fueraDeZona").value(1))
                .andExpect(jsonPath("$.recursos[?(@.uid == 'uid-resumen')].puntualidad").value(50.0))
                .andExpect(jsonPath("$.recursos[?(@.uid == 'uid-resumen')].minutosAnticipacionPromedio").value(30.0))
                .andExpect(jsonPath("$.recursos[?(@.uid == 'uid-resumen')].rol").value("conductor"));
    }

    @Test
    void elResumenEsSoloParaElAdministradorYValidaElRango() throws Exception {
        mvc.perform(get("/api/hitos/resumen").with(conductor())).andExpect(status().isForbidden());
        mvc.perform(get("/api/hitos/resumen").param("desde", "2026-11-21").param("hasta", "2026-11-20").with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("RANGO_INVALIDO"));
        mvc.perform(get("/api/hitos/resumen").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-10-21"))
                .andExpect(jsonPath("$.hasta").value("2026-11-20"));
    }

    @Test
    void unRangoSinHitosDevuelveTotalEnCero() throws Exception {
        mvc.perform(get("/api/hitos/resumen").param("desde", "2026-01-01").param("hasta", "2026-01-31").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total.servicios").value(0))
                .andExpect(jsonPath("$.recursos", hasSize(0)));
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

    // ---------- Casos límite ----------

    static RequestPostProcessor sinIds() {
        return jwt().jwt(j -> j.subject("uid-sin-ids").claim("rol", "conductor"))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CONDUCTOR"));
    }

    static String idDe(String json) {
        return json.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1");
    }

    @Test
    void unaGeocercaSeLeeEditaYBorra() throws Exception {
        String id = idDe(mvc.perform(post("/api/geocercas").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Hotel Temporal\", \"lat\": -41.3, \"lon\": -72.9, \"radioMetros\": 200}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mvc.perform(get("/api/geocercas/" + id).with(conductor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.radioMetros").value(200));
        mvc.perform(put("/api/geocercas/" + id).with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \" Hotel Temporal \", \"lat\": -41.31, \"lon\": -72.91, \"radioMetros\": 100, \"activa\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Hotel Temporal"))
                .andExpect(jsonPath("$.radioMetros").value(100))
                .andExpect(jsonPath("$.activa").value(false));
        mvc.perform(delete("/api/geocercas/" + id).with(admin())).andExpect(status().isNoContent());
        mvc.perform(get("/api/geocercas/" + id).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void unaClaveDeIdempotenciaNoSePuedeUsarConOtraCuenta() throws Exception {
        when(lince.servicio(1401)).thenReturn(Optional.of(servicio(1401, 37, "CF", "CF")));
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                .content(hito(1401, "CHECK_IN", LAT_HOTEL, LON_HOTEL, "clave-1401")));
        mvc.perform(post("/api/hitos").with(reemplazo()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1401, "CHECK_IN", LAT_HOTEL, LON_HOTEL, "clave-1401")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CLAVE_EN_USO"));
    }

    @Test
    void elOrdenCheckInCheckOutSeRespeta() throws Exception {
        when(lince.servicio(1402)).thenReturn(Optional.of(servicio(1402, 37, "CF", "CF")));
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1402, "CHECK_IN", LAT_HOTEL, LON_HOTEL, "clave-1402-a")))
                .andExpect(jsonPath("$.resultado").value("VERIFICADO"));
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1402, "CHECK_IN", LAT_HOTEL, LON_HOTEL, "clave-1402-b")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("HITO_DUPLICADO"));
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1402, "CHECK_OUT", LAT_HOTEL, LON_HOTEL, "clave-1402-c")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("VERIFICADO"));
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1402, "POSICION", LAT_HOTEL, LON_HOTEL, "clave-1402-d")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SERVICIO_TERMINADO"));
        // El conductor ve solo sus hitos de ese servicio.
        mvc.perform(get("/api/hitos").param("servicioId", "1402").with(conductor()))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void unServicioQueNoExisteEnLaApiDa404() throws Exception {
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1403, "CHECK_IN", LAT_HOTEL, LON_HOTEL, "clave-1403")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/hitos/ultima-posicion").param("servicioId", "1403").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SIN_POSICION"));
    }

    @Test
    void siLaApiDeSouthboundFallaSeResponde502() throws Exception {
        when(lince.servicio(1404)).thenThrow(new ErrorLince("La API de Southbound respondió 503", null));
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1404, "CHECK_IN", LAT_HOTEL, LON_HOTEL, "clave-1404")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("LINCE_API_ERROR"));
    }

    @Test
    void siFirestoreFallaSeResponde503() throws Exception {
        when(lince.servicio(1405)).thenReturn(Optional.of(servicio(1405, 37, "CF", "CF")));
        doThrow(new ErrorFirestore("No se pudieron leer los reemplazos en Firestore.", null)).when(asignaciones).de(1405);
        mvc.perform(post("/api/hitos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content(hito(1405, "CHECK_IN", LAT_HOTEL, LON_HOTEL, "clave-1405")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("FIRESTORE_ERROR"));
    }

    @Test
    void unaCuentaSinIdDeLaApiNoPuedeBloquear() throws Exception {
        mvc.perform(post("/api/bloqueos").with(sinIds()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\": \"2026-12-10\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void nadieQuitaElBloqueoDeOtroSalvoElAdministrador() throws Exception {
        LocalDate dia = LocalDate.of(2026, 12, 12);
        when(lince.serviciosDe(eq(dia), eq(37L), any())).thenReturn(List.of());
        String id = idDe(mvc.perform(post("/api/bloqueos").with(conductor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\": \"2026-12-12\", \"motivo\": \"  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.motivo").isEmpty())
                .andReturn().getResponse().getContentAsString());

        mvc.perform(delete("/api/bloqueos/" + id).with(reemplazo())).andExpect(status().isForbidden());
        mvc.perform(get("/api/bloqueos").with(admin()))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].uid").value("uid-conductor"));
        mvc.perform(get("/api/bloqueos").with(reemplazo()))
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
        mvc.perform(delete("/api/bloqueos/" + id).with(admin())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/bloqueos/" + id).with(admin())).andExpect(status().isNotFound());
    }
}
