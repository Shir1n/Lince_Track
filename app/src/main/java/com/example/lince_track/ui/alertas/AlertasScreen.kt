package com.example.lince_track.ui.alertas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TipoAlerta {
    ATRASO,
    SERVICIO
}

enum class FiltroAlertas(val label: String) {
    TODAS("Todas"),
    ATRASOS("Atrasos"),
    SERVICIOS("Servicios")
}

data class AlertaItem(
    val id: String,
    val tipo: TipoAlerta,
    val badgeLabel: String,
    val badgeIcon: ImageVector,
    val badgeContainerColor: @Composable () -> Color,
    val badgeContentColor: @Composable () -> Color,
    val titulo: String,
    val descripcion: String,
    val tiempo: String
)

@Composable
fun AlertasScreen() {
    var filtroSeleccionado by remember { mutableStateOf(FiltroAlertas.TODAS) }

    val alertas = remember {
        listOf(
            AlertaItem(
                id = "ALT-001",
                tipo = TipoAlerta.ATRASO,
                badgeLabel = "Urgent Delay Warning",
                badgeIcon = Icons.Outlined.NotificationsActive,
                badgeContainerColor = { MaterialTheme.colorScheme.tertiary },
                badgeContentColor = { MaterialTheme.colorScheme.onTertiary },
                titulo = "Alerta Margen 15 min - Transfer Tepual",
                descripcion = "Faltan 5 minutos para el límite de tolerancia de Check-in en servicio #SB-8941.",
                tiempo = "Hace 5 min"
            ),
            AlertaItem(
                id = "ALT-002",
                tipo = TipoAlerta.SERVICIO,
                badgeLabel = "Reasignement Notice",
                badgeIcon = Icons.Filled.CheckCircle,
                badgeContainerColor = { MaterialTheme.colorScheme.secondary },
                badgeContentColor = { MaterialTheme.colorScheme.onSecondary },
                titulo = "Reasignación Confirmada",
                descripcion = "Excursión Saltos del Petrohué reasignada con éxito a Guía: Roberto Díaz.",
                tiempo = "Hace 30 min"
            ),
            AlertaItem(
                id = "ALT-003",
                tipo = TipoAlerta.SERVICIO,
                badgeLabel = "Schedule Change",
                badgeIcon = Icons.Outlined.Schedule,
                badgeContainerColor = { MaterialTheme.colorScheme.primary },
                badgeContentColor = { MaterialTheme.colorScheme.onPrimary },
                titulo = "Ajuste de Horario Pickup",
                descripcion = "Pickup en Hotel Cumbres ajustado a 08:45 AM por solicitud de coordinación.",
                tiempo = "Hace 1 hora"
            )
        )
    }

    val alertasFiltradas = remember(filtroSeleccionado, alertas) {
        when (filtroSeleccionado) {
            FiltroAlertas.TODAS -> alertas
            FiltroAlertas.ATRASOS -> alertas.filter { it.tipo == TipoAlerta.ATRASO }
            FiltroAlertas.SERVICIOS -> alertas.filter { it.tipo == TipoAlerta.SERVICIO }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AlertasHeader(
            filtroSeleccionado = filtroSeleccionado,
            onSeleccionarFiltro = { filtroSeleccionado = it }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (alertasFiltradas.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No tienes alertas en esta categoría",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                alertasFiltradas.forEach { alerta ->
                    AlertaCard(alerta = alerta)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AlertasHeader(
    filtroSeleccionado: FiltroAlertas,
    onSeleccionarFiltro: (FiltroAlertas) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            Text(
                text = "Centro de Alertas",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FiltroAlertas.entries.forEach { filtro ->
                    val selected = filtro == filtroSeleccionado
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier.clickable { onSeleccionarFiltro(filtro) }
                    ) {
                        Text(
                            text = filtro.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertaCard(alerta: AlertaItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Surface(
                color = alerta.badgeContainerColor(),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = alerta.badgeIcon,
                        contentDescription = null,
                        tint = alerta.badgeContentColor(),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alerta.badgeLabel,
                        color = alerta.badgeContentColor(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = alerta.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = alerta.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = alerta.tiempo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
