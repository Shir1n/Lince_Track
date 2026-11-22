package com.example.lince_track.data

import com.example.lince_track.R
import com.example.lince_track.data.servicios.ServicioMapper
import com.example.lince_track.util.UiText
import java.util.Locale

/**
 * Textos de los servicios que vienen de la API de Southbound (siempre en español) para mostrarlos en el idioma de la
 * app. Los nombres propios de lugares (Frutillar, Lago Llanquihue, Puerto Varas…) no se traducen.
 */
object TextosServicio {

    /** Partes genéricas de los nombres de la API, en el orden en que se reemplazan. */
    private val REEMPLAZOS_EN = listOf(
        "Servicio guiado medio día" to "Half-day guided service",
        "Servicio guiado día completo" to "Full-day guided service",
        "Servicio guiado" to "Guided service",
        "(día completo)" to "(full day)",
        "(medio día)" to "(half day)",
        "(Inglés)" to "(English)",
        "(Español)" to "(Spanish)",
        "(Portugués)" to "(Portuguese)",
        ", sin guía" to ", no guide",
        "Excursión " to "Excursion ",
        " y " to " and "
    )

    /** Nombre de la actividad en el idioma [idioma]; en español queda tal como viene de la API. */
    fun titulo(titulo: String, idioma: Locale): String {
        if (idioma.language != "en") return titulo
        return REEMPLAZOS_EN.fold(titulo) { texto, (es, en) -> texto.replace(es, en) }
    }

    /** true si el nombre es un marcador de la app ("Sin guía", "Sin asignar"…) y no una persona. */
    fun esSinPersona(nombre: String): Boolean =
        nombre == ServicioMapper.SIN_GUIA || nombre == ServicioMapper.SIN_TRANSPORTISTA ||
            nombre == ReglasCompanero.SIN_ASIGNAR || nombre.isBlank()

    /** Nombre de la persona para mostrar; los marcadores internos se traducen. */
    fun nombre(nombre: String): UiText = when (nombre) {
        ServicioMapper.SIN_GUIA -> UiText.StringResource(R.string.persona_sin_guia)
        ServicioMapper.SIN_TRANSPORTISTA -> UiText.StringResource(R.string.persona_sin_transportista)
        ReglasCompanero.SIN_ASIGNAR -> UiText.StringResource(R.string.perfil_sin_asignar)
        else -> UiText.DynamicString(nombre)
    }
}
