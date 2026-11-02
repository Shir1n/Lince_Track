package com.example.lince_track.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.lince_track.data.Ubicacion
import com.example.lince_track.data.await
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

private const val ESPERA_UBICACION_MS = 5_000L

val PERMISOS_UBICACION = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

fun tienePermisoUbicacion(context: Context): Boolean =
    PERMISOS_UBICACION.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/**
 * Posición actual, solo si es real: sin permiso, sin señal o tras [ESPERA_UBICACION_MS] devuelve
 * null, para que quien la pide pueda seguir sin ella.
 */
@SuppressLint("MissingPermission")
suspend fun ubicacionActual(context: Context): Ubicacion? {
    if (!tienePermisoUbicacion(context)) return null
    val cancelacion = CancellationTokenSource()
    return try {
        withTimeoutOrNull(ESPERA_UBICACION_MS) {
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelacion.token)
                .await()
                ?.let { Ubicacion(it.latitude, it.longitude) }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LinceLogger.e("GPS", "No se pudo obtener la ubicación", e)
        null
    } finally {
        cancelacion.cancel()
    }
}

/** Abre el punto en la app de mapas del teléfono. */
fun abrirEnMapa(context: Context, ubicacion: Ubicacion) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ubicacion.enlaceMapa)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No se encontró una aplicación de mapas", Toast.LENGTH_SHORT).show()
    }
}
