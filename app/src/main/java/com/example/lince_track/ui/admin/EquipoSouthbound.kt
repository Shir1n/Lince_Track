package com.example.lince_track.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lince_track.R
import com.example.lince_track.data.EquipoSouthboundRepository
import com.example.lince_track.data.MiembroEquipo
import com.example.lince_track.data.TipoMiembro

/** Nombre del tipo de servicio de la API en el idioma de la app; un tipo nuevo se muestra con su código. */
@Composable
fun etiquetaTipoServicio(tipo: String): String = when (tipo) {
    "TRANSFER" -> stringResource(R.string.tipo_servicio_transfer)
    "GUIDED_SERVICE" -> stringResource(R.string.tipo_servicio_guiado)
    "EXCURSION" -> stringResource(R.string.tipo_servicio_excursion)
    else -> tipo
}

/** Filtro del panel por tipo de servicio, con los valores de `/catalogos`. [elegido] null = todos los tipos. */
@Composable
fun FiltroTipoServicio(tipos: List<String>, elegido: String?, onElegir: (String?) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (listOf<String?>(null) + tipos).forEach { tipo ->
            FilterChip(
                selected = elegido == tipo,
                onClick = { onElegir(tipo) },
                label = { Text(tipo?.let { etiquetaTipoServicio(it) } ?: stringResource(R.string.panel_tipo_todos)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

/**
 * Guías y transportistas de la oficina (`/guias` y `/transportistas`). Se piden recién al abrir la sección, para no
 * gastar solicitudes de la API en cada visita al panel. Sin clave de la API no se muestra.
 */
@Composable
fun SeccionEquipo(estado: EstadoEquipo, onAbrir: () -> Unit, onReintentar: () -> Unit) {
    var abierta by rememberSaveable { mutableStateOf(false) }
    var verGuias by rememberSaveable { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.equipo_titulo),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.equipo_desc, EquipoSouthboundRepository.OFICINA),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            estado.equipo?.let {
                Text(
                    text = stringResource(R.string.equipo_resumen, it.guias.size, it.transportistas.size),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            TextButton(onClick = {
                abierta = !abierta
                if (abierta) onAbrir()
            }) {
                Text(stringResource(if (abierta) R.string.equipo_ocultar else R.string.equipo_ver))
            }

            if (abierta) {
                val equipo = estado.equipo
                when {
                    estado.cargando -> Detalle(stringResource(R.string.equipo_cargando))
                    estado.error != null -> {
                        Detalle(estado.error.asString())
                        TextButton(onClick = onReintentar) { Text(stringResource(R.string.ruta_reintentar)) }
                    }
                    equipo != null -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = verGuias,
                                onClick = { verGuias = true },
                                label = { Text("${stringResource(R.string.equipo_guias)} (${equipo.guias.size})") }
                            )
                            FilterChip(
                                selected = !verGuias,
                                onClick = { verGuias = false },
                                label = {
                                    Text("${stringResource(R.string.equipo_transportistas)} (${equipo.transportistas.size})")
                                }
                            )
                        }
                        val lista = if (verGuias) equipo.guias else equipo.transportistas
                        if (lista.isEmpty()) Detalle(stringResource(R.string.equipo_vacio))
                        lista.forEachIndexed { i, miembro ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            FilaMiembro(miembro)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaMiembro(miembro: MiembroEquipo) {
    val idioma = LocalConfiguration.current.locales[0]
    Column(modifier = Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = miembro.nombre,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (miembro.activo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (!miembro.activo) {
                Text(
                    text = stringResource(R.string.equipo_inactivo),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                text = stringResource(R.string.equipo_evaluacion, String.format(idioma, "%.1f", miembro.evaluacion)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Detalle(
            when (miembro.tipo) {
                TipoMiembro.GUIA -> "${miembro.codigo} · " +
                    stringResource(R.string.equipo_idiomas, miembro.idiomas.joinToString(", "))
                TipoMiembro.TRANSPORTISTA -> "${miembro.codigo} · " + (
                    miembro.vehiculos.firstOrNull()?.let {
                        stringResource(R.string.equipo_vehiculo, it.tipo, it.patente, it.capacidad)
                    } ?: stringResource(R.string.equipo_sin_vehiculo)
                    )
            }
        )
    }
}

@Composable
private fun Detalle(texto: String) {
    Text(text = texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
