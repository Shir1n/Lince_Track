package com.example.lince_track.data

import com.example.lince_track.R
import com.example.lince_track.util.UiText
import java.time.LocalDate

/**
 * Algo que merece una notificación del teléfono. Título y texto van como UiText: se traducen al idioma de la app
 * recién al mostrar la notificación.
 */
data class AvisoLocal(val servicioId: String, val titulo: UiText, val texto: UiText)

/** Compara dos listas de servicios y dice qué avisar. Sin Firebase; los textos son recursos de Android. */
object CambiosServicios {

    fun detectar(
        antes: List<Servicio>,
        ahora: List<Servicio>,
        rol: Rol,
        hoy: LocalDate = LocalDate.now()
    ): List<AvisoLocal> =
        if (rol == Rol.ADMINISTRADOR) paraAdministrador(antes, ahora) else paraPersonal(antes, ahora, hoy)

    /** Al administrador, lo nuevo; a quien reportó, cuando se resuelve. */
    fun incidencias(
        antes: List<IncidenciaServicio>,
        ahora: List<IncidenciaServicio>,
        rol: Rol
    ): List<AvisoLocal> {
        val previas = antes.associateBy { it.id }
        return ahora.mapNotNull { incidencia ->
            val previa = previas[incidencia.id]
            when {
                rol == Rol.ADMINISTRADOR && previa == null && !incidencia.resuelta -> AvisoLocal(
                    incidencia.servicioId,
                    texto(R.string.aviso_incidencia_nueva, incidencia.tipo.texto),
                    texto(R.string.aviso_en_servicio, incidencia.nombre, UiText.TituloServicio(incidencia.titulo))
                )
                rol != Rol.ADMINISTRADOR && previa?.resuelta == false && incidencia.resuelta -> AvisoLocal(
                    incidencia.servicioId,
                    texto(R.string.aviso_incidencia_resuelta),
                    if (incidencia.respuesta.isNotBlank()) UiText.DynamicString(incidencia.respuesta)
                    else texto(R.string.aviso_en_servicio, incidencia.tipo.texto, UiText.TituloServicio(incidencia.titulo))
                )
                rol != Rol.ADMINISTRADOR && previa != null && previa.respuesta != incidencia.respuesta -> AvisoLocal(
                    incidencia.servicioId,
                    texto(R.string.aviso_coordinacion_respondio),
                    UiText.DynamicString(incidencia.respuesta)
                )
                else -> null
            }
        }
    }

    private fun paraAdministrador(antes: List<Servicio>, ahora: List<Servicio>): List<AvisoLocal> {
        val previos = antes.associateBy { it.id }
        return ahora.mapNotNull { servicio ->
            val previo = previos[servicio.id] ?: return@mapNotNull null
            if (previo.estado == servicio.estado) return@mapNotNull null
            when (servicio.estado) {
                EstadoServicio.RECHAZADO -> AvisoLocal(
                    servicio.id,
                    texto(R.string.aviso_servicio_rechazado),
                    texto(R.string.aviso_rechazo, TextosServicio.nombre(servicio.responsable), UiText.TituloServicio(servicio.titulo))
                )
                EstadoServicio.ACEPTADO -> AvisoLocal(
                    servicio.id,
                    texto(R.string.aviso_servicio_confirmado),
                    texto(R.string.aviso_confirmo, TextosServicio.nombre(servicio.responsable), UiText.TituloServicio(servicio.titulo))
                )
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
                AvisoLocal(
                    servicio.id,
                    texto(R.string.aviso_nuevo_servicio),
                    texto(
                        R.string.aviso_nuevo_servicio_detalle,
                        UiText.TituloServicio(servicio.titulo),
                        formatFechaCorta(servicio.fecha),
                        formatHora(servicio.horaInicio)
                    )
                )
            } else {
                cambioDeCompanero(previo, servicio)
            }
        }
        val quitados = antes
            .filter { it.id !in idsActuales && !it.fecha.isBefore(hoy) && it.estado != EstadoServicio.RECHAZADO }
            .map {
                AvisoLocal(it.id, texto(R.string.aviso_servicio_reasignado), texto(R.string.aviso_ya_no_a_cargo, UiText.TituloServicio(it.titulo)))
            }
        return nuevosOCambiados + quitados
    }

    private fun cambioDeCompanero(previo: Servicio, actual: Servicio): AvisoLocal? {
        val antes = if (actual.esGuiado) previo.conductor else previo.guia
        val ahora = if (actual.esGuiado) actual.conductor else actual.guia
        val antesNadie = TextosServicio.esSinPersona(antes)
        val ahoraNadie = TextosServicio.esSinPersona(ahora)
        return when {
            // "Sin guía" (de la API) y "Sin asignar" (nadie aceptó) son lo mismo: no hay nada que avisar.
            antes == ahora || (antesNadie && ahoraNadie) -> null
            ahoraNadie -> AvisoLocal(
                actual.id,
                texto(if (actual.esGuiado) R.string.aviso_conductor_sin_asignar else R.string.aviso_guia_sin_asignar),
                texto(R.string.aviso_ya_no_esta, antes, UiText.TituloServicio(actual.titulo))
            )
            else -> AvisoLocal(
                actual.id,
                texto(if (actual.esGuiado) R.string.aviso_conductor_confirmado else R.string.aviso_guia_confirmado),
                texto(R.string.aviso_confirmo, ahora, UiText.TituloServicio(actual.titulo))
            )
        }
    }

    private fun texto(resId: Int, vararg args: Any): UiText = UiText.StringResource(resId, *args)
}
