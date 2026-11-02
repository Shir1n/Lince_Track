package com.example.lince_track

import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.Rol
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.VehiculoServicio
import com.example.lince_track.data.servicios.FakeServiciosDataSource
import com.example.lince_track.data.servicios.ServicioMapper
import com.example.lince_track.ui.perfil.DatosPerfil
import com.example.lince_track.ui.perfil.TotalesPerfil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

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
    fun `cargo depende del rol`() {
        assertEquals("Transportista Southbound", DatosPerfil.cargo(Rol.CONDUCTOR))
        assertEquals("Guía Southbound", DatosPerfil.cargo(Rol.GUIA))
    }
}
