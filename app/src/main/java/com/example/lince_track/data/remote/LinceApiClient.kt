package com.example.lince_track.data.remote

import com.example.lince_track.BuildConfig
import com.example.lince_track.data.remote.dto.ErrorRespuestaDto
import com.example.lince_track.data.remote.dto.LinceJson
import com.example.lince_track.util.LinceLogger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Crea el cliente Retrofit de la API de Southbound.
 *
 * La clave se lee de BuildConfig.LINCE_API_KEY, que Gradle toma de local.properties
 * (archivo que no se sube a git). Nunca escribir la clave en el código.
 */
object LinceApiClient {

    /** true si hay una clave configurada en local.properties. */
    val estaConfigurada: Boolean
        get() = BuildConfig.LINCE_API_KEY.isNotBlank()

    val api: LinceApi by lazy { crear() }

    private fun crear(): LinceApi {
        val http = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val peticion = chain.request().newBuilder()
                    .header("X-API-Key", BuildConfig.LINCE_API_KEY)
                    .header("Accept", "application/json")
                    .build()
                val inicio = System.currentTimeMillis()
                val respuesta = chain.proceed(peticion)
                // Se registra la ruta y el código, nunca la clave.
                LinceLogger.i(
                    tag = "API",
                    message = "${peticion.method} ${peticion.url.encodedPath} → ${respuesta.code}",
                    details = "query=${peticion.url.encodedQuery ?: "-"} | ${System.currentTimeMillis() - inicio}ms"
                )
                respuesta
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.LINCE_API_BASE_URL)
            .client(http)
            .addConverterFactory(LinceJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LinceApi::class.java)
    }
}

/**
 * Error de la API traducido a un mensaje para mostrar en pantalla.
 *
 * @property codigoHttp código HTTP (null si no hubo respuesta, por ejemplo sin internet).
 * @property codigoApi código del formato Error de la API (UNAUTHORIZED, RATE_LIMITED...).
 * @property reintentarEnSegundos valor del header Retry-After en respuestas 429.
 */
class LinceApiException(
    mensaje: String,
    val codigoHttp: Int? = null,
    val codigoApi: String? = null,
    val requestId: String? = null,
    val reintentarEnSegundos: Long? = null,
    causa: Throwable? = null
) : Exception(mensaje, causa)

/** Convierte cualquier error de red o de la API en un LinceApiException con mensaje legible. */
fun Throwable.aLinceApiException(): LinceApiException = when (this) {
    is LinceApiException -> this
    is HttpException -> {
        val cuerpo = runCatching { response()?.errorBody()?.string() }.getOrNull()
        val error = cuerpo?.let { runCatching { LinceJson.decodeFromString<ErrorRespuestaDto>(it) }.getOrNull() }?.error
        val reintento = response()?.headers()?.get("Retry-After")?.toLongOrNull()
        val mensaje = when (code()) {
            401 -> if (error?.code == "KEY_EXPIRED") {
                "La clave de la API expiró. Pide una nueva al docente."
            } else {
                "La clave de la API no es válida. Revisa LINCE_API_KEY en local.properties."
            }
            400 -> "Solicitud inválida a la API: ${error?.message ?: "parámetros incorrectos"}"
            404 -> "No se encontró el recurso en la API."
            429 -> "Demasiadas solicitudes a la API. Intenta de nuevo en ${reintento ?: 60} segundos."
            503 -> "La base de datos de la API no está disponible en este momento."
            else -> "Error ${code()} de la API: ${error?.message ?: message()}"
        }
        LinceApiException(mensaje, code(), error?.code, error?.requestId, reintento, this)
    }
    is IOException -> LinceApiException("Sin conexión con la API de Southbound. Revisa tu internet.", causa = this)
    else -> LinceApiException(message ?: "Error inesperado al consultar la API.", causa = this)
}
