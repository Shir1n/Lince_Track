package cl.duoc.lince.backend.lince;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/** El JSON es una respuesta real de GET /api/v1/servicios/{id} (datos ficticios de la API del curso). */
class ServicioLinceJsonTest {

    static final String JSON = """
            {"data": {"id": 3008, "codigo": "SRV-003008", "srv": "EX", "tipo": "EXCURSION", "fecha": "2026-11-20",
             "horaPickup": "08:30", "descripcion": "Tour Frutillar y Lago Llanquihue, sin guía", "descripcionPadre": null,
             "estadoServicio": "CF", "reserva": {"referencia": "WHAB000000", "nombre": "Demo x2", "estado": "CF"},
             "pax": {"adultos": 2, "ninos": 0, "total": 2, "texto": "2+0"},
             "pickup": "Hotel Costanera Puerto Varas", "dropoff": "Hotel Costanera Puerto Varas", "notas": null,
             "oficina": {"codigo": "PUX", "nombre": "Puerto Varas"},
             "cliente": {"id": 1, "codigo": "CLI-001", "nombre": "Austral Explorers (Demo)", "pais": "Reino Unido"},
             "idiomaRequerido": null, "guia": null,
             "transportista": {"id": 37, "codigo": "TRA-037", "nombre": "Andrés Valdés Ibáñez"},
             "vehiculo": {"id": 44, "patente": "ZZ-1044", "tipo": "VAN", "capacidad": 8},
             "asignacion": {"statusPL": "RQ", "actualizadoEn": "2026-10-13T19:40:10"}}}
            """;

    @Test
    void leeElJsonRealIgnorandoLoQueNoUsa() {
        LinceRestClient.Respuesta<ServicioLince> r = JsonMapper.builder().build()
                .readValue(JSON, new TypeReference<LinceRestClient.Respuesta<ServicioLince>>() {});
        ServicioLince s = r.data();

        assertThat(s.id()).isEqualTo(3008);
        assertThat(s.horaPickupLocal()).isEqualTo(LocalTime.of(8, 30));
        assertThat(s.pickup()).isEqualTo("Hotel Costanera Puerto Varas");
        assertThat(s.statusPL()).isEqualTo("RQ");
        assertThat(s.esDe(37L, null)).isTrue();
        assertThat(s.esDe(null, 37L)).isFalse();
        assertThat(s.estaCancelado()).isFalse();
    }
}
