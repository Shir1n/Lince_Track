package com.example.lince_track

import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.PerfilUsuario
import com.example.lince_track.data.ReglasCompanero
import com.example.lince_track.data.RespuestaServicio
import com.example.lince_track.data.Rol
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.servicios.FakeServiciosDataSource
import com.example.lince_track.data.servicios.ServicioMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ReglasCompaneroTest {

    private val hoy = LocalDate.of(2026, 11, 2)
    private val servicios = FakeServiciosDataSource.servicios(hoy).mapNotNull { ServicioMapper.aDominio(it) }

    private fun servicio(id: String): Servicio = servicios.first { it.id == id }

    @Test
    fun `mientras el conductor no acepta el guia no ve ningun nombre`() {
        val guiado = ReglasCompanero.completar(servicio("8942"), listOf(servicio("8941")))

        assertEquals(ReglasCompanero.SIN_ASIGNAR, guiado.conductor)
    }

    @Test
    fun `mientras el guia no acepta el conductor no ve ningun nombre`() {
        val transporte = ReglasCompanero.completar(servicio("8941"), listOf(servicio("8942")))

        assertEquals(ReglasCompanero.SIN_ASIGNAR, transporte.guia)
    }

    @Test
    fun `cuando el conductor acepta el guia ve el nombre de su cuenta`() {
        val publicado = RespuestaServicio("8941", EstadoServicio.ACEPTADO, nombre = "Pedro Rojas")

        val guiado = ReglasCompanero.completar(
            servicio("8942"),
            listOf(ReglasCompanero.aplicarPublicado(servicio("8941"), publicado))
        )

        assertEquals("Pedro Rojas", guiado.conductor)
    }

    @Test
    fun `cuando el guia acepta el conductor ve el nombre de su cuenta`() {
        val publicado = RespuestaServicio("8942", EstadoServicio.ACEPTADO, nombre = "Alfredini Pastelini")

        val transporte = ReglasCompanero.completar(
            servicio("8941"),
            listOf(ReglasCompanero.aplicarPublicado(servicio("8942"), publicado))
        )

        assertEquals("Alfredini Pastelini", transporte.guia)
    }

    @Test
    fun `si lo publicado no trae nombre se usa el de la fuente`() {
        val publicado = RespuestaServicio("8941", EstadoServicio.ACEPTADO)

        val guiado = ReglasCompanero.completar(
            servicio("8942"),
            listOf(ReglasCompanero.aplicarPublicado(servicio("8941"), publicado))
        )

        assertEquals("Carlos Morales", guiado.conductor)
    }

    @Test
    fun `cuando el conductor rechaza vuelve a no haber nombre`() {
        val publicado = RespuestaServicio("8941", EstadoServicio.RECHAZADO, nombre = "Pedro Rojas")

        val guiado = ReglasCompanero.completar(
            servicio("8942"),
            listOf(ReglasCompanero.aplicarPublicado(servicio("8941"), publicado))
        )

        assertEquals(ReglasCompanero.SIN_ASIGNAR, guiado.conductor)
    }

    @Test
    fun `sin transporte en la reserva queda como sin transportista`() {
        val guiado = ReglasCompanero.completar(servicio("8901"), servicios)

        assertEquals(ServicioMapper.SIN_TRANSPORTISTA, guiado.conductor)
    }

    @Test
    fun `el servicio propio muestra el nombre de la cuenta`() {
        val perfil = PerfilUsuario(Rol.GUIA, nombre = "Alfredini Pastelini", guiaId = 7)

        assertEquals("Alfredini Pastelini", ReglasCompanero.conNombrePropio(servicio("8942"), perfil).guia)
    }

    @Test
    fun `una cuenta sin nombre mantiene el de la fuente`() {
        val perfil = PerfilUsuario(Rol.GUIA, nombre = " ", guiaId = 7)

        assertEquals("Andrea Soto", ReglasCompanero.conNombrePropio(servicio("8942"), perfil).guia)
    }

    @Test
    fun `el administrador no cambia los nombres`() {
        val perfil = PerfilUsuario(Rol.ADMINISTRADOR, nombre = "Admin")

        assertEquals("Andrea Soto", ReglasCompanero.conNombrePropio(servicio("8942"), perfil).guia)
    }

    @Test
    fun `no relaciona servicios del mismo tipo de personal`() {
        assertNull(ReglasCompanero.buscar(servicio("8941"), listOf(servicio("8941").copy(id = "1"))))
    }

    @Test
    fun `no relaciona servicios de la misma reserva en otro dia`() {
        val otroDia = servicio("8941").copy(fecha = hoy.plusDays(1))

        assertNull(ReglasCompanero.buscar(servicio("8942"), listOf(otroDia)))
    }

    @Test
    fun `no relaciona servicios de la misma reserva que no se cruzan en el horario`() {
        val temprano = servicio("8941").copy(horaInicio = LocalTime.of(7, 0), horaFin = LocalTime.of(8, 0))

        assertNull(ReglasCompanero.buscar(servicio("8942"), listOf(temprano)))
    }

    @Test
    fun `con varios transportes elige el de la hora mas cercana`() {
        val lejano = servicio("8941").copy(id = "1", horaInicio = LocalTime.of(11, 0))
        val cercano = servicio("8941").copy(id = "2", horaInicio = LocalTime.of(13, 15))

        assertEquals("2", ReglasCompanero.buscar(servicio("8942"), listOf(lejano, cercano))?.id)
    }
}
