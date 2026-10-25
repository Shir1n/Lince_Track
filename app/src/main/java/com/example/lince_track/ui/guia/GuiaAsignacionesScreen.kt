 package com.example.lince_track.ui.guia

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.ServicioRepository
import com.example.lince_track.data.formatFechaCorta
import com.example.lince_track.data.formatHora
import com.example.lince_track.ui.components.EstadoServicioBadge
import com.example.lince_track.ui.components.RejectServicioDialog
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Lista de excursiones/traslados asignados al guía logueado.
 *
 * Basada en ServiciosScreen: muestra los servicios del día filtrados por el
 * nombre del guía (campo `guia` de Servicio).
 *
 * TODO: obtener el nombre del guía desde la sesión (AuthRepository) en lugar
 * del valor por defecto.
 */
@Composable
fun GuiaAsignacionesScreen(
    guiaNombre: String = "Andrea Soto",
    onServiceClick: (String) -> Unit = {},
    onRouteClick: (String) -> Unit = {}
) {
    val servicios by ServicioRepository.servicios.collectAsState()
    val hoy = LocalDate.now()
    val serviciosHoy = remember(servicios, hoy, guiaNombre) {
        servicios
            .filter { it.fecha == hoy && it.guia.equals(guiaNombre, ignoreCase = true) }
            .sortedBy { it.horaInicio }
    }

    val scope = rememberCoroutineScope()
    var procesandoIds by remember { mutableStateOf(setOf<String>()) }
    var rechazoPendiente by remember { mutableStateOf<Servicio?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun operar(servicio: Servicio, op: suspend (String) -> Result<Unit>) {
        if (servicio.id in procesandoIds) return
        scope.launch {
            procesandoIds = procesandoIds + servicio.id
            val resultado = op(servicio.id)
            procesandoIds = procesandoIds - servicio.id
            resultado.onFailure { e ->
                snackbarHostState.showSnackbar(e.message ?: "No se pudo completar la operación")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            TopBar()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryKpiCard(serviciosHoy)

                Text(
                    text = "Mis Asignaciones de Hoy - ${formatFechaCorta(hoy)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (serviciosHoy.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No tienes asignaciones para hoy",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    serviciosHoy.forEach { servicio ->
                        ServicioCard(
                            servicio = servicio,
                            procesando = servicio.id in procesandoIds,
                            onAceptar = { operar(servicio, ServicioRepository::aceptar) },
                            onRechazar = { rechazoPendiente = servicio },
                            onClick = { onServiceClick(servicio.id) },
                            onRouteClick = { onRouteClick(servicio.id) }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    rechazoPendiente?.let { servicio ->
        RejectServicioDialog(
            servicio = servicio,
            onDismiss = { rechazoPendiente = null },
            onConfirm = {
                operar(servicio, ServicioRepository::rechazar)
                rechazoPendiente = null
            }
        )
    }
}

@Composable
private fun TopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lince Track",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Guía SERNATUR",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SummaryKpiCard(servicios: List<Servicio>) {
    val aceptados = servicios.filter { it.estado == EstadoServicio.ACEPTADO }
    val pendientes = servicios.filter { it.estado == EstadoServicio.PENDIENTE }
    val totalPasajeros = (aceptados + pendientes).sumOf { it.pasajeros }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            KpiStat(
                valor = "$totalPasajeros",
                etiqueta = "Pasajeros hoy",
                colorValor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            KpiStat(
                valor = "${aceptados.size}",
                etiqueta = "Tours aceptados",
                colorValor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            KpiStat(
                valor = "${pendientes.size}",
                etiqueta = "Tours pendientes",
                colorValor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiStat(
    valor: String,
    etiqueta: String,
    colorValor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = valor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = colorValor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ServicioCard(
    servicio: Servicio,
    procesando: Boolean,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
    onClick: () -> Unit,
    onRouteClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = servicio.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                EstadoServicioBadge(servicio.estado)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${formatHora(servicio.horaInicio)} – ${formatHora(servicio.horaFin)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${servicio.origen} → ${servicio.destino}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip("${servicio.pasajeros} Pasajeros")
                MetaChip("Conductor: ${servicio.conductor}")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (servicio.estado) {
                    EstadoServicio.PENDIENTE -> {
                        if (procesando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = onAceptar,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text("Confirmar", fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = onRechazar) {
                                    Text(
                                        text = "Rechazar",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    EstadoServicio.ACEPTADO -> {
                        OutlinedButton(
                            onClick = onRouteClick,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Ver Ruta", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    EstadoServicio.RECHAZADO -> {
                        OutlinedButton(
                            onClick = onClick,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Ver Detalle", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}
