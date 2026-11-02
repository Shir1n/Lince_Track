package com.example.lince_track.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lince_track.data.IncidenciaServicio
import com.example.lince_track.data.ReglasIncidencia
import com.example.lince_track.data.ServicioRepository
import com.example.lince_track.data.TipoIncidencia
import com.example.lince_track.util.PERMISOS_UBICACION
import com.example.lince_track.util.tienePermisoUbicacion
import com.example.lince_track.util.ubicacionActual
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Envía una incidencia con la ubicación de quien reporta. Pide el permiso si falta, pero la
 * incidencia sale igual sin ubicación: avisar del problema importa más que el punto exacto.
 * Una vez iniciado, el envío termina aunque la persona salga de la pantalla.
 */
@Composable
fun rememberEnviarIncidencia(
    servicioId: String,
    snackbarHostState: SnackbarHostState
): (TipoIncidencia, String) -> Unit {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var pendiente by remember { mutableStateOf<Pair<TipoIncidencia, String>?>(null) }

    fun enviar(tipo: TipoIncidencia, detalle: String) {
        scope.launch {
            val enviando = launch {
                snackbarHostState.showSnackbar("Enviando incidencia...", duration = SnackbarDuration.Indefinite)
            }
            val resultado = withContext(NonCancellable) {
                ServicioRepository.reportarIncidencia(servicioId, tipo, detalle, ubicacionActual(context))
            }
            enviando.cancelAndJoin()
            resultado
                .onSuccess { aviso -> snackbarHostState.showSnackbar(aviso ?: "Incidencia enviada a Coordinación") }
                .onFailure { e -> snackbarHostState.showSnackbar(e.message ?: "No se pudo enviar la incidencia") }
        }
    }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        pendiente?.let { (tipo, detalle) -> enviar(tipo, detalle) }
        pendiente = null
    }

    return { tipo, detalle ->
        if (tienePermisoUbicacion(context)) {
            enviar(tipo, detalle)
        } else {
            pendiente = tipo to detalle
            pedirPermiso.launch(PERMISOS_UBICACION)
        }
    }
}

@Composable
fun ReportarIncidenciaDialog(
    onDismiss: () -> Unit,
    onConfirm: (TipoIncidencia, String) -> Unit
) {
    var tipo by rememberSaveable { mutableStateOf<TipoIncidencia?>(null) }
    var detalle by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
        },
        title = {
            Text(
                text = "Reportar incidencia",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text(
                    text = "Coordinación recibirá el aviso de inmediato. Se enviará tu ubicación a Coordinación.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                TipoIncidencia.entries.forEach { opcion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tipo = opcion },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = tipo == opcion, onClick = { tipo = opcion })
                        Text(
                            text = opcion.etiqueta,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = detalle,
                    onValueChange = { detalle = it.take(ReglasIncidencia.MAX_DETALLE) },
                    placeholder = {
                        Text(
                            text = if (tipo?.requiereTexto == true) "Describe lo que pasó (Obligatorio)..." else "Describe lo que pasó...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { tipo?.let { onConfirm(it, detalle.trim()) } },
                enabled = ReglasIncidencia.detalleValido(tipo, detalle),
                shape = RoundedCornerShape(50)
            ) {
                Text("ENVIAR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCELAR",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

/** Mensaje de Coordinación para quien reportó. */
@Composable
fun MensajeIncidenciaDialog(
    titulo: String,
    obligatorio: Boolean,
    textoInicial: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var mensaje by rememberSaveable { mutableStateOf(textoInicial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            OutlinedTextField(
                value = mensaje,
                onValueChange = { mensaje = it.take(ReglasIncidencia.MAX_DETALLE) },
                placeholder = {
                    Text(
                        text = if (obligatorio) "Mensaje para quien reportó (Obligatorio)..." else "Mensaje para quien reportó...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(mensaje.trim()) },
                enabled = ReglasIncidencia.respuestaValida(mensaje, obligatorio),
                shape = RoundedCornerShape(50)
            ) {
                Text("ENVIAR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCELAR",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

/**
 * Una incidencia. Con [onResolver] muestra el botón para cerrarla; con [mostrarServicio] indica
 * de qué servicio y quién la reportó, para cuando se ve fuera del detalle. En [acciones] van los
 * demás botones de Coordinación.
 */
@Composable
fun IncidenciaCard(
    incidencia: IncidenciaServicio,
    mostrarServicio: Boolean = false,
    onClick: (() -> Unit)? = null,
    onResolver: (() -> Unit)? = null,
    acciones: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${ReglasIncidencia.resumen(incidencia)} • ${incidencia.estadoTexto}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (incidencia.resuelta) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
            )
            if (mostrarServicio) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${incidencia.titulo} • ${incidencia.nombre}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (incidencia.detalle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = incidencia.detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (incidencia.respuesta.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Coordinación: ${incidencia.respuesta}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (!incidencia.resuelta) {
                if (onResolver != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(onClick = onResolver, shape = RoundedCornerShape(50)) {
                        Text("Marcar resuelta", fontWeight = FontWeight.Bold)
                    }
                }
                acciones?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    it()
                }
            }
        }
    }
}
