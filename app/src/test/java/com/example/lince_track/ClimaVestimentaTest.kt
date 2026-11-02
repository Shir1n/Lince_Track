package com.example.lince_track

import com.example.lince_track.data.clima.ClimaInfo
import com.example.lince_track.data.clima.ClimaService
import com.example.lince_track.data.clima.NivelRiesgo
import com.example.lince_track.data.clima.TipoClima
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para las recomendaciones de vestimenta del guía
 * según el clima en el destino turístico.
 */
class ClimaVestimentaTest {

    private fun clima(tipo: TipoClima, tempC: Double = 15.0, windKmH: Double = 10.0) = ClimaInfo(
        tempC = tempC,
        weatherCode = 0,
        windSpeedKmH = windKmH,
        precipitationMm = 0.0,
        tipo = tipo,
        descripcionCorta = tipo.label,
        esSimulado = false
    )

    @Test
    fun `lluvia fuerte genera alerta de peligro y vestimenta impermeable`() {
        val recomendacion = ClimaService.generarRecomendacionVestimenta(
            clima = clima(TipoClima.LLUVIA_FUERTE),
            nombreGuia = "Andrea Soto",
            destino = "Saltos del Petrohué"
        )

        assertEquals(NivelRiesgo.PELIGRO, recomendacion.nivel)
        assertTrue(recomendacion.alertaCorta.contains("Lluvia intensa", ignoreCase = true))
        assertTrue(recomendacion.vestimentaSugerida.any { it.contains("impermeable", ignoreCase = true) })
        assertTrue(recomendacion.vestimentaSugerida.any { it.contains("antideslizante", ignoreCase = true) })
    }

    @Test
    fun `dia despejado genera alerta optima con proteccion solar y lentes UV`() {
        val recomendacion = ClimaService.generarRecomendacionVestimenta(
            clima = clima(TipoClima.DESPEJADO, tempC = 23.0),
            nombreGuia = "Andrea Soto",
            destino = "Lago Todos los Santos"
        )

        assertEquals(NivelRiesgo.OPTIMO, recomendacion.nivel)
        assertTrue(recomendacion.vestimentaSugerida.any { it.contains("Protector solar", ignoreCase = true) })
        assertTrue(recomendacion.vestimentaSugerida.any { it.contains("UV", ignoreCase = true) })
    }

    @Test
    fun `nieve en cordillera exige prendas termicas y abrigo grueso`() {
        val recomendacion = ClimaService.generarRecomendacionVestimenta(
            clima = clima(TipoClima.NIEVE, tempC = 0.5),
            nombreGuia = "Andrea Soto",
            destino = "Volcán Osorno"
        )

        assertEquals(NivelRiesgo.PELIGRO, recomendacion.nivel)
        assertTrue(recomendacion.vestimentaSugerida.any { it.contains("térmica", ignoreCase = true) })
        assertTrue(recomendacion.vestimentaSugerida.any { it.contains("Guantes", ignoreCase = true) })
    }

    @Test
    fun `viento fuerte advierte sobre objetos sueltos y cortavientos`() {
        val recomendacion = ClimaService.generarRecomendacionVestimenta(
            clima = clima(TipoClima.VIENTO_FUERTE, windKmH = 55.0),
            nombreGuia = "Andrea Soto",
            destino = "Ensenada"
        )

        assertEquals(NivelRiesgo.PRECAUCION, recomendacion.nivel)
        assertTrue(recomendacion.vestimentaSugerida.any { it.contains("cortaviento", ignoreCase = true) })
    }
}
