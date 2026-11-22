package com.example.lince_track.data.remote

import com.example.lince_track.BuildConfig
import com.example.lince_track.data.remote.dto.ErrorRespuestaDto
import com.example.lince_track.data.remote.dto.LinceJson
import com.example.lince_track.util.LinceLogger
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit para el backend propio de Lince (bloqueos y parámetros).
 *
 * Utiliza BuildConfig.LINCE_BACKEND_URL (leído desde local.properties) y adjunta
 * automáticamente el token de Firebase Authentication en cada petición.
 */
object BloqueoApiClient {

    /** Proveedor de token reemplazable para pruebas unitarias. */
    var tokenProvider: () -> String? = {
        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                Tasks.await(user.getIdToken(false), 10, TimeUnit.SECONDS)?.token
            } else {
                null
            }
        } catch (e: Exception) {
            LinceLogger.w("API_BLOQUEOS", "No se pudo obtener el token de Firebase: ${e.message}")
            null
        }
    }

    val api: BloqueoApi by lazy { crear() }

    fun crear(baseUrl: String = BuildConfig.LINCE_BACKEND_URL): BloqueoApi {
        val http = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                    .header("Accept", "application/json")

                val token = tokenProvider()
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }

                val request = requestBuilder.build()
                val inicio = System.currentTimeMillis()
                val respuesta = chain.proceed(request)

                LinceLogger.i(
                    tag = "API_BLOQUEOS",
                    message = "${request.method} ${request.url.encodedPath} → ${respuesta.code}",
                    details = "query=${request.url.encodedQuery ?: "-"} | ${System.currentTimeMillis() - inicio}ms"
                )
                respuesta
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(http)
            .addConverterFactory(LinceJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BloqueoApi::class.java)
    }
}

/**
 * Excepción de la API de bloqueos con detalles de código de error y mensaje amigable.
 */
class BloqueoApiException(
    mensaje: String,
    val codigoHttp: Int? = null,
    val codigoApi: String? = null,
    val requestId: String? = null,
    causa: Throwable? = null
) : Exception(mensaje, causa) {

    /** Mensaje para la pantalla: agrega el `requestId` del backend, si vino, para poder buscar el error en sus logs. */
    val mensajeConRequestId: String
        get() = if (requestId.isNullOrBlank()) message.orEmpty() else "${message.orEmpty()} (ID: $requestId)"
}

/** Mensaje de un error de bloqueo listo para mostrar, con el `requestId` si es un error del backend. */
fun Throwable.mensajeBloqueo(porDefecto: String): String =
    (this as? BloqueoApiException)?.mensajeConRequestId?.ifBlank { null } ?: message?.ifBlank { null } ?: porDefecto

/**
 * Traduce cualquier excepción HTTP o de red a un BloqueoApiException con mensaje amigable.
 */
fun Throwable.aBloqueoApiException(): BloqueoApiException = when (this) {
    is BloqueoApiException -> this
    is HttpException -> {
        val cuerpo = runCatching { response()?.errorBody()?.string() }.getOrNull()
        val error = cuerpo?.let {
            runCatching { LinceJson.decodeFromString<ErrorRespuestaDto>(it) }.getOrNull()
        }?.error

        val codigo = error?.code.orEmpty()
        val backendMessage = error?.message.orEmpty()

        val mensaje = when {
            // Los días de anticipación los decide coordinación: el mensaje del backend ya trae la fecha permitida.
            codigo == "SIN_ANTICIPACION" -> backendMessage.ifBlank { "Ese día no cumple la anticipación mínima para bloquear." }
            codigo == "DIA_CON_SERVICIO" -> backendMessage.ifBlank { "Ese día tienes un servicio asignado." }
            codigo == "YA_BLOQUEADO" -> backendMessage.ifBlank { "Ese día ya está bloqueado." }
            codigo == "MOTIVO_OBLIGATORIO" -> backendMessage.ifBlank { "Indica el motivo del bloqueo." }
            codigo == "FORBIDDEN" || code() == 403 -> backendMessage.ifBlank { "Tu cuenta no tiene permisos para bloquear días." }
            code() == 404 -> "No se encontró el bloqueo solicitado."
            backendMessage.isNotBlank() -> backendMessage
            else -> "Error ${code()} del servidor."
        }
        val requestId = error?.requestId?.ifBlank { null }
        LinceLogger.w("API_BLOQUEOS", "Error ${code()} ${codigo.ifBlank { "-" }}: $mensaje (requestId=${requestId ?: "-"})")
        BloqueoApiException(mensaje, code(), codigo.ifBlank { null }, requestId, this)
    }
    is IOException -> BloqueoApiException("Sin conexión con el servidor. Revisa tu red.", causa = this)
    else -> BloqueoApiException(message ?: "Error inesperado al gestionar el bloqueo.", causa = this)
}
