package com.example.lince_track.ui.admin

import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.IncidenciaServicio
import com.example.lince_track.data.MotivoRechazo
import com.example.lince_track.data.RespuestaServicio
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.TipoIncidencia
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Historial inventado para mostrar el resumen mientras se usan los datos de ejemplo. Solo se usa
 * en esta pantalla, siempre con un aviso visible, y no se usa si hay clave de la API. Cubre los 6
 * días anteriores a hoy: lo de hoy siempre es real.
 */
object ResumenDemostracion {

    private const val SERVICIOS_POR_DIA = 4
    private const val DIAS = 6
    private val HORAS = listOf(8, 10, 14, 16)
    private val DISTANCIAS = listOf(80, 120, 150, 60, 210, 340, 95, 130, 1_800, 45, 170, 260)

    /** Rechazo por motivo; 5 y 16 además se reasignaron a alguien que sí lo aceptó. */
    private val RECHAZOS = mapOf(
        5 to MotivoRechazo.FALLA_MECANICA,
        11 to MotivoRechazo.CRUCE_HORARIO,
        16 to MotivoRechazo.FUERZA_MAYOR,
        22 to MotivoRechazo.CRUCE_HORARIO
    )
    private val REEMPLAZOS = setOf(5, 16)
    private const val RECHAZADO_EN_API = 19
    private const val SIN_CHECK_IN = 20
    private const val SIN_CHECK_OUT = 9
    private val ATRASADOS = setOf(3, 13)
    private val INCIDENCIAS = mapOf(
        3 to TipoIncidencia.ATRASO,
        7 to TipoIncidencia.CAMINO,
        13 to TipoIncidencia.FALLA_MECANICA
    )

    fun datos(hoy: LocalDate, zona: ZoneId = ZoneId.systemDefault()): DatosResumen {
        val servicios = (0 until DIAS * SERVICIOS_POR_DIA).map { servicio(it, hoy) }
        return DatosResumen(
            servicios = servicios,
            respuestas = RECHAZOS.map { (indice, motivo) ->
                RespuestaServicio(id(indice), EstadoServicio.RECHAZADO, motivo = motivo, uid = "demo")
            },
            checkins = servicios
                .filter { it.estado == EstadoServicio.ACEPTADO && it.id != id(SIN_CHECK_IN) }
                .associate { it.id to checkin(it, zona) },
            incidencias = INCIDENCIAS.map { (indice, tipo) ->
                val servicio = servicios[indice]
                IncidenciaServicio(
                    id = "demo-incidencia-$indice",
                    servicioId = servicio.id,
                    titulo = servicio.titulo,
                    uid = "demo",
                    nombre = "Demostración",
                    tipo = tipo,
                    detalle = "",
                    creadaEn = milisegundos(servicio, 30, zona),
                    resuelta = true
                )
            },
            reemplazos = REEMPLAZOS.map { id(it) }.toSet()
        )
    }

    private fun id(indice: Int) = "demo-$indice"

    private fun servicio(indice: Int, hoy: LocalDate): Servicio {
        val hora = LocalTime.of(HORAS[indice % SERVICIOS_POR_DIA], 0)
        val estado = when {
            indice == RECHAZADO_EN_API || (indice in RECHAZOS && indice !in REEMPLAZOS) -> EstadoServicio.RECHAZADO
            else -> EstadoServicio.ACEPTADO
        }
        return Servicio(
            id = id(indice),
            titulo = "Servicio de demostración",
            origen = "",
            destino = "",
            fecha = hoy.minusDays((DIAS - indice / SERVICIOS_POR_DIA).toLong()),
            horaInicio = hora,
            horaFin = hora.plusHours(2),
            pasajeros = 0,
            nombresPasajeros = emptyList(),
            idiomaTour = "",
            liderGrupo = "",
            fonoLider = "",
            notasGrupo = "",
            guia = "",
            conductor = "",
            tarifa = 0,
            estado = estado,
            reserva = ""
        )
    }

    private fun checkin(servicio: Servicio, zona: ZoneId): CheckinServicio {
        val indice = servicio.id.removePrefix("demo-").toInt()
        val minutos = if (indice in ATRASADOS) 25L else indice % 12 - 8L
        return CheckinServicio(
            servicioId = servicio.id,
            checkInEn = milisegundos(servicio, minutos, zona),
            distanciaM = DISTANCIAS[indice % DISTANCIAS.size],
            gpsReal = true,
            checkOutEn = if (indice == SIN_CHECK_OUT) null else milisegundos(servicio, 120, zona),
            uid = "demo"
        )
    }

    private fun milisegundos(servicio: Servicio, minutos: Long, zona: ZoneId): Long =
        servicio.fecha.atTime(servicio.horaInicio).plusMinutes(minutos).atZone(zona).toInstant().toEpochMilli()
}
