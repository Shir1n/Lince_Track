package com.example.lince_track.data.clima

import com.example.lince_track.util.LinceLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

enum class TipoClima(val label: String) {
    DESPEJADO("Despejado"),
    PARCIALMENTE_NUBLADO("Parcialmente nublado"),
    NUBLADO("Nublado"),
    NIEBLA("Niebla / Neblina"),
    LLUVIA_LIGERA("Llovizna / Lluvia ligera"),
    LLUVIA_MODERADA("Lluvia moderada"),
    LLUVIA_FUERTE("Lluvia torrencial"),
    TORMENTA("Tormenta eléctrica"),
    NIEVE("Nieve / Aguanieve"),
    VIENTO_FUERTE("Ráfagas de viento fuerte")
}

enum class NivelRiesgo(val label: String) {
    OPTIMO("Condiciones óptimas"),
    PRECAUCION("Precaución en ruta"),
    PELIGRO("Alerta meteorológica en ruta")
}

data class ClimaInfo(
    val tempC: Double,
    val weatherCode: Int,
    val windSpeedKmH: Double,
    val precipitationMm: Double,
    val tipo: TipoClima,
    val descripcionCorta: String,
    val esSimulado: Boolean = false
)

data class RecomendacionConduccion(
    val nivel: NivelRiesgo,
    val alertaCorta: String,
    val mensajePersonalizado: String,
    val consejos: List<String>
)

enum class PresetClima(val label: String, val clima: ClimaInfo) {
    REAL(
        "Tiempo Real (GPS)",
        ClimaInfo(
            tempC = 18.0,
            weatherCode = 0,
            windSpeedKmH = 10.0,
            precipitationMm = 0.0,
            tipo = TipoClima.DESPEJADO,
            descripcionCorta = "Tiempo real",
            esSimulado = false
        )
    ),
    LLUVIA_TORRENCIAL(
        "Simular: Lluvia Torrencial (11°C, 35 km/h)",
        ClimaInfo(
            tempC = 11.2,
            weatherCode = 65,
            windSpeedKmH = 34.5,
            precipitationMm = 9.8,
            tipo = TipoClima.LLUVIA_FUERTE,
            descripcionCorta = "Lluvia torrencial con calzada inundada",
            esSimulado = true
        )
    ),
    NEBLINA_DENSA(
        "Simular: Neblina Densa (7°C, visibilidad <50m)",
        ClimaInfo(
            tempC = 7.4,
            weatherCode = 45,
            windSpeedKmH = 5.2,
            precipitationMm = 0.0,
            tipo = TipoClima.NIEBLA,
            descripcionCorta = "Neblina densa en tramos de ruta",
            esSimulado = true
        )
    ),
    VIENTO_FUERTE(
        "Simular: Viento Extremo (15°C, 65 km/h)",
        ClimaInfo(
            tempC = 15.0,
            weatherCode = 3,
            windSpeedKmH = 65.0,
            precipitationMm = 0.0,
            tipo = TipoClima.VIENTO_FUERTE,
            descripcionCorta = "Ráfagas fuertes laterales",
            esSimulado = true
        )
    ),
    DESPEJADO(
        "Simular: Despejado / Seco (22°C)",
        ClimaInfo(
            tempC = 22.5,
            weatherCode = 0,
            windSpeedKmH = 12.0,
            precipitationMm = 0.0,
            tipo = TipoClima.DESPEJADO,
            descripcionCorta = "Cielo despejado y calzada seca",
            esSimulado = true
        )
    )
}

object ClimaService {

    suspend fun consultarOpenMeteo(lat: Double, lon: Double): Result<ClimaInfo> = withContext(Dispatchers.IO) {
        val urlStr = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,precipitation,weather_code,wind_speed_10m",
            lat,
            lon
        )
        LinceLogger.i("CLIMA", "Consultando Open-Meteo", "coords=($lat, $lon)")

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

    private fun describeWmo(code: Int, windKmH: Double, precipMm: Double): String {
        return when {
            windKmH >= 50.0 -> "Ráfagas fuertes (${windKmH.roundToInt()} km/h)"
            code == 0 -> "Cielo despejado"
            code in 1..2 -> "Parcialmente cubierto"
            code == 3 -> "Nublado"
            code in listOf(45, 48) -> "Baja visibilidad por niebla"
            code in listOf(51, 53, 55) -> "Llovizna intermitente"
            code in listOf(61, 80) -> "Lluvia ligera"
            code in listOf(63, 81) -> "Lluvia moderada"
            code in listOf(65, 82) -> "Precipitaciones fuertes (${precipMm} mm)"
            code in 71..77 -> "Nieve en calzada"
            code >= 95 -> "Tormenta eléctrica"
            else -> "Condiciones estándar"
        }
    }

    fun generarRecomendacion(
        clima: ClimaInfo,
        nombreConductor: String,
        destino: String
    ): RecomendacionConduccion {
        val chofer = if (nombreConductor.isBlank()) "conductor" else nombreConductor.trim()

        return when (clima.tipo) {
            TipoClima.LLUVIA_FUERTE, TipoClima.TORMENTA -> RecomendacionConduccion(
                nivel = NivelRiesgo.PELIGRO,
                alertaCorta = "Calzada muy resbaladiza y riesgo de hidroplaneo",
                mensajePersonalizado = "Atención $chofer: lluvia intensa hacia $destino. Reduzca la velocidad 25 km/h y mantenga 60 metros de distancia.",
                consejos = listOf(
                    "Distancia de frenado duplicada por asfalto mojado",
                    "Encienda luces bajas de circulación de forma permanente",
                    "Evite maniobras bruscas y charcos profundos"
                )
            )

            TipoClima.LLUVIA_MODERADA, TipoClima.LLUVIA_LIGERA -> RecomendacionConduccion(
                nivel = NivelRiesgo.PRECAUCION,
                alertaCorta = "Pavimento húmedo con adherencia reducida",
                mensajePersonalizado = "Precaución en ruta, $chofer: precipitaciones activas rumbo a $destino. Modere su marcha en curvas.",
                consejos = listOf(
                    "Aumente distancia de seguridad a 40 metros",
                    "Encienda limpiaparabrisas a velocidad adecuada",
                    "Anticipe las frenadas en cruces y semáforos"
                )
            )

            TipoClima.NIEBLA -> RecomendacionConduccion(
                nivel = NivelRiesgo.PELIGRO,
                alertaCorta = "Visibilidad crítica en carretera",
                mensajePersonalizado = "Alerta $chofer: bancos de niebla en trayecto hacia $destino. No use luces altas para evitar encandilamiento.",
                consejos = listOf(
                    "Encienda neblineros delanteros y traseros",
                    "Prohibido adelantar en curvas y zonas de baja visibilidad",
                    "Reduzca velocidad máxima a 50 km/h o según visibilidad"
                )
            )

            TipoClima.VIENTO_FUERTE -> RecomendacionConduccion(
                nivel = NivelRiesgo.PRECAUCION,
                alertaCorta = "Ráfagas laterales de viento (${clima.windSpeedKmH.roundToInt()} km/h)",
                mensajePersonalizado = "Precaución $chofer: viento fuerte en tramos abiertos camino a $destino. Sujete el volante firme con ambas manos.",
                consejos = listOf(
                    "Cuidado especial al salir de túneles o cruzar puentes",
                    "Mantenga mayor distancia lateral con camiones y buses",
                    "Evite cargas sueltas en portaequipajes"
                )
            )

            TipoClima.NIEVE -> RecomendacionConduccion(
                nivel = NivelRiesgo.PELIGRO,
                alertaCorta = "Hielo o nieve en calzada",
                mensajePersonalizado = "Peligro $chofer: calzada congelada hacia $destino. Porte cadenas y circule a marcha suave.",
                consejos = listOf(
                    "Velocidad máxima precautoria 30 km/h",
                    "Acelere y frene progresivamente usando enganche de motor",
                    "Verifique anticongelante y líquido limpiaparabrisas"
                )
            )

            TipoClima.DESPEJADO, TipoClima.PARCIALMENTE_NUBLADO, TipoClima.NUBLADO -> RecomendacionConduccion(
                nivel = NivelRiesgo.OPTIMO,
                alertaCorta = "Condiciones de ruta óptimas y secas",
                mensajePersonalizado = "Buen viaje, $chofer: calzada seca y visibilidad despejada hacia $destino.",
                consejos = listOf(
                    "Respete las velocidades reglamentarias de la vía",
                    "Utilice parasol o gafas ante reflejos solares",
                    "Mantenga hidratación adecuada en trayectos largos"
                )
            )
        }
    }
}
