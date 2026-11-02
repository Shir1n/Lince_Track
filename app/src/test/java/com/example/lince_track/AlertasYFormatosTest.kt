package com.example.lince_track

import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.RespuestaServicio
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.formatHora
import com.example.lince_track.data.formatNombreMes
import com.example.lince_track.data.servicios.FakeServiciosDataSource
import com.example.lince_track.data.servicios.ServicioMapper
import com.example.lince_track.ui.alertas.ClaseAlerta
import com.example.lince_track.ui.alertas.GeneradorAlertas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month

/** Alertas calculadas desde los datos, formatos de hora y mes, y títulos sin flecha. */
class AlertasYFormatosTest {

    private val hoy = LocalDate.of(2026, 11, 2)
    private val ahora = LocalDateTime.of(hoy, LocalTime.of(10, 0))
    private val base: Servicio = ServicioMapper.aDominio(FakeServiciosDataSource.servicios(hoy).first())!!

    private fun servicio(
        id: String,
        estado: EstadoServicio,
        fecha: LocalDate = hoy,
        hora: Int = 8
    ) = base.copy(id = id, estado = estado, fecha = fecha, horaInicio = LocalTime.of(hora, 0))

    private fun checkin(id: String, checkOutEn: Long? = null) =
        CheckinServicio(id, 0L, distanciaM = 10, gpsReal = true, checkOutEn = checkOutEn)

    private fun clases(
        servicios: List<Servicio>,
        checkins: Map<String, CheckinServicio> = emptyMap(),
        respuestas: Map<String, RespuestaServicio> = emptyMap()
    ) = GeneradorAlertas.generar(servicios, checkins, respuestas, ahora).map { it.clase }

    @Test
    fun `aceptado de hoy sin check-in y con la hora pasada esta atrasado`() {
        assertEquals(
            listOf(ClaseAlerta.CHECKIN_ATRASADO),
            clases(listOf(servicio("1", EstadoServicio.ACEPTADO, hora = 8)))
        )
    }

    @Test
    fun `aceptado antes de su hora no genera alerta`() {
        assertTrue(clases(listOf(servicio("1", EstadoServicio.ACEPTADO, hora = 15))).isEmpty())
        assertTrue(clases(listOf(servicio("2", EstadoServicio.ACEPTADO, fecha = hoy.plusDays(1)))).isEmpty())
    }

    @Test
    fun `aceptado de un dia pasado no se marca como atrasado`() {
        assertTrue(clases(listOf(servicio("1", EstadoServicio.ACEPTADO, fecha = hoy.minusDays(1)))).isEmpty())
    }

    @Test
    fun `check-in abierto queda en ruta y el cerrado no alerta`() {
        val servicios = listOf(servicio("1", EstadoServicio.ACEPTADO))
        assertEquals(listOf(ClaseAlerta.EN_RUTA), clases(servicios, mapOf("1" to checkin("1"))))
        assertTrue(clases(servicios, mapOf("1" to checkin("1", checkOutEn = 5L))).isEmpty())
    }

    @Test
    fun `pendiente de hoy o futuro pide respuesta y el pasado no`() {
        assertEquals(listOf(ClaseAlerta.POR_RESPONDER), clases(listOf(servicio("1", EstadoServicio.PENDIENTE))))
        assertEquals(
            listOf(ClaseAlerta.POR_RESPONDER),
            clases(listOf(servicio("2", EstadoServicio.PENDIENTE, fecha = hoy.plusDays(3))))
        )
        assertTrue(clases(listOf(servicio("3", EstadoServicio.PENDIENTE, fecha = hoy.minusDays(1)))).isEmpty())
    }

    @Test
    fun `solo avisa los rechazos que vienen de la API`() {
        val servicios = listOf(servicio("1", EstadoServicio.RECHAZADO))
        assertEquals(listOf(ClaseAlerta.CANCELADO), clases(servicios))
        val propio = mapOf("1" to RespuestaServicio("1", EstadoServicio.RECHAZADO))
        assertTrue(clases(servicios, respuestas = propio).isEmpty())
        assertTrue(clases(listOf(servicio("2", EstadoServicio.RECHAZADO, fecha = hoy.minusDays(1)))).isEmpty())
    }

    @Test
    fun `los atrasos van primero`() {
        val servicios = listOf(
            servicio("pendiente", EstadoServicio.PENDIENTE, hora = 7),
            servicio("atrasado", EstadoServicio.ACEPTADO, hora = 9)
        )
        assertEquals(
            listOf(ClaseAlerta.CHECKIN_ATRASADO, ClaseAlerta.POR_RESPONDER),
            clases(servicios)
        )
    }

    @Test
    fun `alerta de hoy muestra Hoy y la hora`() {
        val alerta = GeneradorAlertas.generar(listOf(servicio("1", EstadoServicio.PENDIENTE, hora = 13)), emptyMap(), emptyMap(), ahora).single()
        assertEquals("Hoy • 13:00", alerta.tiempo)
    }

    @Test
    fun `hora en formato de 24 horas`() {
        assertEquals("08:30", formatHora(LocalTime.of(8, 30)))
        assertEquals("15:05", formatHora(LocalTime.of(15, 5)))
    }

    @Test
    fun `mes en espanol con mayuscula`() {
        assertEquals("Noviembre", formatNombreMes(Month.NOVEMBER))
    }

    @Test
    fun `los titulos no llevan flecha`() {
        val conFlecha = FakeServiciosDataSource.servicios(hoy).first { "→" in it.descripcion }
        val titulo = ServicioMapper.aDominio(conFlecha)!!.titulo
        assertFalse("→" in titulo)
        assertTrue("–" in titulo)
    }
}
