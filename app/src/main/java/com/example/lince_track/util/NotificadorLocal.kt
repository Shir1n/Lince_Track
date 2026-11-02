package com.example.lince_track.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.lince_track.R
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.AvisoLocal
import com.example.lince_track.data.CambiosServicios
import com.example.lince_track.data.IncidenciaServicio
import com.example.lince_track.data.PerfilUsuario
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.ServicioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Notificaciones del teléfono mientras la app sigue viva (abierta o en segundo plano): salen de
 * lo que ya escucha ServicioRepository. Con la app cerrada no llegan; eso requiere un servidor
 * que envíe notificaciones, disponible al pasar Firebase al plan Blaze.
 */
object NotificadorLocal {

    private data class Estado(
        val listos: Boolean,
        val servicios: List<Servicio>,
        val incidencias: List<IncidenciaServicio>,
        val perfil: PerfilUsuario?
    )

    private const val CANAL = "servicios"
    private var iniciado = false

    fun iniciar(context: Context) {
        if (iniciado) return
        iniciado = true
        val app = context.applicationContext
        crearCanal(app)
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            var anterior: List<Servicio>? = null
            var incidenciasAnteriores: List<IncidenciaServicio> = emptyList()
            combine(
                ServicioRepository.datosListos,
                ServicioRepository.servicios,
                ServicioRepository.incidencias,
                AuthRepository.perfilActual
            ) { listos, servicios, incidencias, perfil -> Estado(listos, servicios, incidencias, perfil) }
                .collect { (listos, servicios, incidencias, perfil) ->
                    if (!listos || perfil == null) {
                        anterior = null
                        return@collect
                    }
                    // La primera lista completa es la base: lo que ya estaba no es novedad.
                    anterior?.let { previa ->
                        CambiosServicios.detectar(previa, servicios, perfil.rol).forEach { mostrar(app, it) }
                        CambiosServicios.incidencias(incidenciasAnteriores, incidencias, perfil.rol)
                            .forEach { mostrar(app, it) }
                    }
                    anterior = servicios
                    incidenciasAnteriores = incidencias
                }
        }
    }

    private fun crearCanal(context: Context) {
        val canal = NotificationChannel(CANAL, "Servicios", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Asignaciones, confirmaciones, rechazos e incidencias de servicios"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private fun mostrar(context: Context, aviso: AvisoLocal) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            LinceLogger.w("AVISOS", "Sin permiso de notificaciones: ${aviso.titulo}")
            return
        }
        val abrirApp = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notificacion = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(aviso.titulo)
            .setContentText(aviso.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(aviso.texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify("${aviso.servicioId}|${aviso.titulo}".hashCode(), notificacion)
    }
}
