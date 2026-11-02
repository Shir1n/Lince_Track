package com.example.lince_track.ui.admin

import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.RespuestaServicio
import com.example.lince_track.data.Servicio

enum class FiltroPanel(val etiqueta: String, val estado: EstadoServicio?) {
    TODOS("Todos", null),
    PENDIENTES("Pendientes", EstadoServicio.PENDIENTE),
    ACEPTADOS("Aceptados", EstadoServicio.ACEPTADO),
    RECHAZADOS("Rechazados", EstadoServicio.RECHAZADO)
}

data class TotalesPanel(val pendientes: Int, val aceptados: Int, val rechazados: Int)

/**
 * Un servicio tal como se muestra en el panel.
 *
 * @property motivo por qué está rechazado (null si no lo está).
 * @property observaciones lo que escribió la persona al rechazar (vacío si no hay).
 */
data class FilaPanel(
    val servicio: Servicio,
    val responsable: String,
    val motivo: String?,
    val observaciones: String
)

/** Cálculos del panel del administrador, sin dependencias de Android ni Firebase. */
object PanelAdmin {

    const val RECHAZADO_EN_API = "Rechazado en Southbound"
    private const val MOTIVO_DESCONOCIDO = "Motivo no indicado"

    fun totales(servicios: List<Servicio>) = TotalesPanel(
        pendientes = servicios.count { it.estado == EstadoServicio.PENDIENTE },
        aceptados = servicios.count { it.estado == EstadoServicio.ACEPTADO },
        rechazados = servicios.count { it.estado == EstadoServicio.RECHAZADO }
    )

    /** Filas del filtro elegido: primero los rechazados y, dentro de cada grupo, por hora. */
    fun filas(
        servicios: List<Servicio>,
        respuestas: Map<String, RespuestaServicio>,
        filtro: FiltroPanel
    ): List<FilaPanel> = servicios
        .filter { filtro.estado == null || it.estado == filtro.estado }
        .sortedWith(compareBy({ it.estado != EstadoServicio.RECHAZADO }, { it.horaInicio }))
        .map { aFila(it, respuestas[it.id]) }

    private fun aFila(servicio: Servicio, respuesta: RespuestaServicio?): FilaPanel {
        // El servicio se muestra rechazado aunque la persona lo haya aceptado si la API lo canceló.
        val rechazoPropio = respuesta?.takeIf { it.estado == EstadoServicio.RECHAZADO }
        val motivo = when {
            servicio.estado != EstadoServicio.RECHAZADO -> null
            rechazoPropio == null -> RECHAZADO_EN_API
            else -> rechazoPropio.motivo?.etiqueta ?: MOTIVO_DESCONOCIDO
        }
        return FilaPanel(
            servicio = servicio,
            responsable = if (servicio.tipo == "GUIDED_SERVICE") servicio.guia else servicio.conductor,
            motivo = motivo,
            observaciones = if (servicio.estado == EstadoServicio.RECHAZADO) rechazoPropio?.observaciones.orEmpty() else ""
        )
    }
}
