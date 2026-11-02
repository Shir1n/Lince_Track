package com.example.lince_track.ui.historial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.ReglasCheckin
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.ServicioRepository
import com.example.lince_track.data.esGuiado
import com.example.lince_track.data.formatFechaCorta
import com.example.lince_track.ui.components.AvisoServiciosCard
import com.example.lince_track.ui.theme.BarraSuperior

/**
 * Servicios con check-out registrado, del más reciente al más antiguo. Solo alcanza a lo que
 * carga la app: los últimos 7 días (ver FiltroServicios).
 */
@Composable
fun ServiciosTerminadosScreen(
    onBack: () -> Unit = {},
    onServiceClick: (String) -> Unit = {}
) {
    val servicios by ServicioRepository.servicios.collectAsState()
    val checkins by ServicioRepository.checkins.collectAsState()
    val error by ServicioRepository.error.collectAsState()
    val terminados = remember(servicios, checkins) { ReglasCheckin.terminados(servicios, checkins) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(51.dp)
                .background(BarraSuperior.fondo)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = BarraSuperior.contenido
                )
            }
            Text(
                text = "Servicios Terminados",
                color = BarraSuperior.contenido,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            error?.let { AvisoServiciosCard(it) }

            if (terminados.isEmpty() && error == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aquí aparecerán los servicios al registrar el check-out",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            terminados.forEach { servicio ->
                checkins[servicio.id]?.let { checkin ->
                    TerminadoCard(servicio, checkin, onClick = { onServiceClick(servicio.id) })
                }
            }
        }
    }
}

/** Entrada a esta pantalla, dentro del resumen de hoy de conductores y guías. */
@Composable
fun VerTerminadosBoton(cantidad: Int, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Ver terminados ($cantidad)", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TerminadoCard(servicio: Servicio, checkin: CheckinServicio, onClick: () -> Unit) {
    val companero = if (servicio.esGuiado) "Conductor: ${servicio.conductor}" else "Guía: ${servicio.guia}"

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = servicio.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${formatFechaCorta(servicio.fecha)} • ${ReglasCheckin.horario(checkin)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${servicio.origen} – ${servicio.destino}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${servicio.pasajeros} Pasajeros • $companero",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
