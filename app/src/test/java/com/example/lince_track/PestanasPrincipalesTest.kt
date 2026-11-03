package com.example.lince_track

import com.example.lince_track.data.Rol
import com.example.lince_track.ui.navigation.Routes
import com.example.lince_track.ui.navigation.bottomNavItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PestanasPrincipalesTest {

    @Test
    fun conductorTiene4PestanasEnOrdenCorrecto() {
        val items = bottomNavItems(Rol.CONDUCTOR)
        assertEquals(4, items.size)
        assertEquals(Routes.SERVICIOS, items[0].route)
        assertEquals("Servicios", items[0].label)
        assertEquals(Routes.CALENDARIO, items[1].route)
        assertEquals("Calendario", items[1].label)
        assertEquals(Routes.ALERTAS, items[2].route)
        assertEquals("Alertas", items[2].label)
        assertEquals(Routes.PERFIL, items[3].route)
        assertEquals("Perfil", items[3].label)
    }

    @Test
    fun guiaTiene4PestanasEnOrdenCorrecto() {
        val items = bottomNavItems(Rol.GUIA)
        assertEquals(4, items.size)
        assertEquals(Routes.GUIA_ASIGNACIONES, items[0].route)
        assertEquals("Asignaciones", items[0].label)
        assertEquals(Routes.CALENDARIO, items[1].route)
        assertEquals("Calendario", items[1].label)
        assertEquals(Routes.ALERTAS, items[2].route)
        assertEquals("Alertas", items[2].label)
        assertEquals(Routes.GUIA_PERFIL, items[3].route)
        assertEquals("Perfil", items[3].label)
    }

    @Test
    fun administradorTiene4PestanasEnOrdenCorrecto() {
        val items = bottomNavItems(Rol.ADMINISTRADOR)
        assertEquals(4, items.size)
        assertEquals(Routes.ADMIN_PANEL, items[0].route)
        assertEquals("Panel", items[0].label)
        assertEquals(Routes.ADMIN_RESUMEN, items[1].route)
        assertEquals("Resumen", items[1].label)
        assertEquals(Routes.CALENDARIO, items[2].route)
        assertEquals("Calendario", items[2].label)
        assertEquals(Routes.ADMIN_PERFIL, items[3].route)
        assertEquals("Perfil", items[3].label)
    }

    @Test
    fun rolNuloUsaPestanasPorDefectoDeConductor() {
        val items = bottomNavItems(null)
        assertEquals(4, items.size)
        assertEquals(Routes.SERVICIOS, items[0].route)
    }

    @Test
    fun rutaMainYMainConductorSonCompatibles() {
        assertEquals("main", Routes.MAIN)
        assertEquals(Routes.MAIN, Routes.MAIN_CONDUCTOR)
    }
}
