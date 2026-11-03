package com.example.lince_track.ui.perfil

import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.ReglasCheckin
import com.example.lince_track.data.Rol
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.VehiculoServicio
import com.example.lince_track.ui.admin.DatosResumen
import com.example.lince_track.ui.admin.PeriodoResumen
import com.example.lince_track.ui.admin.ResumenOperacion
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class TotalesPerfil(val servicios: Int, val aceptados: Int, val finalizados: Int)

/** @property puntualidad porcentaje a tiempo; null si no hay check-ins en el periodo. */
data class ActividadPerfil(val terminados: Int, val porRealizar: Int, val puntualidad: Int?)

/** Cálculos del perfil, sin dependencias de Android ni Firebase. */
object DatosPerfil {

    fun cargo(rol: Rol?): String = when (rol) {
        Rol.GUIA -> "Guía Southbound"
        Rol.ADMINISTRADOR -> "Administrador Southbound"
        else -> "Transportista Southbound"
    }

    /**
     * La API no trae un vehículo fijo por persona: se usa el del próximo servicio o, si no hay,
     * el del más reciente. Los rechazados no cuentan.
     */
    fun vehiculo(servicios: List<Servicio>, hoy: LocalDate = LocalDate.now()): VehiculoServicio? {
        val conVehiculo = servicios.filter { it.vehiculo != null && it.estado != EstadoServicio.RECHAZADO }
        val (proximos, pasados) = conVehiculo.partition { !it.fecha.isBefore(hoy) }
        val elegido = proximos.minWithOrNull(compareBy({ it.fecha }, { it.horaInicio }))
            ?: pasados.maxWithOrNull(compareBy({ it.fecha }, { it.horaInicio }))
        return elegido?.vehiculo
    }

    fun totales(servicios: List<Servicio>, checkins: Map<String, CheckinServicio>) = TotalesPerfil(
        servicios = servicios.size,
        aceptados = servicios.count { it.estado == EstadoServicio.ACEPTADO },
        finalizados = servicios.count { checkins[it.id]?.finalizado == true }
    )

    /** Lo que sigue: desde hoy, sin contar lo rechazado ni lo terminado. Incluye lo que está en curso. */
    fun proximoServicio(
        servicios: List<Servicio>,
        checkins: Map<String, CheckinServicio>,
        hoy: LocalDate = LocalDate.now()
    ): Servicio? = servicios
        .filter { !it.fecha.isBefore(hoy) && it.estado != EstadoServicio.RECHAZADO }
        .filterNot { ReglasCheckin.estaTerminado(it, checkins[it.id]) }
        .minWithOrNull(compareBy({ it.fecha }, { it.horaInicio }))

    /** Terminados y puntualidad de los últimos 7 días, y lo aceptado que aún falta por hacer. */
    fun actividad(
        servicios: List<Servicio>,
        checkins: Map<String, CheckinServicio>,
        ahora: LocalDateTime = LocalDateTime.now(),
        zona: ZoneId = ZoneId.systemDefault()
    ): ActividadPerfil {
        val hoy = ahora.toLocalDate()
        val resumen = ResumenOperacion.calcular(
            DatosResumen(servicios = servicios, checkins = checkins),
            ResumenOperacion.rango(PeriodoResumen.SIETE_DIAS, hoy),
            ahora,
            zona
        )
        return ActividadPerfil(
            terminados = resumen.terminados,
            porRealizar = servicios.count {
                it.estado == EstadoServicio.ACEPTADO && !it.fecha.isBefore(hoy) &&
                    !ReglasCheckin.estaTerminado(it, checkins[it.id])
            },
            puntualidad = ResumenOperacion.porcentaje(resumen.aTiempo, resumen.conCheckIn)
        )
    }

    /** true si hay un check-in abierto en alguno de los servicios cargados. */
    fun enRuta(servicios: List<Servicio>, checkins: Map<String, CheckinServicio>): Boolean =
        servicios.any { servicio -> checkins[servicio.id]?.let { !it.finalizado } == true }
}
