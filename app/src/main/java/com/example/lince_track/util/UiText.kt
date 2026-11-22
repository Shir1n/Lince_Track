package com.example.lince_track.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.example.lince_track.data.TextosServicio

/**
 * Texto que se traduce recién al mostrarlo, para que el ViewModel y los repositorios no escriban frases en un idioma
 * fijo. Los argumentos pueden ser otros UiText (por ejemplo, "mensaje (ID: …)").
 */
sealed interface UiText {
    data class DynamicString(val value: String) : UiText

    data class StringResource(
        @param:StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText {
        constructor(@StringRes resId: Int, vararg args: Any) : this(resId, args.toList())
    }

    /** Nombre de una actividad de la API: se traduce lo genérico y se dejan los lugares (ver TextosServicio). */
    data class TituloServicio(val titulo: String) : UiText

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> {
                if (args.isEmpty()) stringResource(resId)
                else stringResource(resId, *args.map { if (it is UiText) it.asString() else it }.toTypedArray())
            }
            is TituloServicio -> TextosServicio.titulo(titulo, LocalConfiguration.current.locales[0])
        }
    }

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> {
                if (args.isEmpty()) context.getString(resId)
                else context.getString(resId, *args.map { if (it is UiText) it.asString(context) else it }.toTypedArray())
            }
            is TituloServicio -> TextosServicio.titulo(titulo, context.resources.configuration.locales[0])
        }
    }
}
