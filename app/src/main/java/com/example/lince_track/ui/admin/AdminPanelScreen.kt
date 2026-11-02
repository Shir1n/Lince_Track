package com.example.lince_track.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lince_track.data.PersonaDisponible
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.ServicioRepository
import com.example.lince_track.data.formatFechaCorta
import com.example.lince_track.data.formatHora
import com.example.lince_track.ui.components.AvisoServiciosCard
import com.example.lince_track.ui.components.EstadoServicioBadge
import java.time.LocalDate
import kotlinx.coroutines.launch
import com.example.lince_track.ui.theme.BarraSuperior

/**
 * Panel del administrador: estado de los servicios de hoy y respuestas de conductores
 * y guías. Se actualiza solo cuando alguien responde.
 */
@Composable
fun AdminPanelScreen(onServiceClick: (String) -> Unit = {}) {
    val servicios by ServicioRepository.servicios.collectAsState()
    val respuestas by ServicioRepository.respuestas.collectAsState()
    val checkins by ServicioRepository.checkins.collectAsState()
    val error by ServicioRepository.error.collectAsState()
    var filtro by rememberSaveable { mutableStateOf(FiltroPanel.TODOS) }
    var porReasignar by remember { mutableStateOf<Servicio?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val hoy = LocalDate.now()
    val serviciosHoy = remember(servicios, hoy) { servicios.filter { it.fecha == hoy } }
    val totales = remember(serviciosHoy) { PanelAdmin.totales(serviciosHoy) }
    val filas = remember(serviciosHoy, respuestas, checkins, filtro) {
        PanelAdmin.filas(serviciosHoy, respuestas, filtro, checkins)
    }
    // Un rechazo de otro día también hay que resolverlo: no puede quedar escondido hasta esa fecha.
    val rechazadosProximos = remember(servicios, respuestas, checkins, hoy) {
        PanelAdmin.filas(servicios.filter { it.fecha.isAfter(hoy) }, respuestas, FiltroPanel.RECHAZADOS, checkins)
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BarraSuperior.fondo)
                .padding(horizontal = 16.dp, vertical = 11.dp)
        ) {
            Text(
                text = "Panel de Coordinación",
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
            Text(
                text = "Servicios de Hoy - ${formatFechaCorta(hoy)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TotalCard("Pendientes", totales.pendientes, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                TotalCard("Aceptados", totales.aceptados, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                TotalCard("Rechazados", totales.rechazados, MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FiltroPanel.entries.forEach { opcion ->
                    FilterChip(
                        selected = filtro == opcion,
                        onClick = { filtro = opcion },
                        label = { Text(opcion.etiqueta) }
                    )
                }
            }

            error?.let { AvisoServiciosCard(it) }

            if (filas.isEmpty() && error == null) {
                Text(
                    text = "No hay servicios para mostrar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }

            filas.forEach { fila ->
                FilaCard(
                    fila = fila,
                    onClick = { onServiceClick(fila.servicio.id) },
                    onReasignar = { porReasignar = fila.servicio }
                )
            }

            if (rechazadosProximos.isNotEmpty()) {
                Text(
                    text = "Rechazados en próximos días",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                rechazadosProximos.forEach { fila ->
                    FilaCard(
                        fila = fila,
                        onClick = { onServiceClick(fila.servicio.id) },
                        onReasignar = { porReasignar = fila.servicio },
                        fecha = formatFechaCorta(fila.servicio.fecha)
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

    porReasignar?.let { servicio ->
        ReasignarDialog(
            servicio = servicio,
            onDismiss = { porReasignar = null },
            onElegir = { persona ->
                porReasignar = null
                scope.launch {
                    ServicioRepository.reasignar(servicio, persona)
                        .onSuccess { aviso -> snackbarHostState.showSnackbar(aviso ?: "Asignado a ${persona.nombre}") }
                        .onFailure { e -> snackbarHostState.showSnackbar(e.message ?: "No se pudo reasignar") }
                }
            }
        )
    }
}

@Composable
private fun ReasignarDialog(
    servicio: Servicio,
    onDismiss: () -> Unit,
    onElegir: (PersonaDisponible) -> Unit
) {
    var candidatos by remember { mutableStateOf<List<PersonaDisponible>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(servicio.id) {
        ServicioRepository.candidatosPara(servicio)
            .onSuccess { candidatos = it }
            .onFailure { error = it.message }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reasignar servicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = servicio.titulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                val lista = candidatos
                when {
                    error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    lista == null -> CircularProgressIndicator()
                    lista.isEmpty() -> Text("No hay otra persona disponible para este servicio.")
                    else -> lista.forEach { persona ->
                        Text(
                            text = persona.nombre,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onElegir(persona) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun TotalCard(etiqueta: String, valor: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = valor.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FilaCard(
    fila: FilaPanel,
    onClick: () -> Unit,
    onReasignar: () -> Unit,
    fecha: String? = null
) {
    val servicio = fila.servicio
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = listOfNotNull(fecha, formatHora(servicio.horaInicio), fila.responsable).joinToString(" • ") +
                    if (servicio.reasignado) " (reemplazo)" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            fila.estadoRuta?.let { estadoRuta ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = estadoRuta,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            fila.motivo?.let { motivo ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Motivo: $motivo",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (fila.observaciones.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = fila.observaciones,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (fila.puedeReasignar) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = onReasignar, shape = RoundedCornerShape(50)) {
                    Text("Reasignar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
