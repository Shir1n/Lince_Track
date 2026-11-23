package com.example.lince_track.data

import com.example.lince_track.data.remote.dto.LinceJson
import com.example.lince_track.data.remote.dto.ResumenHitosDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** El JSON de GET /api/hitos/resumen con la forma de ResumenHitos y ResumenRecurso del backend. */
class ResumenHitosDtoTest {

    @Test
    fun leeElResumenPorPersonaDelBackend() {
        val json = """
            {"desde":"2026-11-16","hasta":"2026-11-22",
             "total":{"uid":null,"rol":null,"servicios":3,"aTiempo":1,"tarde":1,"fueraDeZona":1,"gpsImpreciso":0,
                      "sinGeocerca":0,"intentosCheckIn":4,"conCheckOut":2,"puntualidad":33.3,
                      "minutosAnticipacionPromedio":-2.5,"ultimoHito":"2026-11-22T12:00:00Z"},
             "recursos":[
               {"uid":"uid-carlos","rol":"conductor","servicios":2,"aTiempo":0,"tarde":1,"fueraDeZona":1,"gpsImpreciso":0,
                "sinGeocerca":0,"intentosCheckIn":3,"conCheckOut":1,"puntualidad":0.0,"minutosAnticipacionPromedio":-6.0,
                "ultimoHito":"2026-11-22T12:00:00Z","campoNuevo":1},
               {"uid":"uid-alfredo","rol":"guia","servicios":1,"aTiempo":1,"tarde":0,"fueraDeZona":0,"gpsImpreciso":0,
                "sinGeocerca":0,"intentosCheckIn":1,"conCheckOut":1,"puntualidad":100.0,"minutosAnticipacionPromedio":null}
             ]}
        """.trimIndent()

        val resumen = LinceJson.decodeFromString<ResumenHitosDto>(json)

        assertEquals("2026-11-16", resumen.desde)
        assertNull(resumen.total.uid)
        assertEquals(3, resumen.total.servicios)
        assertEquals(33.3, resumen.total.puntualidad!!, 0.001)
        assertEquals(listOf("uid-carlos", "uid-alfredo"), resumen.recursos.map { it.uid })
        assertEquals(1, resumen.recursos[0].fueraDeZona)
        assertEquals(-6.0, resumen.recursos[0].minutosAnticipacionPromedio!!, 0.001)
        assertNull(resumen.recursos[1].minutosAnticipacionPromedio)
    }
}
