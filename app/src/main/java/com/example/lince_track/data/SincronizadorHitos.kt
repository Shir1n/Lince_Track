package com.example.lince_track.data

import com.example.lince_track.data.local.HitoPendiente
import com.example.lince_track.data.local.LinceDao
import com.example.lince_track.data.remote.BloqueoApiException
import com.example.lince_track.util.LinceLogger
import kotlinx.coroutines.CancellationException

/** Lo que logró una pasada de sincronización. */
data class ResultadoSincronizacion(
    /** Check-in verificados y check-out registrados. */
    val enviados: Int = 0,
    /** Check-in que el backend rechazó (se creó la incidencia). */
    val rechazados: Int = 0,
    /** Hitos que el backend no aceptó por una razón que no cambia al reintentar (servicio de otra persona, etc.). */
    val descartados: Int = 0,
    /** Hitos que siguen en la cola porque todavía no hay conexión con el backend. */
    val pendientes: Int = 0,
    /** Servicios cuyos hitos llegaron al backend (enviados o rechazados), para el aviso. */
    val servicios: List<String> = emptyList()
) {
    val reintentar: Boolean get() = pendientes > 0
}

/**
 * Envía al backend los check-in y check-out guardados sin conexión, en el orden en que se hicieron y con su clave
 * de idempotencia. Lo usa [com.example.lince_track.util.SincronizarHitosWorker]; no depende de la pantalla, así que
 * funciona con la app cerrada. Las escrituras en Firestore y la incidencia se reciben por el constructor para
 * probarlo en la JVM.
 */
class SincronizadorHitos(
    private val dao: LinceDao,
    private val hitos: HitoRepository = HitoRepository(dao = { dao }),
    private val guardarCheckIn: suspend (HitoPendiente) -> Unit = { h ->
        CheckinRepository.registrarCheckIn(
            h.uid, Rol.desde(h.rol) ?: Rol.CONDUCTOR, h.servicioId, h.lat, h.lon, h.distanciaMetros, gpsReal = true
        )
    },
    private val guardarCheckOut: suspend (HitoPendiente) -> Unit = { h ->
        CheckinRepository.registrarCheckOut(h.uid, h.servicioId, h.lat, h.lon)
    },
    private val reportarRechazo: suspend (HitoPendiente, ResultadoCheckin, HitoRegistrado) -> Unit = { h, resultado, hito ->
        IncidenciaRepository.reportar(
            uid = h.uid,
            rol = Rol.desde(h.rol) ?: Rol.CONDUCTOR,
            nombre = h.nombre,
            servicioId = h.servicioId,
            titulo = h.titulo,
            fecha = h.fecha,
            tipo = ReglasGeocerca.tipoIncidencia(resultado),
            detalle = ReglasGeocerca.detalleRechazoDiferido(
                resultado, h.lugar, hito.distanciaMetros ?: h.distanciaMetros, hito.radioMetros, h.precisionMetros
            ),
            ubicacion = Ubicacion(h.lat, h.lon)
        )
    }
) {

    suspend fun sincronizar(): ResultadoSincronizacion {
        val cola = dao.hitosPendientes()
        var resultado = ResultadoSincronizacion()
        // Si un check-in se rechazó, su check-out (hecho sin conexión después) ya no corresponde.
        val checkInRechazados = mutableSetOf<String>()

        for ((indice, hito) in cola.withIndex()) {
            if (!hito.checkIn && hito.servicioId in checkInRechazados) {
                dao.borrarHito(hito.clave)
                resultado = resultado.copy(descartados = resultado.descartados + 1)
                continue
            }
            val respuesta = hitos.registrar(
                hito.servicioId, hito.checkIn, hito.lat, hito.lon, hito.precisionMetros, hito.clave
            )
            val error = respuesta.exceptionOrNull()
            if (error != null) {
                if ((error as? BloqueoApiException)?.esSinConexion != false) {
                    // Se corta aquí para respetar el orden: el resto espera al próximo intento.
                    dao.marcarIntento(hito.clave, error.message)
                    return resultado.copy(pendientes = cola.size - indice)
                }
                LinceLogger.w("CHECKIN", "El backend no aceptó un hito guardado sin conexión: ${error.message}")
                dao.borrarHito(hito.clave)
                if (hito.checkIn) checkInRechazados += hito.servicioId
                resultado = resultado.copy(descartados = resultado.descartados + 1)
                continue
            }

            val registrado = respuesta.getOrThrow()
            val verificado = !hito.checkIn || registrado.resultado == ResultadoCheckin.VERIFICADO
            guardarEnFirestore {
                when {
                    !hito.checkIn -> guardarCheckOut(hito)
                    verificado -> guardarCheckIn(hito)
                    else -> reportarRechazo(hito, registrado.resultado ?: ResultadoCheckin.SIN_GEOCERCA, registrado)
                }
            }
            dao.borrarHito(hito.clave)
            val servicios = (resultado.servicios + hito.servicioId).distinct()
            resultado = if (verificado) {
                resultado.copy(enviados = resultado.enviados + 1, servicios = servicios)
            } else {
                checkInRechazados += hito.servicioId
                resultado.copy(rechazados = resultado.rechazados + 1, servicios = servicios)
            }
        }
        return resultado
    }

    /** Firestore guarda sin conexión y envía después; si falla (otra sesión, permisos), el hito ya está en el backend. */
    private suspend fun guardarEnFirestore(accion: suspend () -> Unit) {
        try {
            accion()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LinceLogger.w("CHECKIN", "Hito sincronizado, pero no se pudo guardar en Firestore: ${e.message}")
        }
    }
}
