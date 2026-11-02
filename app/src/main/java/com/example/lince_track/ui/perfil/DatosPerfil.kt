package com.example.lince_track.ui.perfil

import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.Rol
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.VehiculoServicio
import java.time.LocalDate

data class TotalesPerfil(val servicios: Int, val aceptados: Int, val finalizados: Int)

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

    /** true si hay un check-in abierto en alguno de los servicios cargados. */
    fun enRuta(servicios: List<Servicio>, checkins: Map<String, CheckinServicio>): Boolean =
        servicios.any { servicio -> checkins[servicio.id]?.let { !it.finalizado } == true }
}
