package com.example.lince_track

import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.ReglasCheckin
import com.example.lince_track.data.Rol
import com.example.lince_track.data.clima.zonaClima
import com.example.lince_track.data.formatHora
import com.example.lince_track.ui.ruta.RolMapa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneOffset

/** Textos del check-in, zona del clima y vista del mapa según el rol. */
class CheckinServicioTest {

    private val diezYMedia = 37_800_000L // 10:30 UTC del 01-01-1970
    private val doce = 43_200_000L // 12:00 UTC del 01-01-1970

    private fun checkin(gpsReal: Boolean = true, checkOutEn: Long? = null) =
        CheckinServicio("8941", diezYMedia, distanciaM = 120, gpsReal = gpsReal, checkOutEn = checkOutEn)

    @Test
    fun `distancia se muestra en metros o kilometros`() {
        assertEquals("850 m", ReglasCheckin.formatoDistancia(850))
        assertEquals("2,5 km", ReglasCheckin.formatoDistancia(2_500))
        assertEquals("980 km", ReglasCheckin.formatoDistancia(980_000))
    }

    @Test
    fun `resumen en ruta muestra hora y distancia`() {
        val hora = formatHora(LocalTime.of(10, 30))
        assertEquals("Check-in $hora • a 120 m", ReglasCheckin.resumen(checkin(), ZoneOffset.UTC))
    }

    @Test
    fun `resumen finalizado muestra la hora de termino`() {
        val hora = formatHora(LocalTime.of(12, 0))
        assertEquals("Finalizado $hora", ReglasCheckin.resumen(checkin(checkOutEn = doce), ZoneOffset.UTC))
    }

    @Test
    fun `resumen avisa si el GPS era simulado`() {
        assertTrue(ReglasCheckin.resumen(checkin(gpsReal = false), ZoneOffset.UTC).endsWith("(GPS simulado)"))
    }

    @Test
    fun `finalizado depende del check-out`() {
        assertFalse(checkin().finalizado)
        assertTrue(checkin(checkOutEn = doce).finalizado)
    }

    @Test
    fun `lecturas cercanas de GPS son la misma zona de clima`() {
        assertEquals(zonaClima(-41.3195, -72.9854), zonaClima(-41.3198, -72.9851))
        assertNotEquals(zonaClima(-41.3195, -72.9854), zonaClima(-41.1731, -72.4468))
    }

    @Test
    fun `la vista del mapa sale del rol real`() {
        assertEquals(RolMapa.TRANSPORTISTA, RolMapa.desde(Rol.CONDUCTOR))
        assertEquals(RolMapa.GUIA, RolMapa.desde(Rol.GUIA))
        assertEquals(RolMapa.COORDINADOR, RolMapa.desde(Rol.ADMINISTRADOR))
    }
}
