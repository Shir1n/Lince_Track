package com.example.lince_track.ui.admin

import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.IncidenciaServicio
import com.example.lince_track.data.ReglasAsignacion
import com.example.lince_track.data.ReglasCheckin
import com.example.lince_track.data.RespuestaServicio
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.responsable
import com.example.lince_track.data.servicios.FiltroServicios
import java.time.LocalDate

enum class FiltroPanel(val etiqueta: String) {
    TODOS("Todos"),
    PENDIENTES("Pendientes"),
    ACEPTADOS("Aceptados"),
    TERMINADOS("Terminados"),
    RECHAZADOS("Rechazados")
}

/** Cada servicio cuenta una sola vez: [aceptados] son los que aún no terminan. */
data class TotalesPanel(val pendientes: Int, val aceptados: Int, val rechazados: Int, val terminados: Int = 0)

/**
 * Un servicio tal como se muestra en el panel.
 *
 * @property motivo por qué está rechazado (null si no lo está).
 * @property observaciones lo que escribió la persona al rechazar (vacío si no hay).
 * @property estadoRuta resumen del check-in o check-out (null si aún no hay check-in).
 * @property puedeReasignar true si el administrador puede elegir a otra persona.
 */
data class FilaPanel(
    val servicio: Servicio,
    val responsable: String,
    val motivo: String?,
    val observaciones: String,
    val estadoRuta: String? = null,
    val puedeReasignar: Boolean = false,
    val incidenciasAbiertas: Int = 0
)

/** Cálculos del panel del administrador, sin dependencias de Android ni Firebase. */
object PanelAdmin {

    const val RECHAZADO_EN_API = "Rechazado en Southbound"
    const val MOTIVO_DESCONOCIDO = "Motivo no indicado"

    /** Los días que se pueden elegir: los que la app tiene cargados. */
    fun rangoFechas(hoy: LocalDate): ClosedRange<LocalDate> =
        FiltroServicios.porDefecto(hoy).let { it.desde..it.hasta }

    fun totales(servicios: List<Servicio>, checkins: Map<String, CheckinServicio> = emptyMap()): TotalesPanel {
        val terminados = servicios.count { ReglasCheckin.estaTerminado(it, checkins[it.id]) }
        return TotalesPanel(
            pendientes = servicios.count { it.estado == EstadoServicio.PENDIENTE },
            aceptados = servicios.count { it.estado == EstadoServicio.ACEPTADO } - terminados,
            rechazados = servicios.count { it.estado == EstadoServicio.RECHAZADO },
            terminados = terminados
        )
    }

    /**
     * Filas del filtro elegido: primero los rechazados, al final los terminados y, dentro de cada
     * grupo, por hora.
     *
     * @param hoy con fecha, lo anterior a ella ya no se puede reasignar.
     */
    fun filas(
        servicios: List<Servicio>,
        respuestas: Map<String, RespuestaServicio>,
        filtro: FiltroPanel,
        checkins: Map<String, CheckinServicio> = emptyMap(),
        incidencias: List<IncidenciaServicio> = emptyList(),
        hoy: LocalDate? = null
    ): List<FilaPanel> = servicios
        .filter { cumple(filtro, it, checkins[it.id]) }
        .sortedWith(
            compareBy(
                { it.estado != EstadoServicio.RECHAZADO },
                { ReglasCheckin.estaTerminado(it, checkins[it.id]) },
                { it.horaInicio }
            )
        )
        .map { servicio ->
            val fila = aFila(servicio, respuestas[servicio.id], checkins[servicio.id])
            fila.copy(
                incidenciasAbiertas = incidencias.count { it.servicioId == servicio.id && !it.resuelta },
                puedeReasignar = fila.puedeReasignar && (hoy == null || !servicio.fecha.isBefore(hoy))
            )
        }

    private fun cumple(filtro: FiltroPanel, servicio: Servicio, checkin: CheckinServicio?): Boolean {
        val terminado = ReglasCheckin.estaTerminado(servicio, checkin)
        return when (filtro) {
            FiltroPanel.TODOS -> true
            FiltroPanel.PENDIENTES -> servicio.estado == EstadoServicio.PENDIENTE
            FiltroPanel.ACEPTADOS -> servicio.estado == EstadoServicio.ACEPTADO && !terminado
            FiltroPanel.TERMINADOS -> terminado
            FiltroPanel.RECHAZADOS -> servicio.estado == EstadoServicio.RECHAZADO
        }
    }

    private fun aFila(servicio: Servicio, respuesta: RespuestaServicio?, checkin: CheckinServicio?): FilaPanel {
        // El servicio se muestra rechazado aunque la persona lo haya aceptado si la API lo canceló.
        val rechazoPropio = respuesta?.takeIf { it.estado == EstadoServicio.RECHAZADO }
        val motivo = when {
            servicio.estado != EstadoServicio.RECHAZADO -> null
            rechazoPropio == null -> RECHAZADO_EN_API
            else -> rechazoPropio.motivo?.etiqueta ?: MOTIVO_DESCONOCIDO
        }
        return FilaPanel(
            servicio = servicio,
            responsable = servicio.responsable,
            motivo = motivo,
            observaciones = if (servicio.estado == EstadoServicio.RECHAZADO) rechazoPropio?.observaciones.orEmpty() else "",
            estadoRuta = checkin?.let { ReglasCheckin.resumen(it) },
            puedeReasignar = ReglasAsignacion.puedeReasignar(servicio, checkin)
        )
    }
}
