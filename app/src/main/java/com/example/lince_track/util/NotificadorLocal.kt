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
import com.example.lince_track.ui.theme.enIdiomaDeLaApp
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
 * lo que ya escucha ServicioRepository. Con la app cerrada las envían las Cloud Functions
 * (`functions/`); ambas usan la etiqueta "servicio|<id>", así que si llegan las dos, una reemplaza a la otra.
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
        crearCanal(app.enIdiomaDeLaApp())
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

    /** Aviso que no sale de comparar servicios (por ejemplo, la sincronización de check-ins sin conexión). */
    fun avisar(context: Context, aviso: AvisoLocal) {
        crearCanal(context)
        mostrar(context, aviso)
    }

    /** Misma etiqueta que usan las Cloud Functions (`android.notification.tag`). */
    fun etiqueta(servicioId: String): String = "servicio|$servicioId"

    private fun crearCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL,
            context.getString(R.string.canal_servicios),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.canal_servicios_desc)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private fun mostrar(context: Context, aviso: AvisoLocal) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            LinceLogger.w("AVISOS", "Sin permiso de notificaciones: ${aviso.titulo.asString(context)}")
            return
        }
        // Se traducen aquí, al idioma elegido en la app (no al del contexto de la aplicación, que puede ser el
        // del teléfono o el de cuando se abrió la app). El canal se actualiza para que su nombre siga ese idioma.
        val traducido = context.enIdiomaDeLaApp()
        crearCanal(traducido)
        val titulo = aviso.titulo.asString(traducido)
        val texto = aviso.texto.asString(traducido)
        val abrirApp = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            it.putExtra(AperturaNotificacion.EXTRA_SERVICIO, aviso.servicioId)
            PendingIntent.getActivity(
                context,
                aviso.servicioId.hashCode(),
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val notificacion = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion_small_icon)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(etiqueta(aviso.servicioId), 0, notificacion)
    }
}
