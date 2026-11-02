package com.example.lince_track

import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.ReglasCheckin
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.servicios.FakeServiciosDataSource
import com.example.lince_track.data.servicios.ServicioMapper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ServiciosTerminadosTest {

    private val base: Servicio =
        ServicioMapper.aDominio(FakeServiciosDataSource.servicios(LocalDate.of(2026, 11, 2)).first())!!
            .copy(estado = EstadoServicio.ACEPTADO)

    private fun checkin(id: String, checkOutEn: Long? = null) =
        CheckinServicio(id, checkInEn = 0L, distanciaM = 10, gpsReal = true, checkOutEn = checkOutEn)

    @Test
    fun `con check-out el servicio pasa a terminados y sale de por realizar`() {
        val servicios = listOf(base.copy(id = "1"), base.copy(id = "2"))
        val checkins = mapOf("1" to checkin("1", checkOutEn = 1_000L))

        assertEquals(listOf("1"), ReglasCheckin.terminados(servicios, checkins).map { it.id })
        assertEquals(listOf("2"), ReglasCheckin.porRealizar(servicios, checkins).map { it.id })
    }

    @Test
    fun `con solo check-in el servicio sigue por realizar`() {
        val servicios = listOf(base.copy(id = "1"))
        val checkins = mapOf("1" to checkin("1"))

        assertEquals(emptyList<Servicio>(), ReglasCheckin.terminados(servicios, checkins))
        assertEquals(listOf("1"), ReglasCheckin.porRealizar(servicios, checkins).map { it.id })
    }

    @Test
    fun `lo cancelado despues del check-out no cuenta como terminado`() {
        val servicios = listOf(base.copy(id = "1", estado = EstadoServicio.RECHAZADO))
        val checkins = mapOf("1" to checkin("1", checkOutEn = 1_000L))

        assertEquals(emptyList<Servicio>(), ReglasCheckin.terminados(servicios, checkins))
    }

    @Test
    fun `los terminados van del mas reciente al mas antiguo`() {
        val servicios = listOf(base.copy(id = "antiguo"), base.copy(id = "reciente"))
        val checkins = mapOf(
            "antiguo" to checkin("antiguo", checkOutEn = 1_000L),
            "reciente" to checkin("reciente", checkOutEn = 2_000L)
        )

        assertEquals(listOf("reciente", "antiguo"), ReglasCheckin.terminados(servicios, checkins).map { it.id })
    }

    @Test
    fun `el horario muestra el inicio y el termino reales`() {
        val checkin = CheckinServicio("1", 37_920_000L, 10, true, checkOutEn = 45_600_000L)

        assertEquals("10:32 a 12:40", ReglasCheckin.horario(checkin, ZoneOffset.UTC))
    }
}
