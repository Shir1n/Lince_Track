package com.example.lince_track.data

import java.time.Duration
import kotlin.math.abs

/**
 * Relaciona el servicio guiado con su transporte. En la API son dos servicios distintos, uno
 * con solo guía y otro con solo transportista, que comparten reserva.referencia. Una reserva
 * puede tener varios servicios, así que además deben ser del mismo día y cruzarse en el horario.
 */
object ReglasCompanero {

    const val SIN_ASIGNAR = "Sin asignar"

    /** El servicio de la otra persona; si hay varios, el que parte a la hora más cercana. */
    fun buscar(servicio: Servicio, candidatos: List<Servicio>): Servicio? =
        candidatos
            .filter {
                it.reserva == servicio.reserva &&
                    it.fecha == servicio.fecha &&
                    it.esGuiado != servicio.esGuiado &&
                    it.horaInicio <= servicio.horaFin &&
                    servicio.horaInicio <= it.horaFin
            }
            .minByOrNull { abs(Duration.between(servicio.horaInicio, it.horaInicio).toMinutes()) }

    /**
     * Agrega al servicio la otra persona del tour. Mientras no acepte no hay nadie asignado,
     * así que no se muestra ningún nombre.
     */
    fun completar(servicio: Servicio, candidatos: List<Servicio>): Servicio {
        val companero = buscar(servicio, candidatos) ?: return servicio
        val aceptado = companero.estado == EstadoServicio.ACEPTADO
        return if (servicio.esGuiado) {
            servicio.copy(conductor = if (aceptado) companero.conductor else SIN_ASIGNAR)
        } else {
            servicio.copy(guia = if (aceptado) companero.guia else SIN_ASIGNAR)
        }
    }

    /** Aplica al servicio de otra persona lo que ella publicó: su estado y su nombre real. */
    fun aplicarPublicado(companero: Servicio, publicado: RespuestaServicio?): Servicio {
        val conEstado = ReglasRespuesta.aplicar(companero, publicado)
        val nombre = publicado?.nombre?.takeIf { it.isNotBlank() } ?: return conEstado
        return if (companero.esGuiado) conEstado.copy(guia = nombre) else conEstado.copy(conductor = nombre)
    }

    /** Pone el nombre de la cuenta en el servicio propio, en vez del que trae la fuente. */
    fun conNombrePropio(servicio: Servicio, perfil: PerfilUsuario?): Servicio {
        val nombre = perfil?.nombre?.takeIf { it.isNotBlank() } ?: return servicio
        return when (perfil.rol) {
            Rol.CONDUCTOR -> servicio.copy(conductor = nombre)
            Rol.GUIA -> servicio.copy(guia = nombre)
            Rol.ADMINISTRADOR -> servicio
        }
    }
}
