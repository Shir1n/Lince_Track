package com.example.lince_track.data

import java.time.LocalDate

/**
 * Representa un día bloqueado por un conductor o guía para indicar no disponibilidad.
 *
 * @property id Identificador del bloqueo asignado por el backend.
 * @property uid UID de Firebase del usuario dueño del bloqueo.
 * @property fecha Día bloqueado (formato LocalDate).
 * @property motivo Razón opcional del bloqueo (máx. 200 caracteres).
 * @property creadoEn Marca de tiempo ISO-8601 de creación en el servidor.
 */
data class Bloqueo(
    val id: Long,
    val uid: String,
    val fecha: LocalDate,
    val motivo: String? = null,
    val creadoEn: String? = null
)
