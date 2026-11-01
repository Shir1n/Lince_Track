package com.example.lince_track.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Abre un chat de WhatsApp con el número indicado usando el enlace oficial wa.me.
 *
 * Si WhatsApp está instalado, se abre directamente la conversación; si no, el
 * enlace se abre en el navegador (wa.me ofrece instalar la app).
 *
 * @param telefono número en cualquier formato ("+56 9 1234 5678", "569...").
 *   Se limpia dejando solo dígitos. Debe incluir código de país.
 * @param mensaje texto opcional que queda pre-escrito en el chat.
 */
fun abrirWhatsApp(context: Context, telefono: String, mensaje: String? = null) {
    val numero = telefono.filter { it.isDigit() }
    if (numero.isEmpty()) {
        Toast.makeText(context, "No hay un número de contacto disponible", Toast.LENGTH_SHORT).show()
        return
    }
    val url = buildString {
        append("https://wa.me/").append(numero)
        if (!mensaje.isNullOrBlank()) {
            append("?text=").append(Uri.encode(mensaje))
        }
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
    }
}
