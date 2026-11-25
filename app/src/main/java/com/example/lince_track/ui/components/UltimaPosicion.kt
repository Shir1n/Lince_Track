package com.example.lince_track.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lince_track.R
import com.example.lince_track.data.PosicionRepository
import com.example.lince_track.data.PosicionServicio
import com.example.lince_track.data.ReglasPosicion
import com.example.lince_track.data.Ubicacion
import com.example.lince_track.data.remote.mensajeBloqueo
import com.example.lince_track.util.abrirEnMapa
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HORA = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Para Coordinación, en el detalle de un servicio con check-in: la última posición que informó el conductor o guía
 * (se envía cada 5 minutos mientras tiene el mapa abierto y el servicio en curso). No hay rastreo continuo.
 */
@Composable
fun UltimaPosicionCard(servicioId: String) {
    val context = LocalContext.current
    val repositorio = remember { PosicionRepository() }
    var cargando by remember { mutableStateOf(true) }
    var posicion by remember { mutableStateOf<PosicionServicio?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var recargar by remember { mutableIntStateOf(0) }
    val errorGenerico = stringResource(R.string.error_operacion)

    LaunchedEffect(servicioId, recargar) {
        cargando = true
        error = null
        repositorio.ultima(servicioId)
            .onSuccess { posicion = it }
            .onFailure { error = it.mensajeBloqueo(errorGenerico) }
        cargando = false
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.MyLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.posicion_titulo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            val actual = posicion
            Text(
                text = when {
                    cargando -> stringResource(R.string.posicion_cargando)
                    error != null -> error.orEmpty()
                    actual == null -> stringResource(R.string.posicion_sin_datos)
                    actual.registradaEn == null -> stringResource(R.string.posicion_sin_hora, actual.precisionMetros)
                    else -> stringResource(
                        R.string.posicion_hace,
                        ReglasPosicion.minutosDesde(actual.registradaEn, Instant.now()),
                        actual.registradaEn.atZone(ZoneId.of("America/Santiago")).format(HORA),
                        actual.precisionMetros
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.posicion_nota),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (actual != null) {
                    OutlinedButton(
                        onClick = { abrirEnMapa(context, Ubicacion(actual.lat, actual.lon)) },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(stringResource(R.string.posicion_ver_mapa), fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = { recargar++ }, enabled = !cargando) {
                    Text(stringResource(R.string.posicion_actualizar))
                }
            }
        }
    }
}
