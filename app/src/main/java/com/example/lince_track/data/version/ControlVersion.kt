package com.example.lince_track.data.version

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.lince_track.BuildConfig
import com.example.lince_track.util.LinceLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

private val Context.versionDataStore by preferencesDataStore(name = "version_prefs")

@Serializable
data class InfoVersion(
    val versionCode: Int = 1,
    val versionName: String = "1.0",
    val autorUltimoCambio: String = "",
    val apkUrl: String = DEFAULT_APK_URL,
    val novedades: List<String> = emptyList()
) {
    companion object {
        const val DEFAULT_APK_URL =
            "https://github.com/Shir1n/Lince_Track/releases/latest/download/lince-track.apk"
        const val REMOTE_VERSION_URL =
            "https://raw.githubusercontent.com/Shir1n/Lince_Track/master/version.json"
    }
}

sealed interface EstadoAvisoVersion {
    data object Ninguno : EstadoAvisoVersion
    data class RecienActualizada(val version: InfoVersion) : EstadoAvisoVersion
    data class Desactualizada(val local: InfoVersion, val remota: InfoVersion) : EstadoAvisoVersion
}

/**
 * Reglas puras de comparación y lectura de versión, sin dependencias de Android.
 */
object ReglasVersion {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun decodificarJson(json: String): InfoVersion? {
        if (json.isBlank()) return null
        return runCatching {
            jsonParser.decodeFromString<InfoVersion>(json)
        }.getOrNull()
    }

    fun decodificarBase64(b64: String, fallbackCode: Int, fallbackName: String): InfoVersion {
        val decodificado = runCatching {
            val bytes = Base64.getDecoder().decode(b64)
            String(bytes, Charsets.UTF_8)
        }.getOrNull().orEmpty()
        return decodificarJson(decodificado) ?: InfoVersion(
            versionCode = fallbackCode,
            versionName = fallbackName
        )
    }

    /**
     * Evalúa qué aviso mostrar al iniciar la app:
     * 1. Si los avisos automáticos están desactivados -> [EstadoAvisoVersion.Ninguno].
     * 2. Si `master` tiene una versión mayor a la instalada -> [EstadoAvisoVersion.Desactualizada].
     * 3. Si la versión instalada es mayor a la última vista en el teléfono -> [EstadoAvisoVersion.RecienActualizada].
     */
    fun evaluar(
        local: InfoVersion,
        ultimaVersionVista: Int,
        remota: InfoVersion?,
        avisosActivos: Boolean = true
    ): EstadoAvisoVersion {
        if (!avisosActivos) return EstadoAvisoVersion.Ninguno
        if (remota != null && remota.versionCode > local.versionCode) {
            return EstadoAvisoVersion.Desactualizada(local = local, remota = remota)
        }
        if (local.versionCode > ultimaVersionVista) {
            return EstadoAvisoVersion.RecienActualizada(version = local)
        }
        return EstadoAvisoVersion.Ninguno
    }
}

object ControlVersionRepository {

    private val ULTIMA_VERSION_VISTA_KEY = intPreferencesKey("ultima_version_vista")
    private val AVISOS_VERSION_ACTIVOS_KEY = booleanPreferencesKey("avisos_version_activos")

    val versionLocal: InfoVersion by lazy {
        ReglasVersion.decodificarBase64(
            b64 = BuildConfig.LOCAL_VERSION_JSON_B64,
            fallbackCode = BuildConfig.VERSION_CODE,
            fallbackName = BuildConfig.VERSION_NAME
        )
    }

    fun avisosActivosFlow(context: Context): Flow<Boolean> =
        context.versionDataStore.data.map { prefs ->
            prefs[AVISOS_VERSION_ACTIVOS_KEY] ?: true
        }

    suspend fun setAvisosActivos(context: Context, activos: Boolean) {
        context.versionDataStore.edit { prefs ->
            prefs[AVISOS_VERSION_ACTIVOS_KEY] = activos
        }
    }

    suspend fun marcarVersionVista(context: Context, versionCode: Int = versionLocal.versionCode) {
        context.versionDataStore.edit { prefs ->
            prefs[ULTIMA_VERSION_VISTA_KEY] = versionCode
        }
    }

    suspend fun evaluarAlIniciar(context: Context): EstadoAvisoVersion {
        val prefs = context.versionDataStore.data.first()
        val avisosActivos = prefs[AVISOS_VERSION_ACTIVOS_KEY] ?: true
        if (!avisosActivos) return EstadoAvisoVersion.Ninguno

        val ultimaVista = prefs[ULTIMA_VERSION_VISTA_KEY] ?: 0
        val remota = consultarVersionRemota()
        return ReglasVersion.evaluar(
            local = versionLocal,
            ultimaVersionVista = ultimaVista,
            remota = remota,
            avisosActivos = avisosActivos
        )
    }

    suspend fun consultarVersionRemota(): InfoVersion? = withContext(Dispatchers.IO) {
        runCatching {
            val cacheBuster = System.currentTimeMillis() / 60_000L
            val url = URL("${InfoVersion.REMOTE_VERSION_URL}?t=$cacheBuster")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3500
                readTimeout = 3500
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache")
            }
            try {
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    ReglasVersion.decodificarJson(body)?.also {
                        LinceLogger.i(
                            "VERSION",
                            "Remota v${it.versionName} (${it.versionCode}) | Local v${versionLocal.versionName} (${versionLocal.versionCode})"
                        )
                    }
                } else {
                    LinceLogger.w("VERSION", "Consulta remota devolvió HTTP ${conn.responseCode}")
                    null
                }
            } finally {
                conn.disconnect()
            }
        }.onFailure { e ->
            LinceLogger.w("VERSION", "Sin conexión a version.json remoto: ${e.message}")
        }.getOrNull()
    }
}
