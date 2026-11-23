package com.example.lince_track

import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.remote.dto.ConteoPorClaveDto
import com.example.lince_track.data.remote.dto.CatalogosDto
import com.example.lince_track.data.remote.dto.TransportistaDto
import com.example.lince_track.data.remote.dto.ErrorRespuestaDto
import com.example.lince_track.data.remote.dto.LinceJson
import com.example.lince_track.data.remote.dto.PaginaDto
import com.example.lince_track.data.remote.dto.RespuestaDto
import com.example.lince_track.data.remote.dto.ResumenDto
import com.example.lince_track.data.remote.dto.ServicioDto
import com.example.lince_track.data.servicios.FakeServiciosDataSource
import com.example.lince_track.data.servicios.FiltroServicios
import com.example.lince_track.data.servicios.ServicioMapper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Tests de la integración con la API de Southbound que no necesitan red ni clave:
 * lectura del JSON, conversión al modelo de la app y datos de ejemplo.
 */
class LinceApiTest {

    // JSON con la forma documentada en la API (incluye nulls y un campo desconocido).
    private val paginaJson = """
        {
          "data": [
            {
              "id": 245,
              "codigo": "SRV-245",
              "srv": "TR",
              "tipo": "TRANSFER",
              "fecha": "2026-10-06",
              "horaPickup": "09:00",
              "descripcion": "Transfer Hotel → Aeropuerto",
              "descripcionPadre": null,
              "estadoServicio": "CF",
              "reserva": { "referencia": "R-1001", "nombre": "Smith x3", "estado": "CF" },
              "pax": { "adultos": 2, "ninos": 1, "total": 3, "texto": "2+1" },
              "pickup": "Hotel Cumbres",
              "dropoff": "Aeropuerto El Tepual",
              "notas": null,
              "oficina": { "codigo": "PUX", "nombre": "Puerto Varas" },
              "cliente": { "id": 3, "codigo": "CL-003", "nombre": "Agencia X", "pais": "Chile" },
              "idiomaRequerido": null,
              "guia": null,
              "transportista": { "id": 12, "codigo": "TR-012", "nombre": "Carlos Morales" },
              "vehiculo": { "id": 41, "patente": "AB-CD-12", "tipo": "VAN", "capacidad": 12 },
              "asignacion": { "statusPL": null, "actualizadoEn": null },
              "campoNuevoQueLaAppNoConoce": true
            }
          ],
          "meta": { "page": 1, "pageSize": 20, "total": 1, "totalPages": 1 }
        }
    """.trimIndent()

    @Test
    fun leePaginaDeServicios() {
        val pagina = LinceJson.decodeFromString<PaginaDto<ServicioDto>>(paginaJson)

        assertEquals(1, pagina.data.size)
        assertEquals(1, pagina.meta.totalPages)
        val s = pagina.data.first()
        assertEquals(245L, s.id)
        assertNull(s.guia)
        assertNull(s.asignacion.statusPL)
        assertEquals("Carlos Morales", s.transportista?.nombre)
        assertEquals("2+1", s.pax.texto)
    }

    @Test
    fun leeFormatoDeError() {
        val json = """
            {"error":{"code":"UNAUTHORIZED","message":"API key ausente o inválida.","requestId":"abc-123"}}
        """.trimIndent()
        val error = LinceJson.decodeFromString<ErrorRespuestaDto>(json).error

        assertEquals("UNAUTHORIZED", error.code)
        assertEquals("abc-123", error.requestId)
        assertTrue(error.details.isEmpty())
    }

    @Test
    fun leeElResumenDeLaApi() {
        // Recorte del JSON real de /api/v1/resumen (oficina PUX), con un campo nuevo que se debe ignorar.
        val json = """
            {"data":{"fechaDesde":"2026-11-15","fechaHasta":"2026-11-22","totalServicios":102,"totalPax":525,
             "porEstadoServicio":[{"clave":"CF","servicios":70,"pax":337},{"clave":"XX","servicios":19,"pax":157}],
             "porStatusPL":[{"clave":"CF","servicios":7,"pax":21},{"clave":"SIN_ASIGNAR","servicios":62,"pax":288}],
             "porOficina":[{"clave":"PUX","servicios":102,"pax":525}],"campoNuevo":true}}
        """.trimIndent()
        val resumen = LinceJson.decodeFromString<RespuestaDto<ResumenDto>>(json).data

        assertEquals(102, resumen.totalServicios)
        assertEquals(525, resumen.totalPax)
        assertEquals("2026-11-15", resumen.fechaDesde)
        assertEquals(ConteoPorClaveDto("SIN_ASIGNAR", 62, 288), resumen.porStatusPL.last())
        assertEquals(listOf("CF", "XX"), resumen.porEstadoServicio.map { it.clave })
        assertEquals("PUX", resumen.porOficina.single().clave)
    }

    @Test
    fun leeLosCatalogosYElEquipoDeLaApi() {
        // Recortes de las respuestas reales de /catalogos y /transportistas (oficina PUX).
        val catalogos = LinceJson.decodeFromString<RespuestaDto<CatalogosDto>>(
            """
            {"data":{"oficinas":[{"codigo":"PUX","nombre":"Puerto Varas"}],
             "idiomas":[{"codigo":"ES","nombre":"Español"},{"codigo":"EN","nombre":"Inglés"}],
             "tiposServicio":[{"tipo":"TRANSFER","srv":"TR"},{"tipo":"GUIDED_SERVICE","srv":"GS"},{"tipo":"EXCURSION","srv":"EX"}],
             "estadosServicio":[{"codigo":"CF","descripcion":"Confirmado"}],
             "statusPL":[{"codigo":"DL","descripcion":"Eliminado"}],
             "tiposVehiculo":[{"tipo":"VAN","capacidad":8}],"nuevo":[]}}
            """.trimIndent()
        ).data
        assertEquals(listOf("TRANSFER", "GUIDED_SERVICE", "EXCURSION"), catalogos.tiposServicio.map { it.tipo })
        assertEquals("EN", catalogos.idiomas[1].codigo)
        assertEquals(8, catalogos.tiposVehiculo.single().capacidad)

        val pagina = LinceJson.decodeFromString<PaginaDto<TransportistaDto>>(
            """
            {"data":[{"id":37,"codigo":"TRA-037","nombre":"Andrés Valdés Ibáñez","oficina":{"codigo":"PUX","nombre":"Puerto Varas"},
              "vehiculos":[{"id":44,"patente":"ZZ-1044","tipo":"VAN","capacidad":8}],"evaluacionPromedio":4.99,
              "fechaIngreso":"2021-01-18","activo":true}],
             "meta":{"page":1,"pageSize":100,"total":20,"totalPages":1}}
            """.trimIndent()
        )
        assertEquals("ZZ-1044", pagina.data.single().vehiculos.single().patente)
        assertEquals(20, pagina.meta.total)
    }

    @Test
    fun convierteServicioAlModeloDeLaApp() {
        val dto = LinceJson.decodeFromString<PaginaDto<ServicioDto>>(paginaJson).data.first()
        val servicio = ServicioMapper.aDominio(dto)!!

        assertEquals("245", servicio.id)
        assertEquals(LocalDate.of(2026, 10, 6), servicio.fecha)
        assertEquals(LocalTime.of(9, 0), servicio.horaInicio)
        assertEquals(LocalTime.of(10, 0), servicio.horaFin) // TRANSFER: 60 min estimados
        assertTrue(servicio.horaFinEstimada)
        assertEquals(3, servicio.pasajeros)
        assertEquals(ServicioMapper.SIN_GUIA, servicio.guia)
        assertEquals("Carlos Morales", servicio.conductor)
        assertEquals("#R-1001", servicio.reserva)
        assertEquals(EstadoServicio.PENDIENTE, servicio.estado) // statusPL null = sin staff
    }

    @Test
    fun mapeaStatusPLAlEstadoDeLaApp() {
        assertEquals(EstadoServicio.ACEPTADO, ServicioMapper.estadoDesde("CF"))
        assertEquals(EstadoServicio.PENDIENTE, ServicioMapper.estadoDesde("RQ"))
        assertEquals(EstadoServicio.PENDIENTE, ServicioMapper.estadoDesde(null))
        assertEquals(EstadoServicio.RECHAZADO, ServicioMapper.estadoDesde("DL"))
        assertEquals(EstadoServicio.RECHAZADO, ServicioMapper.estadoDesde("CX"))
        assertEquals(EstadoServicio.RECHAZADO, ServicioMapper.estadoDesde("CF", estadoServicio = "XX"))
    }

    @Test
    fun descartaServicioConFechaInvalida() {
        val dto = ServicioDto(id = 1, fecha = "06-10-2026")
        assertNull(ServicioMapper.aDominio(dto))
    }

    @Test
    fun datosDeEjemploRespetanLasReglasDeLaApi() = runBlocking {
        val hoy = LocalDate.of(2026, 10, 28)
        val fuente = FakeServiciosDataSource(hoy = { hoy })
        val todos = fuente.obtener(FiltroServicios(hoy.minusDays(30), hoy.plusDays(30)))

        assertTrue(todos.isNotEmpty())
        todos.forEach { s ->
            when (s.tipo) {
                "GUIDED_SERVICE" -> assertTrue("${s.id} no debe tener transportista", s.transportista == null)
                "TRANSFER", "EXCURSION" -> assertTrue("${s.id} no debe tener guía", s.guia == null)
            }
        }

        val deCarlos = fuente.obtener(
            FiltroServicios(hoy, hoy, transportistaId = FakeServiciosDataSource.CARLOS_MORALES.id)
        )
        assertTrue(deCarlos.all { it.transportista?.id == 12L && it.fecha == hoy.toString() })

        // Carlos debe tener un servicio aceptado y uno pendiente para mostrar todo el flujo.
        val estados = deCarlos.map { it.asignacion.statusPL }
        assertTrue("CF" in estados)
        assertTrue("RQ" in estados)
    }
}
