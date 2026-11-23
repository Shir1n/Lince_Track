package com.example.lince_track.data

import com.example.lince_track.util.LinceLogger
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Foto de perfil en Firebase Storage (`perfil/{uid}/foto.jpg`). Las reglas de Storage dejan verla a cualquier cuenta
 * con rol y subirla o borrarla solo a su dueño. Al aprobarse una solicitud de eliminación de datos, la Cloud Function
 * borra la carpeta.
 */
object FotoPerfilRepository {

    private const val TAG = "FOTO"
    /** Lado mayor de la foto guardada: basta para el avatar y pesa unas decenas de KB. */
    const val LADO_MAXIMO = 512
    const val CALIDAD_JPEG = 85

    /** Firebase Storage real; los tests lo reemplazan por uno simulado con MockK. */
    internal val storageReemplazable = Reemplazable { FirebaseStorage.getInstance() }
    private val storage get() = storageReemplazable.valor

    fun ruta(uid: String) = "perfil/$uid/foto.jpg"

    /** Medidas para achicar una foto de [ancho] × [alto] sin deformarla (nunca la agranda). */
    fun medidas(ancho: Int, alto: Int, ladoMaximo: Int = LADO_MAXIMO): Pair<Int, Int> {
        val mayor = max(ancho, alto)
        if (mayor <= ladoMaximo || mayor <= 0) return ancho to alto
        val factor = ladoMaximo.toDouble() / mayor
        return max(1, (ancho * factor).roundToInt()) to max(1, (alto * factor).roundToInt())
    }

    /** Enlace de descarga de la foto, o null si la persona no ha subido una. */
    suspend fun url(uid: String): String? = try {
        storage.reference.child(ruta(uid)).downloadUrl.await().toString()
    } catch (e: CancellationException) {
        throw e
    } catch (e: StorageException) {
        if (e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) {
            LinceLogger.w(TAG, "No se pudo leer la foto de perfil (${e.errorCode})")
        }
        null
    } catch (e: Exception) {
        LinceLogger.w(TAG, "No se pudo leer la foto de perfil: ${e.message}")
        null
    }

    /** Sube [jpeg] (ya achicada y comprimida) y devuelve el enlace nuevo. */
    suspend fun subir(uid: String, jpeg: ByteArray): Result<String> = try {
        val referencia = storage.reference.child(ruta(uid))
        val metadatos = StorageMetadata.Builder().setContentType("image/jpeg").build()
        referencia.putBytes(jpeg, metadatos).await()
        LinceLogger.i(TAG, "Foto de perfil actualizada (${jpeg.size / 1024} KB)")
        Result.success(referencia.downloadUrl.await().toString())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LinceLogger.e(TAG, "No se pudo subir la foto de perfil", e)
        Result.failure(e)
    }

    suspend fun borrar(uid: String): Result<Unit> = try {
        storage.reference.child(ruta(uid)).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LinceLogger.w(TAG, "No se pudo borrar la foto de perfil: ${e.message}")
        Result.failure(e)
    }
}
