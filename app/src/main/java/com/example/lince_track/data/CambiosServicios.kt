package com.example.lince_track.data

import java.time.LocalDate

/** Algo que merece una notificación del teléfono. */
data class AvisoLocal(val servicioId: String, val titulo: String, val texto: String)

/** Compara dos listas de servicios y dice qué avisar. Sin dependencias de Android ni Firebase. */
object CambiosServicios {

    fun detectar(
        antes: List<Servicio>,
        ahora: List<Servicio>,
        rol: Rol,
        hoy: LocalDate = LocalDate.now()
    ): List<AvisoLocal> =
        if (rol == Rol.ADMINISTRADOR) paraAdministrador(antes, ahora) else paraPersonal(antes, ahora, hoy)

    private fun paraAdministrador(antes: List<Servicio>, ahora: List<Servicio>): List<AvisoLocal> {
        val previos = antes.associateBy { it.id }
        return ahora.mapNotNull { servicio ->
            val previo = previos[servicio.id] ?: return@mapNotNull null
            if (previo.estado == servicio.estado) return@mapNotNull null
            when (servicio.estado) {
                EstadoServicio.RECHAZADO ->
                    AvisoLocal(servicio.id, "Servicio rechazado", "${servicio.responsable} rechazó ${servicio.titulo}")
                EstadoServicio.ACEPTADO ->
                    AvisoLocal(servicio.id, "Servicio confirmado", "${servicio.responsable} confirmó ${servicio.titulo}")
                EstadoServicio.PENDIENTE -> null
            }
        }
    }

    private fun paraPersonal(antes: List<Servicio>, ahora: List<Servicio>, hoy: LocalDate): List<AvisoLocal> {
        val previos = antes.associateBy { it.id }
        val idsActuales = ahora.map { it.id }.toSet()
        val nuevosOCambiados = ahora.mapNotNull { servicio ->
            val previo = previos[servicio.id]
            if (previo == null) {
                val cuando = "${formatFechaCorta(servicio.fecha)}, ${formatHora(servicio.horaInicio)}"
                AvisoLocal(servicio.id, "Nuevo servicio asignado", "${servicio.titulo} • $cuando")
            } else {
                cambioDeCompanero(previo, servicio)
            }
        }
        val quitados = antes
            .filter { it.id !in idsActuales && !it.fecha.isBefore(hoy) && it.estado != EstadoServicio.RECHAZADO }
            .map { AvisoLocal(it.id, "Servicio reasignado", "${it.titulo} ya no está a tu cargo") }
        return nuevosOCambiados + quitados
    }

    private fun cambioDeCompanero(previo: Servicio, actual: Servicio): AvisoLocal? {
        val etiqueta = if (actual.esGuiado) "Conductor" else "Guía"
        val antes = if (actual.esGuiado) previo.conductor else previo.guia
        val ahora = if (actual.esGuiado) actual.conductor else actual.guia
        return when {
            antes == ahora -> null
            ahora == ReglasCompanero.SIN_ASIGNAR ->
                AvisoLocal(actual.id, "$etiqueta sin asignar", "$antes ya no está en ${actual.titulo}")
            else ->
                AvisoLocal(actual.id, "$etiqueta confirmado", "$ahora confirmó ${actual.titulo}")
        }
    }
}
