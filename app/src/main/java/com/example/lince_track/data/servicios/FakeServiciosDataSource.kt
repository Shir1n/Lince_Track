package com.example.lince_track.data.servicios

import com.example.lince_track.data.remote.dto.AsignacionDto
import com.example.lince_track.data.remote.dto.ClienteRefDto
import com.example.lince_track.data.remote.dto.IdiomaDto
import com.example.lince_track.data.remote.dto.OficinaDto
import com.example.lince_track.data.remote.dto.PaxDto
import com.example.lince_track.data.remote.dto.PersonaDto
import com.example.lince_track.data.remote.dto.ReservaDto
import com.example.lince_track.data.remote.dto.ServicioDto
import com.example.lince_track.data.remote.dto.VehiculoDto
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * Datos de ejemplo con la MISMA forma que devuelve la API de Southbound.
 * Se usa mientras LINCE_API_KEY está vacía en local.properties.
 *
 * Respeta las reglas de la API: TRANSFER y EXCURSION llevan solo transportista;
 * GUIDED_SERVICE lleva solo guía. Un tour completo se relaciona por reserva.referencia.
 */
class FakeServiciosDataSource(
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ServiciosDataSource {

    override val nombre = "Datos de ejemplo"

    override suspend fun obtener(filtro: FiltroServicios): List<ServicioDto> {
        delay(300) // simula la latencia de red
        return servicios(hoy())
            .filter { LocalDate.parse(it.fecha) in filtro.desde..filtro.hasta }
            .filter { filtro.oficina == null || it.oficina?.codigo == filtro.oficina }
            .filter { filtro.guiaId == null || it.guia?.id == filtro.guiaId }
            .filter { filtro.transportistaId == null || it.transportista?.id == filtro.transportistaId }
            .sortedWith(compareBy({ it.fecha }, { it.horaPickup }))
    }

    companion object {
        // Personas de ejemplo (ids ficticios)
        val CARLOS_MORALES = PersonaDto(12, "TR-012", "Carlos Morales")
        val MIGUEL_SOTO = PersonaDto(14, "TR-014", "Miguel Soto")
        val JORGE_VALDIVIA = PersonaDto(15, "TR-015", "Jorge Valdivia")
        val PATRICIO_LAGOS = PersonaDto(16, "TR-016", "Patricio Lagos")
        val ANDREA_SOTO = PersonaDto(7, "GU-007", "Andrea Soto")
        val ROBERTO_DIAZ = PersonaDto(8, "GU-008", "Roberto Díaz")

        private val PUX = OficinaDto("PUX", "Puerto Varas")
        private val ES = IdiomaDto("ES", "Español")
        private val EN = IdiomaDto("EN", "Inglés")
        private val CLIENTE_US = ClienteRefDto(3, "CL-003", "Andes Travel Co.", "Estados Unidos")
        private val CLIENTE_CL = ClienteRefDto(5, "CL-005", "Turismo Sur Ltda.", "Chile")

        private fun pax(adultos: Int, ninos: Int) =
            PaxDto(adultos, ninos, adultos + ninos, "$adultos+$ninos")

        fun servicios(hoy: LocalDate): List<ServicioDto> = listOf(
            ServicioDto(
                id = 8890, codigo = "SRV-8890", srv = "TR", tipo = "TRANSFER",
                fecha = hoy.toString(), horaPickup = "08:30",
                descripcion = "Transfer Hotel Cumbres → Aeropuerto El Tepual",
                estadoServicio = "CF",
                reserva = ReservaDto("SB-2026-8890", "Miller x4", "CF"),
                pax = pax(3, 1),
                pickup = "Hotel Cumbres, Puerto Varas", dropoff = "Aeropuerto El Tepual",
                notas = "Vuelo LA-281 sale 11:45. Equipaje: 4 maletas grandes.",
                oficina = PUX, cliente = CLIENTE_US,
                transportista = CARLOS_MORALES,
                vehiculo = VehiculoDto(41, "LK-VF-84", "VAN", 12),
                asignacion = AsignacionDto("CF", "${hoy.minusDays(2)}T10:15:00")
            ),
            ServicioDto(
                id = 8901, codigo = "SRV-8901", srv = "GS", tipo = "GUIDED_SERVICE",
                fecha = hoy.toString(), horaPickup = "09:00",
                descripcion = "City Tour Puerto Varas",
                estadoServicio = "CF",
                reserva = ReservaDto("SB-2026-8901", "Fuentes x6", "CF"),
                pax = pax(4, 2),
                pickup = "Hotel Cumbres, Puerto Varas", dropoff = "Centro de Puerto Varas",
                notas = "Dos niños menores de 8 años.",
                oficina = PUX, cliente = CLIENTE_CL,
                idiomaRequerido = ES,
                guia = ANDREA_SOTO,
                asignacion = AsignacionDto("CF", "${hoy.minusDays(1)}T18:40:00")
            ),
            // Un mismo tour: el transporte (EXCURSION) y el guiado (GUIDED_SERVICE)
            // comparten reserva.referencia.
            ServicioDto(
                id = 8941, codigo = "SRV-8941", srv = "EX", tipo = "EXCURSION",
                fecha = hoy.toString(), horaPickup = "13:30",
                descripcion = "Excursión Saltos del Petrohué",
                estadoServicio = "CF",
                reserva = ReservaDto("SB-2026-8941", "Brown x8", "CF"),
                pax = pax(8, 0),
                pickup = "Hotel Cumbres, Puerto Varas", dropoff = "Parque Nacional Vicente Pérez Rosales",
                notas = "Llevar comprobantes CONAF. Un pasajero con movilidad reducida.",
                oficina = PUX, cliente = CLIENTE_US,
                transportista = MIGUEL_SOTO,
                vehiculo = VehiculoDto(44, "KJ-RT-21", "MINIBUS", 19),
                asignacion = AsignacionDto("RQ", "${hoy}T08:00:00")
            ),
            ServicioDto(
                id = 8942, codigo = "SRV-8942", srv = "GS", tipo = "GUIDED_SERVICE",
                fecha = hoy.toString(), horaPickup = "13:30",
                descripcion = "Guiado Saltos del Petrohué",
                descripcionPadre = "Excursión Saltos del Petrohué",
                estadoServicio = "CF",
                reserva = ReservaDto("SB-2026-8941", "Brown x8", "CF"),
                pax = pax(8, 0),
                pickup = "Hotel Cumbres, Puerto Varas", dropoff = "Parque Nacional Vicente Pérez Rosales",
                oficina = PUX, cliente = CLIENTE_US,
                idiomaRequerido = EN,
                guia = ANDREA_SOTO,
                asignacion = AsignacionDto("RQ", "${hoy}T08:05:00")
            ),
            ServicioDto(
                id = 9020, codigo = "SRV-9020", srv = "TR", tipo = "TRANSFER",
                fecha = hoy.plusDays(2).toString(), horaPickup = "15:00",
                descripcion = "Transfer Aeropuerto El Tepual → Puerto Montt",
                estadoServicio = "CF",
                reserva = ReservaDto("SB-2026-9020", "Souza x2", "CF"),
                pax = pax(2, 0),
                pickup = "Aeropuerto El Tepual", dropoff = "Puerto Montt",
                notas = "Llegan en vuelo SKY-114. Esperar en puerta 2 de arribos.",
                oficina = PUX, cliente = CLIENTE_CL,
                transportista = JORGE_VALDIVIA,
                vehiculo = VehiculoDto(45, "PH-XS-12", "SEDAN", 4),
                asignacion = AsignacionDto("RQ", "${hoy}T09:30:00")
            ),
            ServicioDto(
                id = 8950, codigo = "SRV-8950", srv = "EX", tipo = "EXCURSION",
                fecha = hoy.plusDays(5).toString(), horaPickup = "09:00",
                descripcion = "Excursión Isla de Chiloé",
                estadoServicio = "CF",
                reserva = ReservaDto("SB-2026-8950", "González x10", "CF"),
                pax = pax(6, 4),
                pickup = "Puerto Montt", dropoff = "Ancud, Chiloé",
                notas = "Almuerzo no incluido.",
                oficina = PUX, cliente = CLIENTE_CL,
                transportista = PATRICIO_LAGOS,
                vehiculo = VehiculoDto(46, "GT-PL-77", "MINIBUS", 19),
                asignacion = AsignacionDto("CF", "${hoy.minusDays(3)}T11:00:00")
            ),
            ServicioDto(
                id = 8702, codigo = "SRV-8702", srv = "TR", tipo = "TRANSFER",
                fecha = hoy.minusDays(1).toString(), horaPickup = "12:00",
                descripcion = "Transfer Frutillar → Puerto Varas",
                estadoServicio = "CF",
                reserva = ReservaDto("SB-2026-8702", "Lagos x3", "CF"),
                pax = pax(3, 0),
                pickup = "Frutillar", dropoff = "Puerto Varas",
                oficina = PUX, cliente = CLIENTE_CL,
                transportista = JORGE_VALDIVIA,
                vehiculo = VehiculoDto(45, "PH-XS-12", "SEDAN", 4),
                asignacion = AsignacionDto("DL", "${hoy.minusDays(2)}T16:20:00")
            ),
            ServicioDto(
                id = 8605, codigo = "SRV-8605", srv = "EX", tipo = "EXCURSION",
                fecha = hoy.minusDays(3).toString(), horaPickup = "08:00",
                descripcion = "Excursión Volcán Osorno",
                estadoServicio = "CF",
                reserva = ReservaDto("SB-2026-8605", "Clark x5", "CF"),
                pax = pax(5, 0),
                pickup = "Puerto Varas", dropoff = "Volcán Osorno",
                notas = "Adulto mayor en el grupo, ritmo lento.",
                oficina = PUX, cliente = CLIENTE_US,
                transportista = MIGUEL_SOTO,
                vehiculo = VehiculoDto(44, "KJ-RT-21", "MINIBUS", 19),
                asignacion = AsignacionDto("CF", "${hoy.minusDays(6)}T12:00:00")
            )
        )
    }
}
