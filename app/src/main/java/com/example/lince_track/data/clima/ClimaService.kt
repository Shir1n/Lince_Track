package com.example.lince_track.data.clima

import com.example.lince_track.R
import com.example.lince_track.util.LinceLogger
import com.example.lince_track.util.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

/*
 * Los textos que ve la persona (tipo de clima, alertas, consejos y vestimenta) van como UiText: la tarjeta del mapa los
 * traduce al idioma elegido en la app. Las `label` en español quedan solo para el registro (LinceLogger).
 */

private fun texto(resId: Int, vararg args: Any): UiText = UiText.StringResource(resId, *args)

enum class TipoClima(val label: String, val texto: UiText) {
    DESPEJADO("Despejado", texto(R.string.clima_tipo_despejado)),
    PARCIALMENTE_NUBLADO("Parcialmente nublado", texto(R.string.clima_tipo_parcial)),
    NUBLADO("Nublado", texto(R.string.clima_tipo_nublado)),
    NIEBLA("Niebla / Neblina", texto(R.string.clima_tipo_niebla)),
    LLUVIA_LIGERA("Llovizna / Lluvia ligera", texto(R.string.clima_tipo_lluvia_ligera)),
    LLUVIA_MODERADA("Lluvia moderada", texto(R.string.clima_tipo_lluvia_moderada)),
    LLUVIA_FUERTE("Lluvia torrencial", texto(R.string.clima_tipo_lluvia_fuerte)),
    TORMENTA("Tormenta eléctrica", texto(R.string.clima_tipo_tormenta)),
    NIEVE("Nieve / Aguanieve", texto(R.string.clima_tipo_nieve)),
    VIENTO_FUERTE("Ráfagas de viento fuerte", texto(R.string.clima_tipo_viento))
}

enum class NivelRiesgo(val texto: UiText) {
    OPTIMO(texto(R.string.clima_nivel_optimo)),
    PRECAUCION(texto(R.string.clima_nivel_precaucion)),
    PELIGRO(texto(R.string.clima_nivel_peligro))
}

data class ClimaInfo(
    val tempC: Double,
    val weatherCode: Int,
    val windSpeedKmH: Double,
    val precipitationMm: Double,
    val tipo: TipoClima,
    val descripcionCorta: UiText,
    val esSimulado: Boolean = false
)

data class RecomendacionConduccion(
    val nivel: NivelRiesgo,
    val alertaCorta: UiText,
    val mensajePersonalizado: UiText,
    val consejos: List<UiText>
)

data class RecomendacionVestimenta(
    val nivel: NivelRiesgo,
    val alertaCorta: UiText,
    val mensajePersonalizado: UiText,
    val vestimentaSugerida: List<UiText>,
    val consejosGrupo: List<UiText>
)

enum class PresetClima(val texto: UiText, val clima: ClimaInfo) {
    REAL(
        texto(R.string.clima_preset_real),
        ClimaInfo(
            tempC = 18.0,
            weatherCode = 0,
            windSpeedKmH = 10.0,
            precipitationMm = 0.0,
            tipo = TipoClima.DESPEJADO,
            descripcionCorta = texto(R.string.clima_desc_tiempo_real),
            esSimulado = false
        )
    ),
    LLUVIA_TORRENCIAL(
        texto(R.string.clima_preset_lluvia),
        ClimaInfo(
            tempC = 11.2,
            weatherCode = 65,
            windSpeedKmH = 34.5,
            precipitationMm = 9.8,
            tipo = TipoClima.LLUVIA_FUERTE,
            descripcionCorta = texto(R.string.clima_desc_lluvia_inundada),
            esSimulado = true
        )
    ),
    NEBLINA_DENSA(
        texto(R.string.clima_preset_neblina),
        ClimaInfo(
            tempC = 7.4,
            weatherCode = 45,
            windSpeedKmH = 5.2,
            precipitationMm = 0.0,
            tipo = TipoClima.NIEBLA,
            descripcionCorta = texto(R.string.clima_desc_neblina_densa),
            esSimulado = true
        )
    ),
    VIENTO_FUERTE(
        texto(R.string.clima_preset_viento),
        ClimaInfo(
            tempC = 15.0,
            weatherCode = 3,
            windSpeedKmH = 65.0,
            precipitationMm = 0.0,
            tipo = TipoClima.VIENTO_FUERTE,
            descripcionCorta = texto(R.string.clima_desc_rafagas_laterales),
            esSimulado = true
        )
    ),
    DESPEJADO(
        texto(R.string.clima_preset_despejado),
        ClimaInfo(
            tempC = 22.5,
            weatherCode = 0,
            windSpeedKmH = 12.0,
            precipitationMm = 0.0,
            tipo = TipoClima.DESPEJADO,
            descripcionCorta = texto(R.string.clima_desc_despejado_seco),
            esSimulado = true
        )
    )
}

object ClimaService {

    suspend fun consultarOpenMeteo(lat: Double, lon: Double): Result<ClimaInfo> = withContext(Dispatchers.IO) {
        // Open-Meteo es un servicio externo: para el clima basta la zona, así que se envían solo 2 decimales
        // (unos 1 km) y no la posición exacta de la persona. Tampoco se anota en el registro.
        val urlStr = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current=temperature_2m,precipitation,weather_code,wind_speed_10m",
            lat,
            lon
        )
        LinceLogger.i("CLIMA", "Consultando Open-Meteo")

        try {
            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "LinceTrackChile/1.0 (cl.duoc.lince_track)")
            }

            val code = connection.responseCode
            if (code != 200) {
                LinceLogger.w("CLIMA", "Open-Meteo HTTP $code", "Fallo al consultar endpoint")
                return@withContext Result.failure(Exception("HTTP $code al consultar Open-Meteo"))
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val jsonText = reader.use { it.readText() }
            val root = JSONObject(jsonText)
            val current = root.getJSONObject("current")

            val temp = current.optDouble("temperature_2m", 15.0)
            val precip = current.optDouble("precipitation", 0.0)
            val wmoCode = current.optInt("weather_code", 0)
            val wind = current.optDouble("wind_speed_10m", 10.0)

            val tipo = clasificarWmo(wmoCode, wind, precip)
            val descripcion = describeWmo(wmoCode, wind, precip)

            val clima = ClimaInfo(
                tempC = temp,
                weatherCode = wmoCode,
                windSpeedKmH = wind,
                precipitationMm = precip,
                tipo = tipo,
                descripcionCorta = descripcion,
                esSimulado = false
            )

            LinceLogger.i(
                "CLIMA",
                "Clima obtenido con éxito: ${temp.roundToInt()}°C, ${tipo.label}",
                "WMO=$wmoCode | Viento=${wind.roundToInt()}km/h | Lluvia=${precip}mm"
            )

            Result.success(clima)
        } catch (e: Exception) {
            LinceLogger.e("CLIMA", "Error conectando con Open-Meteo: ${e.message}", throwable = e)
            Result.failure(e)
        }
    }

    private fun clasificarWmo(code: Int, windKmH: Double, precipMm: Double): TipoClima {
        if (windKmH >= 45.0) return TipoClima.VIENTO_FUERTE
        return when (code) {
            0 -> TipoClima.DESPEJADO
            1, 2 -> TipoClima.PARCIALMENTE_NUBLADO
            3 -> TipoClima.NUBLADO
            45, 48 -> TipoClima.NIEBLA
            51, 53, 55 -> TipoClima.LLUVIA_LIGERA
            61, 80 -> TipoClima.LLUVIA_LIGERA
            63, 81 -> TipoClima.LLUVIA_MODERADA
            65, 82 -> TipoClima.LLUVIA_FUERTE
            71, 73, 75, 77, 85, 86 -> TipoClima.NIEVE
            95, 96, 99 -> TipoClima.TORMENTA
            else -> if (precipMm > 2.0) TipoClima.LLUVIA_MODERADA else TipoClima.PARCIALMENTE_NUBLADO
        }
    }

    private fun describeWmo(code: Int, windKmH: Double, precipMm: Double): UiText {
        return when {
            windKmH >= 50.0 -> texto(R.string.clima_desc_rafagas_kmh, windKmH.roundToInt())
            code == 0 -> texto(R.string.clima_desc_cielo_despejado)
            code in 1..2 -> texto(R.string.clima_desc_parcial)
            code == 3 -> texto(R.string.clima_desc_nublado)
            code in listOf(45, 48) -> texto(R.string.clima_desc_niebla)
            code in listOf(51, 53, 55) -> texto(R.string.clima_desc_llovizna)
            code in listOf(61, 80) -> texto(R.string.clima_desc_lluvia_ligera)
            code in listOf(63, 81) -> texto(R.string.clima_desc_lluvia_moderada)
            code in listOf(65, 82) -> texto(R.string.clima_desc_precipitaciones, precipMm.toString())
            code in 71..77 -> texto(R.string.clima_desc_nieve)
            code >= 95 -> texto(R.string.clima_desc_tormenta)
            else -> texto(R.string.clima_desc_estandar)
        }
    }

    fun generarRecomendacion(
        clima: ClimaInfo,
        nombreConductor: String,
        destino: String
    ): RecomendacionConduccion {
        val chofer: Any =
            if (nombreConductor.isBlank()) texto(R.string.clima_conductor_generico) else nombreConductor.trim()

        fun reco(nivel: NivelRiesgo, alerta: UiText, mensaje: Int, vararg consejos: Int) = RecomendacionConduccion(
            nivel = nivel,
            alertaCorta = alerta,
            mensajePersonalizado = texto(mensaje, chofer, destino),
            consejos = consejos.map { texto(it) }
        )

        return when (clima.tipo) {
            TipoClima.LLUVIA_FUERTE, TipoClima.TORMENTA -> reco(
                NivelRiesgo.PELIGRO,
                texto(R.string.clima_c_lluvia_fuerte_alerta),
                R.string.clima_c_lluvia_fuerte_msg,
                R.string.clima_c_lluvia_fuerte_1, R.string.clima_c_lluvia_fuerte_2, R.string.clima_c_lluvia_fuerte_3
            )

            TipoClima.LLUVIA_MODERADA, TipoClima.LLUVIA_LIGERA -> reco(
                NivelRiesgo.PRECAUCION,
                texto(R.string.clima_c_lluvia_alerta),
                R.string.clima_c_lluvia_msg,
                R.string.clima_c_lluvia_1, R.string.clima_c_lluvia_2, R.string.clima_c_lluvia_3
            )

            TipoClima.NIEBLA -> reco(
                NivelRiesgo.PELIGRO,
                texto(R.string.clima_c_niebla_alerta),
                R.string.clima_c_niebla_msg,
                R.string.clima_c_niebla_1, R.string.clima_c_niebla_2, R.string.clima_c_niebla_3
            )

            TipoClima.VIENTO_FUERTE -> reco(
                NivelRiesgo.PRECAUCION,
                texto(R.string.clima_c_viento_alerta, clima.windSpeedKmH.roundToInt()),
                R.string.clima_c_viento_msg,
                R.string.clima_c_viento_1, R.string.clima_c_viento_2, R.string.clima_c_viento_3
            )

            TipoClima.NIEVE -> reco(
                NivelRiesgo.PELIGRO,
                texto(R.string.clima_c_nieve_alerta),
                R.string.clima_c_nieve_msg,
                R.string.clima_c_nieve_1, R.string.clima_c_nieve_2, R.string.clima_c_nieve_3
            )

            TipoClima.DESPEJADO, TipoClima.PARCIALMENTE_NUBLADO, TipoClima.NUBLADO -> reco(
                NivelRiesgo.OPTIMO,
                texto(R.string.clima_c_despejado_alerta),
                R.string.clima_c_despejado_msg,
                R.string.clima_c_despejado_1, R.string.clima_c_despejado_2, R.string.clima_c_despejado_3
            )
        }
    }

    fun generarRecomendacionVestimenta(
        clima: ClimaInfo,
        nombreGuia: String,
        destino: String
    ): RecomendacionVestimenta {
        val guia: Any = nombreGuia.ifBlank { null } ?: texto(R.string.clima_guia_generico)

        fun reco(
            nivel: NivelRiesgo,
            alerta: UiText,
            mensaje: UiText,
            prendas: List<Int>,
            consejos: List<Int>
        ) = RecomendacionVestimenta(
            nivel = nivel,
            alertaCorta = alerta,
            mensajePersonalizado = mensaje,
            vestimentaSugerida = prendas.map { texto(it) },
            consejosGrupo = consejos.map { texto(it) }
        )

        return when (clima.tipo) {
            TipoClima.LLUVIA_FUERTE, TipoClima.TORMENTA -> reco(
                NivelRiesgo.PELIGRO,
                texto(R.string.clima_v_lluvia_fuerte_alerta),
                texto(R.string.clima_v_lluvia_fuerte_msg, guia, destino),
                listOf(
                    R.string.clima_v_lluvia_fuerte_p1, R.string.clima_v_lluvia_fuerte_p2,
                    R.string.clima_v_lluvia_fuerte_p3, R.string.clima_v_lluvia_fuerte_p4
                ),
                listOf(R.string.clima_v_lluvia_fuerte_c1, R.string.clima_v_lluvia_fuerte_c2, R.string.clima_v_lluvia_fuerte_c3)
            )

            TipoClima.LLUVIA_MODERADA, TipoClima.LLUVIA_LIGERA -> reco(
                NivelRiesgo.PRECAUCION,
                texto(R.string.clima_v_lluvia_alerta),
                texto(R.string.clima_v_lluvia_msg, guia, destino),
                listOf(R.string.clima_v_lluvia_p1, R.string.clima_v_lluvia_p2, R.string.clima_v_lluvia_p3),
                listOf(R.string.clima_v_lluvia_c1, R.string.clima_v_lluvia_c2, R.string.clima_v_lluvia_c3)
            )

            TipoClima.NIEVE -> reco(
                NivelRiesgo.PELIGRO,
                texto(R.string.clima_v_nieve_alerta),
                texto(R.string.clima_v_nieve_msg, guia, destino),
                listOf(R.string.clima_v_nieve_p1, R.string.clima_v_nieve_p2, R.string.clima_v_nieve_p3, R.string.clima_v_nieve_p4),
                listOf(R.string.clima_v_nieve_c1, R.string.clima_v_nieve_c2, R.string.clima_v_nieve_c3)
            )

            TipoClima.NIEBLA -> reco(
                NivelRiesgo.PRECAUCION,
                texto(R.string.clima_v_niebla_alerta),
                texto(R.string.clima_v_niebla_msg, guia, destino),
                listOf(R.string.clima_v_niebla_p1, R.string.clima_v_niebla_p2, R.string.clima_v_niebla_p3),
                listOf(R.string.clima_v_niebla_c1, R.string.clima_v_niebla_c2, R.string.clima_v_niebla_c3)
            )

            TipoClima.VIENTO_FUERTE -> reco(
                NivelRiesgo.PRECAUCION,
                texto(R.string.clima_v_viento_alerta, clima.windSpeedKmH.roundToInt()),
                texto(R.string.clima_v_viento_msg, guia, destino),
                listOf(R.string.clima_v_viento_p1, R.string.clima_v_viento_p2, R.string.clima_v_viento_p3),
                listOf(R.string.clima_v_viento_c1, R.string.clima_v_viento_c2, R.string.clima_v_viento_c3)
            )

            TipoClima.DESPEJADO, TipoClima.PARCIALMENTE_NUBLADO -> reco(
                NivelRiesgo.OPTIMO,
                texto(R.string.clima_v_despejado_alerta),
                texto(R.string.clima_v_despejado_msg, guia, destino),
                listOf(
                    R.string.clima_v_despejado_p1, R.string.clima_v_despejado_p2,
                    R.string.clima_v_despejado_p3, R.string.clima_v_despejado_p4
                ),
                listOf(R.string.clima_v_despejado_c1, R.string.clima_v_despejado_c2, R.string.clima_v_despejado_c3)
            )

            TipoClima.NUBLADO -> reco(
                NivelRiesgo.OPTIMO,
                texto(R.string.clima_v_nublado_alerta),
                texto(R.string.clima_v_nublado_msg, destino),
                listOf(R.string.clima_v_nublado_p1, R.string.clima_v_nublado_p2, R.string.clima_v_nublado_p3),
                listOf(R.string.clima_v_nublado_c1, R.string.clima_v_nublado_c2)
            )
        }
    }
}
