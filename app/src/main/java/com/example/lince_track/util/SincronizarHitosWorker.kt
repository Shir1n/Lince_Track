package com.example.lince_track.util

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.lince_track.R
import com.example.lince_track.data.AvisoLocal
import com.example.lince_track.data.ResultadoSincronizacion
import com.example.lince_track.data.SincronizadorHitos
import com.example.lince_track.data.local.BaseLocal
import java.util.concurrent.TimeUnit

/**
 * Envía los check-in y check-out guardados sin conexión. WorkManager lo corre solo cuando hay red, aunque la app
 * esté cerrada, y lo repite con espera creciente mientras el backend no responda.
 */
class SincronizarHitosWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = BaseLocal.iniciar(applicationContext)
        val resultado = SincronizadorHitos(dao).sincronizar()
        LinceLogger.i(
            "CHECKIN",
            "Sincronización: ${resultado.enviados} enviados, ${resultado.rechazados} rechazados, " +
                "${resultado.descartados} descartados, ${resultado.pendientes} pendientes"
        )
        aviso(resultado)?.let { NotificadorLocal.avisar(applicationContext, it) }
        return if (resultado.reintentar) Result.retry() else Result.success()
    }

    private fun aviso(resultado: ResultadoSincronizacion): AvisoLocal? {
        val total = resultado.enviados + resultado.rechazados
        val servicio = resultado.servicios.lastOrNull() ?: return null
        if (total == 0) return null
        val texto = if (resultado.rechazados > 0) {
            UiText.StringResource(R.string.sync_texto_rechazos, total, resultado.rechazados)
        } else {
            UiText.StringResource(R.string.sync_texto, total)
        }
        return AvisoLocal(servicio, UiText.StringResource(R.string.sync_titulo), texto)
    }

    companion object {
        private const val NOMBRE = "sincronizar-hitos"

        /**
         * Programa la sincronización. Reemplaza a la que esté esperando: así parte apenas haya red, sin esperar
         * el tiempo de reintento de la anterior (el backend no duplica gracias a la clave de idempotencia).
         */
        fun programar(context: Context) {
            val pedido = OneTimeWorkRequestBuilder<SincronizarHitosWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.REPLACE, pedido)
        }
    }
}
