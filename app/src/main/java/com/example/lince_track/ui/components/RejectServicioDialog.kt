package com.example.lince_track.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lince_track.R
import com.example.lince_track.data.MotivoRechazo
import com.example.lince_track.data.ReglasRespuesta
import com.example.lince_track.data.Servicio

@Composable
fun RejectServicioDialog(
    servicio: Servicio,
    onDismiss: () -> Unit,
    onConfirm: (MotivoRechazo, String) -> Unit
) {
    var selectedReason by rememberSaveable { mutableStateOf<MotivoRechazo?>(null) }
    var observations by rememberSaveable { mutableStateOf("") }

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
                text = stringResource(R.string.rechazar_titulo, servicio.reserva),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.rechazar_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.rechazar_motivo_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                MotivoRechazo.entries.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(
                            text = reason.etiquetaLocalizada(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = observations,
                    onValueChange = { observations = it.take(ReglasRespuesta.MAX_OBSERVACIONES) },
                    placeholder = {
                        Text(
                            text = stringResource(
                                if (selectedReason?.requiereTexto == true) {
                                    R.string.rechazar_obs_obligatorio
                                } else {
                                    R.string.rechazar_obs_opcional
                                }
                            ),
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
                onClick = { selectedReason?.let { onConfirm(it, observations.trim()) } },
                enabled = ReglasRespuesta.motivoValido(selectedReason, observations),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.rechazar_confirmar), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.accion_cancelar),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun MotivoRechazo.etiquetaLocalizada(): String = when (this) {
    MotivoRechazo.FALLA_MECANICA -> stringResource(R.string.motivo_falla_mecanica)
    MotivoRechazo.FUERZA_MAYOR -> stringResource(R.string.motivo_fuerza_mayor)
    MotivoRechazo.CRUCE_HORARIO -> stringResource(R.string.motivo_cruce_horario)
    MotivoRechazo.OTRO -> stringResource(R.string.motivo_otro)
}
