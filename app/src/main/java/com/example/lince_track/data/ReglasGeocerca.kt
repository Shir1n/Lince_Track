package com.example.lince_track.data

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Punto de pickup o dropoff (hotel, aeropuerto) con su radio, según el backend. */
data class Geocerca(
    val id: Long,
    val nombre: String,
    val lat: Double,
    val lon: Double,
    val radioMetros: Int
)

/** Reglas del check-in que coordinación cambia en el backend (`/api/parametros`). */
data class ParametrosCheckin(
    val anticipacionMinutos: Int,
    val precisionMaximaMetros: Int
)

/** Resultado de validar un check-in. Los nombres son los mismos del backend, más [SIN_GPS_REAL]. */
enum class ResultadoCheckin {
    VERIFICADO,
    /** El pickup del servicio no tiene geocerca. */
    SIN_GEOCERCA,
    /** Solo en la app: sin GPS real (posición simulada o sin señal) no se valida. */
    SIN_GPS_REAL,
    GPS_IMPRECISO,
    FUERA_DE_ZONA,
    /** Después del pickup menos la anticipación. */
    TARDE;

    val permitido: Boolean get() = this == VERIFICADO

    companion object {
        /** Resultado que trae el backend; null si es otro (por ejemplo REGISTRADO). */
        fun desdeBackend(valor: String?): ResultadoCheckin? =
            entries.firstOrNull { it.name == valor && it != SIN_GPS_REAL }
    }
}

/** Lo que se muestra en el mapa: el resultado y los datos con que se decidió. */
data class EvaluacionCheckin(
    val resultado: ResultadoCheckin,
    val geocerca: Geocerca?,
    val distanciaMetros: Int?,
    val precisionMetros: Int?,
    val limite: LocalDateTime,
    val parametros: ParametrosCheckin
)

/**
 * Valida el check-in en la app con las mismas reglas y el mismo orden que el backend
 * (`ReglasHito`): sin geocerca → GPS impreciso → fuera de zona → tarde. El backend lo vuelve a
 * validar con su propia hora, así que esto solo evita enviar lo que de todas formas se rechaza.
 */
object ReglasGeocerca {

    /** Hora de Chile: la misma con que el backend compara el pickup. */
    val ZONA: ZoneId = ZoneId.of("America/Santiago")

    private const val RADIO_TIERRA_METROS = 6_371_000.0

    /** La geocerca cuyo nombre coincide con el texto del pickup o dropoff (sin distinguir mayúsculas). */
    fun buscar(geocercas: List<Geocerca>, lugar: String): Geocerca? {
        val buscado = lugar.trim()
        if (buscado.isEmpty()) return null
        return geocercas.firstOrNull { it.nombre.trim().equals(buscado, ignoreCase = true) }
    }

    /** Distancia en metros entre dos puntos (Haversine, igual que el backend). */
    fun distanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * RADIO_TIERRA_METROS * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Última hora válida para el check-in: pickup menos la anticipación. */
    fun limiteLlegada(inicio: LocalDateTime, anticipacionMinutos: Int): LocalDateTime =
        inicio.minusMinutes(anticipacionMinutos.toLong())

    /**
     * @param precisionMetros precisión que informó el GPS, o null si no la dio
     * @param ahora hora de Chile
     * @param inicio fecha y hora de pickup del servicio
     */
    fun evaluar(
        geocerca: Geocerca?,
        lat: Double,
        lon: Double,
        gpsReal: Boolean,
        precisionMetros: Float?,
        ahora: LocalDateTime,
        inicio: LocalDateTime,
        parametros: ParametrosCheckin
    ): EvaluacionCheckin {
        val distancia = geocerca?.let { distanciaMetros(lat, lon, it.lat, it.lon).roundToInt() }
        val precision = precisionMetros?.roundToInt()
        val limite = limiteLlegada(inicio, parametros.anticipacionMinutos)
        val resultado = when {
            geocerca == null || distancia == null -> ResultadoCheckin.SIN_GEOCERCA
            !gpsReal || precision == null -> ResultadoCheckin.SIN_GPS_REAL
            precision > parametros.precisionMaximaMetros -> ResultadoCheckin.GPS_IMPRECISO
            distancia > geocerca.radioMetros -> ResultadoCheckin.FUERA_DE_ZONA
            ahora.isAfter(limite) -> ResultadoCheckin.TARDE
            else -> ResultadoCheckin.VERIFICADO
        }
        return EvaluacionCheckin(resultado, geocerca, distancia, precision, limite, parametros)
    }

    /** Tipo de incidencia que se genera cuando el check-in no se permite. */
    fun tipoIncidencia(resultado: ResultadoCheckin): TipoIncidencia =
        if (resultado == ResultadoCheckin.TARDE) TipoIncidencia.ATRASO else TipoIncidencia.OTRO

    /** Detalle de la incidencia para Coordinación (siempre en español: lo lee la oficina). */
    fun detalleIncidencia(evaluacion: EvaluacionCheckin, lugar: String): String {
        val nombre = evaluacion.geocerca?.nombre ?: lugar
        val distancia = evaluacion.distanciaMetros?.let { "$it m" } ?: "sin dato"
        val radio = evaluacion.geocerca?.let { "${it.radioMetros} m" } ?: "sin geocerca"
        val gps = evaluacion.precisionMetros?.let { "± $it m" } ?: "sin dato"
        val hora = "%02d:%02d".format(evaluacion.limite.hour, evaluacion.limite.minute)
        val motivo = when (evaluacion.resultado) {
            ResultadoCheckin.VERIFICADO -> "verificado"
            ResultadoCheckin.SIN_GEOCERCA -> "el punto de recogida no tiene geocerca"
            ResultadoCheckin.SIN_GPS_REAL -> "sin GPS real"
            ResultadoCheckin.GPS_IMPRECISO -> "GPS impreciso"
            ResultadoCheckin.FUERA_DE_ZONA -> "fuera de la zona"
            ResultadoCheckin.TARDE -> "después de la hora límite ($hora)"
        }
        return "Check-in bloqueado: $motivo. $nombre: a $distancia (radio $radio), GPS $gps."
    }
}
