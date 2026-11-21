package com.example.lince_track.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lince_track.R
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.util.abrirWhatsApp

/**
 * Diálogo de contacto con Soporte TI / Mesa de Ayuda.
 *
 * Ofrece las tres vías de contacto del equipo operativo:
 * WhatsApp (con mensaje pre-escrito), llamada directa y correo con
 * datos de diagnóstico pre-llenados (versión, rol, usuario, dispositivo).
 *
 * Los datos de contacto son provisionales: el plan de base de datos
 * contempla leerlos desde Firestore (config/app).
 */
// TODO(soporte): leer teléfono y correo desde Firestore config/app.
private const val SOPORTE_FONO = "+56987654321"
private const val SOPORTE_EMAIL = "soporte@lincetrack.cl"

@Composable
fun SoporteDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val rol by AuthRepository.rolActual.collectAsState()
    val whatsappMensaje = stringResource(R.string.soporte_whatsapp_mensaje)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Outlined.SupportAgent,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.soporte_titulo),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.soporte_subtitulo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OpcionContacto(
                    icono = Icons.Outlined.Chat,
                    titulo = stringResource(R.string.soporte_whatsapp_titulo),
                    descripcion = stringResource(R.string.soporte_whatsapp_desc),
                    onClick = {
                        abrirWhatsApp(
                            context = context,
                            telefono = SOPORTE_FONO,
                            mensaje = whatsappMensaje
                        )
                        onDismiss()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                OpcionContacto(
                    icono = Icons.Outlined.Call,
                    titulo = stringResource(R.string.soporte_llamada_titulo),
                    descripcion = SOPORTE_FONO,
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$SOPORTE_FONO"))
                        )
                        onDismiss()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                OpcionContacto(
                    icono = Icons.Outlined.Email,
                    titulo = stringResource(R.string.soporte_correo_titulo),
                    descripcion = SOPORTE_EMAIL,
                    onClick = {
                        enviarCorreoSoporte(context, rol?.name)
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.soporte_cerrar))
            }
        }
    )
}

@Composable
private fun OpcionContacto(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Abre la app de correo con asunto y datos de diagnóstico pre-llenados,
 * para que soporte reciba el contexto técnico sin pedírselo al usuario.
 */
private fun enviarCorreoSoporte(context: Context, rol: String?) {
    val versionApp = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrDefault("desconocida")
    val uidCorto = AuthRepository.usuarioActual?.uid?.take(8) ?: "desconocido"

    val cuerpo = """
        Describe tu problema aquí:


        ---- Datos de diagnóstico (no borrar) ----
        Versión app: $versionApp
        Rol: ${rol ?: "sin rol"}
        Usuario: $uidCorto
        Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}
        Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SOPORTE_EMAIL")).apply {
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.soporte_correo_asunto))
        putExtra(Intent.EXTRA_TEXT, cuerpo)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.soporte_correo_sin_app), Toast.LENGTH_SHORT).show()
    }
}
