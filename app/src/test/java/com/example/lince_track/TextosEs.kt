package com.example.lince_track

import com.example.lince_track.data.AvisoLocal
import com.example.lince_track.util.UiText
import java.io.File
import java.util.Locale

/**
 * Traduce un UiText al español leyendo `res/values/strings.xml`, para que los tests sigan comparando la frase que ve
 * el usuario (en la JVM no hay recursos de Android).
 */
object TextosEs {

    private val textos: Map<String, String> by lazy {
        val archivo = listOf("src/main/res/values/strings.xml", "app/src/main/res/values/strings.xml")
            .map(::File).first { it.exists() }
        Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(archivo.readText())
            .associate { it.groupValues[1] to desescapar(it.groupValues[2]) }
    }

    private val nombres: Map<Int, String> by lazy {
        R.string::class.java.fields.associate { it.getInt(null) to it.name }
    }

    fun de(texto: UiText): String = when (texto) {
        is UiText.DynamicString -> texto.value
        is UiText.StringResource -> {
            val nombre = nombres[texto.resId] ?: error("Recurso desconocido: ${texto.resId}")
            val plantilla = textos[nombre] ?: error("Falta $nombre en values/strings.xml")
            val args = texto.args.map { if (it is UiText) de(it) else it }
            if (args.isEmpty()) plantilla else String.format(Locale.forLanguageTag("es-CL"), plantilla, *args.toTypedArray())
        }
    }

    /** Aviso con título y texto ya traducidos, para comparar en los tests. */
    fun de(aviso: AvisoLocal): Triple<String, String, String> =
        Triple(aviso.servicioId, de(aviso.titulo), de(aviso.texto))

    private fun desescapar(xml: String): String = xml
        .replace("\\n", "\n")
        .replace("\\'", "'")
        .replace("\\\"", "\"")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
}
