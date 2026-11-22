package com.example.lince_track.util

import androidx.annotation.StringRes
import com.example.lince_track.data.SinRolException
import com.example.lince_track.data.remote.LinceApiException

/**
 * Error con su mensaje en español (para el registro y los tests) y el texto que se muestra, que se traduce al idioma
 * elegido en la app.
 */
class ErrorTraducible(val texto: UiText, mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa)

/** Texto de un error en el idioma de la app; si no trae uno propio, su mensaje o [porDefecto]. */
fun Throwable.textoUi(@StringRes porDefecto: Int): UiText = when (this) {
    is ErrorTraducible -> texto
    is SinRolException -> texto
    is LinceApiException -> texto
    else -> message?.takeIf { it.isNotBlank() }?.let { UiText.DynamicString(it) } ?: UiText.StringResource(porDefecto)
}
