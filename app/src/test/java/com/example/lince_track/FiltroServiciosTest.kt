package com.example.lince_track

import com.example.lince_track.data.PerfilUsuario
import com.example.lince_track.data.Rol
import com.example.lince_track.data.servicios.FakeServiciosDataSource
import com.example.lince_track.data.servicios.FiltroServicios
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Qué servicios se piden según el perfil del usuario con sesión iniciada. */
class FiltroServiciosTest {

    private val hoy = LocalDate.of(2026, 11, 1)
    private val fuente = FakeServiciosDataSource(hoy = { hoy })

    @Test
    fun `conductor filtra por su transportistaId`() {
        val filtro = FiltroServicios.paraPerfil(PerfilUsuario(Rol.CONDUCTOR, transportistaId = 12), hoy)!!

        assertEquals(12L, filtro.transportistaId)
        assertNull(filtro.guiaId)
    }

    @Test
    fun `guia filtra por su guiaId`() {
        val filtro = FiltroServicios.paraPerfil(PerfilUsuario(Rol.GUIA, guiaId = 7), hoy)!!

        assertEquals(7L, filtro.guiaId)
        assertNull(filtro.transportistaId)
    }

    @Test
    fun `administrador ve toda la oficina`() {
        val filtro = FiltroServicios.paraPerfil(PerfilUsuario(Rol.ADMINISTRADOR), hoy)!!

        assertNull(filtro.transportistaId)
        assertNull(filtro.guiaId)
        assertEquals(FiltroServicios.OFICINA_POR_DEFECTO, filtro.oficina)
    }

    @Test
    fun `cuenta sin id no pide servicios`() {
        assertNull(FiltroServicios.paraPerfil(PerfilUsuario(Rol.CONDUCTOR), hoy))
        assertNull(FiltroServicios.paraPerfil(PerfilUsuario(Rol.GUIA), hoy))
    }

    @Test
    fun `el id del otro rol no sirve`() {
        assertNull(FiltroServicios.paraPerfil(PerfilUsuario(Rol.CONDUCTOR, guiaId = 7), hoy))
        assertNull(FiltroServicios.paraPerfil(PerfilUsuario(Rol.GUIA, transportistaId = 12), hoy))
    }

    @Test
    fun `cada persona recibe solo sus servicios`() = runBlocking {
        val conductor = fuente.obtener(
            FiltroServicios.paraPerfil(PerfilUsuario(Rol.CONDUCTOR, transportistaId = 12), hoy)!!
        )
        val guia = fuente.obtener(
            FiltroServicios.paraPerfil(PerfilUsuario(Rol.GUIA, guiaId = 7), hoy)!!
        )
        val todos = fuente.obtener(FiltroServicios.paraPerfil(PerfilUsuario(Rol.ADMINISTRADOR), hoy)!!)

        assertTrue(conductor.isNotEmpty())
        assertTrue(conductor.all { it.transportista?.id == 12L })
        assertTrue(guia.isNotEmpty())
        assertTrue(guia.all { it.guia?.id == 7L })
        assertTrue(todos.size > conductor.size)
        assertTrue(todos.size > guia.size)
    }
}
