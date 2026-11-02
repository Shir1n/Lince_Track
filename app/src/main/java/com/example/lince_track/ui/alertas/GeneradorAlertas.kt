package com.example.lince_track.ui.alertas

import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.RespuestaServicio
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.formatFechaCorta
import com.example.lince_track.data.formatHora
import java.time.LocalDate
import java.time.LocalDateTime

enum class TipoAlerta {
    ATRASO,
    SERVICIO
}

enum class ClaseAlerta(val etiqueta: String, val tipo: TipoAlerta) {
    CHECKIN_ATRASADO("Check-in atrasado", TipoAlerta.ATRASO),
    POR_RESPONDER("Por responder", TipoAlerta.SERVICIO),
    EN_RUTA("En ruta", TipoAlerta.SERVICIO),
    CANCELADO("Cancelado", TipoAlerta.SERVICIO)
}

data class Alerta(
    val servicioId: String,
    val clase: ClaseAlerta,
    val titulo: String,
    val descripcion: String,
    val tiempo: String
)

/**
 * Alertas calculadas desde los servicios y lo que hizo la persona. Solo se ven en esta pantalla:
 * no son notificaciones del teléfono. Sin dependencias de Android ni Firebase.
 */
object GeneradorAlertas {

    /** Primero las atrasadas; dentro de cada grupo, por fecha y hora. */
    fun generar(
        servicios: List<Servicio>,
        checkins: Map<String, CheckinServicio>,
        respuestas: Map<String, RespuestaServicio>,
        ahora: LocalDateTime = LocalDateTime.now()
    ): List<Alerta> = servicios
        .mapNotNull { servicio ->
            claseDe(servicio, checkins[servicio.id], respuestas[servicio.id], ahora)?.let { servicio to it }
        }
        .sortedWith(compareBy({ it.second.tipo != TipoAlerta.ATRASO }, { it.first.fecha }, { it.first.horaInicio }))
        .map { (servicio, clase) -> aAlerta(servicio, clase, ahora.toLocalDate()) }

    private fun claseDe(
        servicio: Servicio,
        checkin: CheckinServicio?,
        respuesta: RespuestaServicio?,
        ahora: LocalDateTime
    ): ClaseAlerta? {
        val hoy = ahora.toLocalDate()
        return when (servicio.estado) {
            EstadoServicio.PENDIENTE ->
                if (!servicio.fecha.isBefore(hoy)) ClaseAlerta.POR_RESPONDER else null
            // Un rechazo propio no es una novedad para quien lo hizo: solo se avisa lo que viene de la API.
            EstadoServicio.RECHAZADO ->
                if (!servicio.fecha.isBefore(hoy) && respuesta?.estado != EstadoServicio.RECHAZADO) {
                    ClaseAlerta.CANCELADO
                } else {
                    null
                }
            EstadoServicio.ACEPTADO -> when {
                checkin != null -> if (checkin.finalizado) null else ClaseAlerta.EN_RUTA
                servicio.fecha == hoy && servicio.horaInicio.isBefore(ahora.toLocalTime()) ->
                    ClaseAlerta.CHECKIN_ATRASADO
                else -> null
            }
        }
    }

    private fun aAlerta(servicio: Servicio, clase: ClaseAlerta, hoy: LocalDate): Alerta {
        val descripcion = when (clase) {
            ClaseAlerta.CHECKIN_ATRASADO -> "Ya pasó la hora de inicio y aún no registras el check-in."
            ClaseAlerta.POR_RESPONDER -> "Confirma o rechaza esta asignación."
            ClaseAlerta.EN_RUTA -> "Recuerda registrar el check-out al terminar."
            ClaseAlerta.CANCELADO -> "Este servicio fue rechazado o cancelado en Southbound."
        }
        val dia = if (servicio.fecha == hoy) "Hoy" else formatFechaCorta(servicio.fecha)
        return Alerta(
            servicioId = servicio.id,
            clase = clase,
            titulo = servicio.titulo,
            descripcion = descripcion,
            tiempo = "$dia • ${formatHora(servicio.horaInicio)}"
        )
    }
}
