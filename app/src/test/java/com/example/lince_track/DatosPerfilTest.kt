package com.example.lince_track

import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.Rol
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.VehiculoServicio
import com.example.lince_track.data.servicios.FakeServiciosDataSource
import com.example.lince_track.data.servicios.ServicioMapper
import com.example.lince_track.ui.perfil.ActividadPerfil
import com.example.lince_track.ui.perfil.DatosPerfil
import com.example.lince_track.ui.perfil.TotalesPerfil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Vehículo, totales y cargo del perfil con datos reales. */
class DatosPerfilTest {

    private val hoy = LocalDate.of(2026, 11, 1)
    private val base: Servicio = ServicioMapper.aDominio(FakeServiciosDataSource.servicios(hoy).first())!!

    private fun vehiculo(patente: String) = VehiculoServicio(patente, "VAN", 12)

    private fun servicio(
        id: String,
        fecha: LocalDate = hoy,
        hora: Int = 8,
        estado: EstadoServicio = EstadoServicio.ACEPTADO,
        patente: String? = null
    ) = base.copy(
        id = id,
        fecha = fecha,
        horaInicio = LocalTime.of(hora, 0),
        estado = estado,
        vehiculo = patente?.let { vehiculo(it) }
    )

    private fun checkin(id: String, checkOutEn: Long? = null) =
        CheckinServicio(id, 0L, distanciaM = 10, gpsReal = true, checkOutEn = checkOutEn)

    @Test
    fun `el mapper entrega el vehiculo de la API`() {
        assertEquals(VehiculoServicio("LK-VF-84", "VAN", 12), base.vehiculo)
    }

    @Test
    fun `usa el vehiculo del proximo servicio`() {
        val servicios = listOf(
            servicio("pasado", fecha = hoy.minusDays(1), patente = "PASADO"),
            servicio("lejano", fecha = hoy.plusDays(5), patente = "LEJANO"),
            servicio("tarde", hora = 15, patente = "TARDE"),
            servicio("temprano", hora = 9, patente = "TEMPRANO")
        )
        assertEquals("TEMPRANO", DatosPerfil.vehiculo(servicios, hoy)?.patente)
    }

    @Test
    fun `sin proximos usa el mas reciente`() {
        val servicios = listOf(
            servicio("antiguo", fecha = hoy.minusDays(6), patente = "ANTIGUO"),
            servicio("reciente", fecha = hoy.minusDays(1), patente = "RECIENTE")
        )
        assertEquals("RECIENTE", DatosPerfil.vehiculo(servicios, hoy)?.patente)
    }

    @Test
    fun `los rechazados no cuentan para el vehiculo`() {
        val servicios = listOf(
            servicio("rechazado", estado = EstadoServicio.RECHAZADO, patente = "RECHAZADO"),
            servicio("lejano", fecha = hoy.plusDays(5), patente = "LEJANO")
        )
        assertEquals("LEJANO", DatosPerfil.vehiculo(servicios, hoy)?.patente)
    }

    @Test
    fun `sin servicios no hay vehiculo`() {
        assertNull(DatosPerfil.vehiculo(emptyList(), hoy))
        assertNull(DatosPerfil.vehiculo(listOf(servicio("sin vehiculo")), hoy))
    }

    @Test
    fun `totales cuentan servicios aceptados y finalizados`() {
        val servicios = listOf(
            servicio("1"),
            servicio("2"),
            servicio("3", estado = EstadoServicio.PENDIENTE)
        )
        val checkins = mapOf("1" to checkin("1", checkOutEn = 5L), "2" to checkin("2"))

        assertEquals(TotalesPerfil(servicios = 3, aceptados = 2, finalizados = 1), DatosPerfil.totales(servicios, checkins))
    }

    @Test
    fun `sin datos los totales quedan en cero`() {
        assertEquals(TotalesPerfil(0, 0, 0), DatosPerfil.totales(emptyList(), emptyMap()))
    }

    @Test
    fun `en ruta solo con un check-in abierto`() {
        val servicios = listOf(servicio("1"))
        assertFalse(DatosPerfil.enRuta(servicios, emptyMap()))
        assertTrue(DatosPerfil.enRuta(servicios, mapOf("1" to checkin("1"))))
        assertFalse(DatosPerfil.enRuta(servicios, mapOf("1" to checkin("1", checkOutEn = 5L))))
    }

    @Test
    fun `proximo servicio salta lo pasado rechazado y terminado`() {
        val servicios = listOf(
            servicio("ayer", fecha = hoy.minusDays(1)),
            servicio("rechazado", hora = 7, estado = EstadoServicio.RECHAZADO),
            servicio("terminado", hora = 8),
            servicio("pendiente", hora = 10, estado = EstadoServicio.PENDIENTE),
            servicio("manana", fecha = hoy.plusDays(1), hora = 6)
        )
        val checkins = mapOf("terminado" to checkin("terminado", checkOutEn = 5L))
        assertEquals("pendiente", DatosPerfil.proximoServicio(servicios, checkins, hoy)?.id)
    }

    @Test
    fun `proximo servicio incluye el que esta en curso`() {
        val servicios = listOf(servicio("en-curso", hora = 8), servicio("despues", hora = 15))
        assertEquals("en-curso", DatosPerfil.proximoServicio(servicios, mapOf("en-curso" to checkin("en-curso")), hoy)?.id)
    }

    @Test
    fun `sin nada por delante no hay proximo servicio`() {
        assertNull(DatosPerfil.proximoServicio(listOf(servicio("ayer", fecha = hoy.minusDays(1))), emptyMap(), hoy))
    }

    @Test
    fun `actividad cuenta terminados puntualidad y lo que falta`() {
        val zona = ZoneOffset.UTC
        val ahora = hoy.atTime(12, 0)
        fun enMinutos(fecha: LocalDate, hora: Int, minutos: Long) =
            fecha.atTime(hora, 0).plusMinutes(minutos).toInstant(zona).toEpochMilli()

        val servicios = listOf(
            servicio("a-tiempo", fecha = hoy.minusDays(1), hora = 8),
            servicio("atrasado", fecha = hoy.minusDays(2), hora = 8),
            servicio("hoy", hora = 18),
            servicio("futuro", fecha = hoy.plusDays(3)),
            servicio("sin-aceptar", fecha = hoy.plusDays(3), estado = EstadoServicio.PENDIENTE),
            servicio("antiguo", fecha = hoy.minusDays(7))
        )
        val checkins = mapOf(
            "a-tiempo" to CheckinServicio("a-tiempo", enMinutos(hoy.minusDays(1), 8, 5), 10, true, 1L),
            "atrasado" to CheckinServicio("atrasado", enMinutos(hoy.minusDays(2), 8, 40), 10, true, 1L),
            "antiguo" to CheckinServicio("antiguo", enMinutos(hoy.minusDays(7), 8, 0), 10, true, 1L)
        )

        assertEquals(
            ActividadPerfil(terminados = 2, porRealizar = 2, puntualidad = 50),
            DatosPerfil.actividad(servicios, checkins, ahora, zona)
        )
    }

    @Test
    fun `sin check-ins la puntualidad queda vacia`() {
        assertNull(DatosPerfil.actividad(listOf(servicio("1")), emptyMap(), hoy.atTime(12, 0), ZoneOffset.UTC).puntualidad)
    }

    @Test
    fun `cargo depende del rol`() {
        assertEquals("Transportista Southbound", DatosPerfil.cargo(Rol.CONDUCTOR))
        assertEquals("Guía Southbound", DatosPerfil.cargo(Rol.GUIA))
    }
}
