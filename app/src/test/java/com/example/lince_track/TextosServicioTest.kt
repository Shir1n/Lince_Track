package com.example.lince_track

import com.example.lince_track.data.CambiosServicios
import com.example.lince_track.data.Datos
import com.example.lince_track.data.ReglasCompanero
import com.example.lince_track.data.Rol
import com.example.lince_track.data.TextosServicio
import com.example.lince_track.data.servicios.ServicioMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/** Nombres de actividades y marcadores de persona en el idioma de la app (los lugares no se traducen). */
class TextosServicioTest {

    private val ingles = Locale.ENGLISH
    private val espanol = Locale.forLanguageTag("es-CL")

    @Test
    fun traduceLoGenericoYDejaLosLugares() {
        assertEquals("Tour Frutillar and Lago Llanquihue, no guide", TextosServicio.titulo("Tour Frutillar y Lago Llanquihue, sin guía", ingles))
        assertEquals("Half-day guided service (English)", TextosServicio.titulo("Servicio guiado medio día (Inglés)", ingles))
        assertEquals("Half-day guided service (Portuguese)", TextosServicio.titulo("Servicio guiado medio día (Portugués)", ingles))
        assertEquals("Excursion Chiloé (full day)", TextosServicio.titulo("Excursión Chiloé (día completo)", ingles))
        assertEquals("Excursion Saltos del Petrohué", TextosServicio.titulo("Excursión Saltos del Petrohué", ingles))
        assertEquals("City Tour Puerto Varas, no guide", TextosServicio.titulo("City Tour Puerto Varas, sin guía", ingles))
        assertEquals(
            "Transfer Aeropuerto El Tepual – Hotel Costanera Puerto Varas",
            TextosServicio.titulo("Transfer Aeropuerto El Tepual – Hotel Costanera Puerto Varas", ingles)
        )
    }

    @Test
    fun enEspanolQuedaComoVieneDeLaApi() {
        assertEquals("Tour Frutillar y Lago Llanquihue, sin guía", TextosServicio.titulo("Tour Frutillar y Lago Llanquihue, sin guía", espanol))
    }

    @Test
    fun losMarcadoresNoSonPersonas() {
        assertTrue(TextosServicio.esSinPersona(ServicioMapper.SIN_GUIA))
        assertTrue(TextosServicio.esSinPersona(ServicioMapper.SIN_TRANSPORTISTA))
        assertTrue(TextosServicio.esSinPersona(ReglasCompanero.SIN_ASIGNAR))
        assertFalse(TextosServicio.esSinPersona("Carlos Morales R."))
        assertEquals("Sin guía", TextosEs.de(TextosServicio.nombre(ServicioMapper.SIN_GUIA)))
        assertEquals("Carlos Morales R.", TextosEs.de(TextosServicio.nombre("Carlos Morales R.")))
    }

    @Test
    fun deSinGuiaASinAsignarNoHayAviso() {
        val hoy = LocalDate.of(2026, 11, 20)
        val servicio = Datos.servicio(3008)
        val antes = listOf(servicio.copy(guia = ServicioMapper.SIN_GUIA))
        val ahora = listOf(servicio.copy(guia = ReglasCompanero.SIN_ASIGNAR))

        assertTrue(CambiosServicios.detectar(antes, ahora, Rol.CONDUCTOR, hoy).isEmpty())
    }
}
