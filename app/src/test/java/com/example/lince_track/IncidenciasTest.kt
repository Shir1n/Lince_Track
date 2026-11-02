package com.example.lince_track

import com.example.lince_track.data.CambiosServicios
import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.IncidenciaServicio
import com.example.lince_track.data.ReglasIncidencia
import com.example.lince_track.data.Rol
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.TipoIncidencia
import com.example.lince_track.data.servicios.FakeServiciosDataSource
import com.example.lince_track.data.servicios.ServicioMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class IncidenciasTest {

    private val servicio: Servicio =
        ServicioMapper.aDominio(FakeServiciosDataSource.servicios(LocalDate.of(2026, 11, 2)).first())!!

    private fun incidencia(
        id: String,
        creadaEn: Long = 1_000L,
        resuelta: Boolean = false,
        servicioId: String = "8941"
    ) = IncidenciaServicio(
        id, servicioId, "Excursión", "uid-carlos", "Carlos Morales",
        TipoIncidencia.ATRASO, "Corte de ruta", creadaEn, resuelta
    )

    private fun checkin(checkOutEn: Long?) = CheckinServicio("8890", 0L, 10, true, checkOutEn)

    @Test
    fun `otro exige detalle y los demas tipos no`() {
        assertFalse(ReglasIncidencia.detalleValido(TipoIncidencia.OTRO, "  "))
        assertTrue(ReglasIncidencia.detalleValido(TipoIncidencia.OTRO, "Se cortó el camino"))
        assertTrue(ReglasIncidencia.detalleValido(TipoIncidencia.ATRASO, ""))
    }

    @Test
    fun `sin tipo o con detalle muy largo no se envia`() {
        assertFalse(ReglasIncidencia.detalleValido(null, "Detalle"))
        assertFalse(ReglasIncidencia.detalleValido(TipoIncidencia.ATRASO, "x".repeat(501)))
    }

    @Test
    fun `se reporta solo lo aceptado y sin check-out`() {
        val aceptado = servicio.copy(estado = EstadoServicio.ACEPTADO)

        assertTrue(ReglasIncidencia.puedeReportar(aceptado, null))
        assertTrue(ReglasIncidencia.puedeReportar(aceptado, checkin(null)))
        assertFalse(ReglasIncidencia.puedeReportar(aceptado, checkin(5_000L)))
        assertFalse(ReglasIncidencia.puedeReportar(servicio.copy(estado = EstadoServicio.PENDIENTE), null))
    }

    @Test
    fun `abiertas excluye las resueltas y parte por la mas reciente`() {
        val lista = listOf(
            incidencia("antigua", creadaEn = 1_000L),
            incidencia("reciente", creadaEn = 2_000L),
            incidencia("resuelta", creadaEn = 3_000L, resuelta = true)
        )

        assertEquals(listOf("reciente", "antigua"), ReglasIncidencia.abiertas(lista).map { it.id })
    }

    @Test
    fun `las de un servicio muestran primero las abiertas`() {
        val lista = listOf(
            incidencia("resuelta", creadaEn = 3_000L, resuelta = true),
            incidencia("abierta", creadaEn = 1_000L),
            incidencia("de-otro", servicioId = "1")
        )

        assertEquals(listOf("abierta", "resuelta"), ReglasIncidencia.deServicio(lista, "8941").map { it.id })
    }

    @Test
    fun `el resumen muestra el tipo y la hora`() {
        assertEquals("Atraso • 10:32", ReglasIncidencia.resumen(incidencia("1", creadaEn = 37_920_000L), ZoneOffset.UTC))
    }

    @Test
    fun `avisa al administrador de una incidencia nueva`() {
        val avisos = CambiosServicios.incidencias(emptyList(), listOf(incidencia("1")), Rol.ADMINISTRADOR)

        assertEquals("Incidencia: Atraso", avisos.single().titulo)
        assertEquals("Carlos Morales en Excursión", avisos.single().texto)
    }

    @Test
    fun `avisa a quien reporto cuando se resuelve`() {
        val avisos = CambiosServicios.incidencias(
            listOf(incidencia("1")), listOf(incidencia("1", resuelta = true)), Rol.CONDUCTOR
        )

        assertEquals("Incidencia resuelta", avisos.single().titulo)
    }

    @Test
    fun `quien reporta no recibe aviso de su propia incidencia`() {
        assertTrue(CambiosServicios.incidencias(emptyList(), listOf(incidencia("1")), Rol.CONDUCTOR).isEmpty())
    }

    @Test
    fun `el administrador no recibe aviso de lo que el mismo resolvio`() {
        val avisos = CambiosServicios.incidencias(
            listOf(incidencia("1")), listOf(incidencia("1", resuelta = true)), Rol.ADMINISTRADOR
        )

        assertTrue(avisos.isEmpty())
    }
}
