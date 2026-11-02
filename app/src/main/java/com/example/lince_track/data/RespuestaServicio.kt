package com.example.lince_track.data

/** Motivos de rechazo. [codigo] es el valor que se guarda en Firestore. */
enum class MotivoRechazo(val codigo: String, val etiqueta: String, val requiereTexto: Boolean) {
    FALLA_MECANICA("falla_mecanica", "Falla mecánica de vehículo", false),
    FUERZA_MAYOR("fuerza_mayor", "Problema de salud / Fuerza mayor", false),
    CRUCE_HORARIO("cruce_horario", "Cruce de horario con otro servicio", false),
    OTRO("otro", "Otro", true);

    companion object {
        fun desde(codigo: String?): MotivoRechazo? = entries.firstOrNull { it.codigo == codigo }
    }
}

/**
 * Respuesta de una persona a un servicio, guardada en `respuestas/{uid}_{servicioId}`.
 *
 * @property respondidoEn hora del servidor en milisegundos (null si aún no llega al servidor).
 */
data class RespuestaServicio(
    val servicioId: String,
    val estado: EstadoServicio,
    val motivo: MotivoRechazo? = null,
    val observaciones: String = "",
    val respondidoEn: Long? = null
)

object ReglasRespuesta {

    const val MAX_OBSERVACIONES = 500

    /** Un motivo que exige texto no se acepta con observaciones vacías. */
    fun motivoValido(motivo: MotivoRechazo?, observaciones: String): Boolean =
        motivo != null &&
            observaciones.length <= MAX_OBSERVACIONES &&
            (!motivo.requiereTexto || observaciones.isNotBlank())

    /**
     * Estado que se muestra de un servicio: manda la respuesta de la persona, salvo que
     * la API ya lo marque como rechazado o cancelado (DL, CX o XX).
     */
    fun aplicar(servicio: Servicio, respuesta: RespuestaServicio?): Servicio =
        if (respuesta == null || servicio.estado == EstadoServicio.RECHAZADO) {
            servicio
        } else {
            servicio.copy(estado = respuesta.estado)
        }
}
