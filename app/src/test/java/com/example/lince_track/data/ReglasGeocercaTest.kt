package com.example.lince_track.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ReglasGeocercaTest {

    private val hotel = Geocerca(1, "Hotel Centro Puerto Varas", -41.3192, -72.9858, 150)
    private val parametros = ParametrosCheckin(anticipacionMinutos = 15, precisionMaximaMetros = 50)
    private val pickup = LocalDateTime.of(2026, 11, 27, 8, 30)
    private val aTiempo = LocalDateTime.of(2026, 11, 27, 8, 0)

    /** Punto a unos [metros] al norte del hotel (1° de latitud ≈ 111.195 m). */
    private fun alNorte(metros: Double) = hotel.lat + metros / 111_195.0

    private fun evaluar(
        geocerca: Geocerca? = hotel,
        lat: Double = hotel.lat,
        gpsReal: Boolean = true,
        precision: Float? = 10f,
        ahora: LocalDateTime = aTiempo
    ) = ReglasGeocerca.evaluar(geocerca, lat, hotel.lon, gpsReal, precision, ahora, pickup, parametros)

    @Test
    fun buscaLaGeocercaPorElNombreDelPickupSinDistinguirMayusculas() {
        val otra = Geocerca(2, "Aeropuerto El Tepual", -41.4386, -73.0939, 150)
        val lista = listOf(otra, hotel)

        assertSame(hotel, ReglasGeocerca.buscar(lista, "  hotel centro PUERTO VARAS "))
        assertNull(ReglasGeocerca.buscar(lista, "Hotel Cumbres"))
        assertNull(ReglasGeocerca.buscar(lista, "   "))
    }

    @Test
    fun laDistanciaCalzaConLaDelBackend() {
        // De Puerto Varas al aeropuerto El Tepual hay unos 16 km en línea recta.
        val metros = ReglasGeocerca.distanciaMetros(hotel.lat, hotel.lon, -41.4386, -73.0939)

        assertEquals(16_050.0, metros, 100.0)
        assertEquals(0.0, ReglasGeocerca.distanciaMetros(hotel.lat, hotel.lon, hotel.lat, hotel.lon), 0.001)
    }

    @Test
    fun dentroDelRadioAtiempoYConGpsPrecisoSePermite() {
        val evaluacion = evaluar(lat = alNorte(100.0))

        assertEquals(ResultadoCheckin.VERIFICADO, evaluacion.resultado)
        assertTrue(evaluacion.resultado.permitido)
        assertEquals(100, evaluacion.distanciaMetros)
        assertEquals(10, evaluacion.precisionMetros)
        assertEquals(LocalDateTime.of(2026, 11, 27, 8, 15), evaluacion.limite)
    }

    @Test
    fun fueraDelRadioSeBloquea() {
        val evaluacion = evaluar(lat = alNorte(230.0))

        assertEquals(ResultadoCheckin.FUERA_DE_ZONA, evaluacion.resultado)
        assertFalse(evaluacion.resultado.permitido)
        assertEquals(230, evaluacion.distanciaMetros)
    }

    @Test
    fun justoEnElRadioYEnLaHoraLimiteTodaviaSePermite() {
        val evaluacion = evaluar(lat = alNorte(149.0), ahora = LocalDateTime.of(2026, 11, 27, 8, 15))

        assertEquals(ResultadoCheckin.VERIFICADO, evaluacion.resultado)
    }

    @Test
    fun despuesDeLaHoraLimiteQuedaTarde() {
        val evaluacion = evaluar(ahora = LocalDateTime.of(2026, 11, 27, 8, 15, 1))

        assertEquals(ResultadoCheckin.TARDE, evaluacion.resultado)
    }

    @Test
    fun elOrdenEsElMismoDelBackend() {
        val lejosYTarde = LocalDateTime.of(2026, 11, 27, 9, 0)

        assertEquals(ResultadoCheckin.SIN_GEOCERCA, evaluar(geocerca = null, gpsReal = false).resultado)
        assertEquals(ResultadoCheckin.SIN_GPS_REAL, evaluar(gpsReal = false, lat = alNorte(500.0)).resultado)
        assertEquals(ResultadoCheckin.SIN_GPS_REAL, evaluar(precision = null).resultado)
        assertEquals(ResultadoCheckin.GPS_IMPRECISO, evaluar(precision = 80f, lat = alNorte(500.0)).resultado)
        assertEquals(ResultadoCheckin.FUERA_DE_ZONA, evaluar(lat = alNorte(500.0), ahora = lejosYTarde).resultado)
    }

    @Test
    fun sinGeocercaNoHayDistancia() {
        val evaluacion = evaluar(geocerca = null)

        assertNull(evaluacion.distanciaMetros)
        assertNull(evaluacion.geocerca)
    }

    @Test
    fun leeLosResultadosDelBackend() {
        assertEquals(ResultadoCheckin.VERIFICADO, ResultadoCheckin.desdeBackend("VERIFICADO"))
        assertEquals(ResultadoCheckin.TARDE, ResultadoCheckin.desdeBackend("TARDE"))
        // REGISTRADO es de las posiciones; SIN_GPS_REAL solo existe en la app.
        assertNull(ResultadoCheckin.desdeBackend("REGISTRADO"))
        assertNull(ResultadoCheckin.desdeBackend("SIN_GPS_REAL"))
        assertNull(ResultadoCheckin.desdeBackend(null))
    }

    @Test
    fun laIncidenciaEsAtrasoSoloSiLlegoTarde() {
        assertEquals(TipoIncidencia.ATRASO, ReglasGeocerca.tipoIncidencia(ResultadoCheckin.TARDE))
        assertEquals(TipoIncidencia.OTRO, ReglasGeocerca.tipoIncidencia(ResultadoCheckin.FUERA_DE_ZONA))
    }

    @Test
    fun elDetalleDeLaIncidenciaTraeDistanciaRadioYGps() {
        val fuera = ReglasGeocerca.detalleIncidencia(evaluar(lat = alNorte(230.0)), "Hotel Centro Puerto Varas")
        val tarde = ReglasGeocerca.detalleIncidencia(evaluar(ahora = LocalDateTime.of(2026, 11, 27, 9, 0)), "x")
        val sinGeocerca = ReglasGeocerca.detalleIncidencia(evaluar(geocerca = null, precision = null), "Hotel Cumbres")

        assertEquals(
            "Check-in bloqueado: fuera de la zona. Hotel Centro Puerto Varas: a 230 m (radio 150 m), GPS ± 10 m.",
            fuera
        )
        assertTrue(tarde.contains("después de la hora límite (08:15)"))
        assertEquals(
            "Check-in bloqueado: el punto de recogida no tiene geocerca. Hotel Cumbres: a sin dato " +
                "(radio sin geocerca), GPS sin dato.",
            sinGeocerca
        )
        assertTrue(ReglasGeocerca.detalleIncidencia(evaluar(gpsReal = false), "x").contains("sin GPS real"))
        assertTrue(ReglasGeocerca.detalleIncidencia(evaluar(precision = 90f), "x").contains("GPS impreciso"))
        assertTrue(ReglasGeocerca.detalleIncidencia(evaluar(), "x").contains("verificado"))
        assertTrue(fuera.length <= ReglasIncidencia.MAX_DETALLE)
    }
}
