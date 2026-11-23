package com.example.lince_track.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lince_track.R
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.Rol
import com.example.lince_track.data.privacidad.EstadoSolicitud
import com.example.lince_track.data.privacidad.SolicitudPrivacidad
import com.example.lince_track.data.privacidad.SolicitudPrivacidadRepository
import com.example.lince_track.data.remote.mensajeBloqueo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy")

private fun fecha(milis: Long?): String =
    milis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("America/Santiago")).format(FORMATO_FECHA) } ?: "—"

/**
 * Derechos sobre los datos personales (Ley 21.719) para conductores y guías, dentro de Ajustes → Privacidad:
 * descargar lo propio y pedir la eliminación (o cancelarla mientras esté pendiente).
 */
@Composable
fun SeccionMisDatos() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val perfil by AuthRepository.perfilActual.collectAsState()
    val uid = AuthRepository.usuarioActual?.uid
    val rol = perfil?.rol
    if (uid == null || rol == null || rol == Rol.ADMINISTRADOR) return

    val solicitud by remember(uid) { SolicitudPrivacidadRepository.observar(uid) }.collectAsState(initial = null)
    var mostrarDialogo by remember { mutableStateOf(false) }
    var trabajando by remember { mutableStateOf(false) }
    val errorGenerico = stringResource(R.string.error_operacion)

    fun avisar(texto: String) = Toast.makeText(context, texto, Toast.LENGTH_LONG).show()

    val guardarArchivo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            trabajando = true
            SolicitudPrivacidadRepository.exportar(uid)
                .onSuccess { contenido ->
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(contenido.toByteArray()) }
                    }
                    avisar(context.getString(R.string.mis_datos_descargados))
                }
                .onFailure { avisar(it.mensajeBloqueo(errorGenerico)) }
            trabajando = false
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    Text(
        text = stringResource(R.string.mis_datos_titulo),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Text(
        text = stringResource(R.string.mis_datos_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedButton(
        onClick = { guardarArchivo.launch("lince-track-mis-datos.json") },
        enabled = !trabajando,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(stringResource(R.string.mis_datos_descargar), fontWeight = FontWeight.Bold)
    }

    val actual = solicitud
    when (actual?.estado) {
        EstadoSolicitud.PENDIENTE -> {
            Text(
                text = stringResource(R.string.mis_datos_pendiente, fecha(actual.creadaEn)),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary
            )
            TextButton(
                onClick = {
                    scope.launch {
                        SolicitudPrivacidadRepository.cancelar(uid)
                            .onSuccess { avisar(context.getString(R.string.mis_datos_cancelada)) }
                            .onFailure { avisar(errorGenerico) }
                    }
                },
                enabled = !trabajando
            ) {
                Text(stringResource(R.string.mis_datos_cancelar), fontWeight = FontWeight.Bold)
            }
        }
        EstadoSolicitud.APROBADA, EstadoSolicitud.COMPLETADA -> Text(
            text = stringResource(R.string.mis_datos_aprobada),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error
        )
        else -> {
            if (actual?.estado == EstadoSolicitud.RECHAZADA) {
                Text(
                    text = stringResource(R.string.mis_datos_rechazada, actual.respuesta),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = { mostrarDialogo = true },
                enabled = !trabajando,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.mis_datos_solicitar), fontWeight = FontWeight.Bold)
            }
        }
    }

    if (mostrarDialogo) {
        SolicitarEliminacionDialog(
            onDismiss = { mostrarDialogo = false },
            onConfirm = { motivo ->
                mostrarDialogo = false
                val perfilActual = perfil ?: return@SolicitarEliminacionDialog
                scope.launch {
                    SolicitudPrivacidadRepository.solicitar(uid, perfilActual, motivo)
                        .onSuccess { avisar(context.getString(R.string.mis_datos_enviada)) }
                        .onFailure { avisar(errorGenerico) }
                }
            }
        )
    }
}

@Composable
private fun SolicitarEliminacionDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var motivo by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.mis_datos_dialogo_titulo), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.mis_datos_dialogo_texto),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it.take(SolicitudPrivacidadRepository.MAX_MOTIVO) },
                    placeholder = { Text(stringResource(R.string.mis_datos_motivo)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(motivo) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.mis_datos_enviar), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.accion_cancelar), fontWeight = FontWeight.Bold) }
        }
    )
}

/**
 * Para Coordinación (Panel, sobre las incidencias abiertas): solicitudes de eliminación pendientes, con aprobar
 * (borra todo y cierra la cuenta) o rechazar (con motivo, que le llega a la persona). No se muestra si no hay.
 */
@Composable
fun SolicitudesPrivacidadAdmin() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rol by AuthRepository.rolActual.collectAsState()
    val pendientes by remember(rol) {
        if (rol == Rol.ADMINISTRADOR) SolicitudPrivacidadRepository.observarPendientes() else flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    if (pendientes.isEmpty()) return

    var aprobar by remember { mutableStateOf<SolicitudPrivacidad?>(null) }
    var rechazar by remember { mutableStateOf<SolicitudPrivacidad?>(null) }
    var trabajando by remember { mutableStateOf(false) }
    val errorGenerico = stringResource(R.string.error_operacion)
    val adminUid = AuthRepository.usuarioActual?.uid ?: return

    fun avisar(texto: String) = Toast.makeText(context, texto, Toast.LENGTH_LONG).show()

    // Igual que "Incidencias abiertas": un título de sección y una tarjeta por solicitud.
    Text(
        text = stringResource(R.string.admin_privacidad_titulo, pendientes.size),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.tertiary
    )
    pendientes.forEach { solicitud ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.admin_privacidad_tarjeta),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = solicitud.nombre.ifBlank { "—" } + " • " +
                        stringResource(if (solicitud.rol == Rol.GUIA.id) R.string.rol_guia else R.string.rol_conductor),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.admin_privacidad_fecha, fecha(solicitud.creadaEn)) +
                        if (solicitud.motivo.isNotBlank()) " • " + solicitud.motivo else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { aprobar = solicitud },
                        enabled = !trabajando,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.admin_privacidad_aprobar), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { rechazar = solicitud }, enabled = !trabajando, shape = RoundedCornerShape(50)) {
                        Text(stringResource(R.string.admin_privacidad_rechazar), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    aprobar?.let { solicitud ->
        AlertDialog(
            onDismissRequest = { aprobar = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.admin_privacidad_confirmar_titulo), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.admin_privacidad_confirmar_texto, solicitud.nombre.ifBlank { "—" })) },
            confirmButton = {
                Button(
                    onClick = {
                        aprobar = null
                        scope.launch {
                            trabajando = true
                            SolicitudPrivacidadRepository.aprobar(solicitud.uid, adminUid)
                                .onSuccess { avisar(context.getString(R.string.admin_privacidad_aprobada)) }
                                .onFailure { avisar(it.mensajeBloqueo(errorGenerico)) }
                            trabajando = false
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.admin_privacidad_aprobar), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { aprobar = null }) {
                    Text(stringResource(R.string.accion_cancelar), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    rechazar?.let { solicitud ->
        MensajeIncidenciaDialog(
            titulo = stringResource(R.string.admin_privacidad_rechazar_titulo, solicitud.nombre.ifBlank { "—" }),
            obligatorio = true,
            onDismiss = { rechazar = null },
            onConfirm = { respuesta ->
                rechazar = null
                scope.launch {
                    SolicitudPrivacidadRepository.rechazar(solicitud.uid, adminUid, respuesta)
                        .onSuccess { avisar(context.getString(R.string.admin_privacidad_rechazada)) }
                        .onFailure { avisar(errorGenerico) }
                }
            }
        )
    }
}
