package com.example.lince_track.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

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

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> {
                if (args.isEmpty()) stringResource(resId)
                else stringResource(resId, *args.map { if (it is UiText) it.asString() else it }.toTypedArray())
            }
        }
    }

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> {
                if (args.isEmpty()) context.getString(resId)
                else context.getString(resId, *args.map { if (it is UiText) it.asString(context) else it }.toTypedArray())
            }
        }
    }
}
