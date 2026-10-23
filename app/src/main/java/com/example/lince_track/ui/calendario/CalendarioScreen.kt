package com.example.lince_track.ui.calendario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.formatFecha
import com.example.lince_track.data.formatHora
import com.example.lince_track.data.formatPrecioCLP
import com.example.lince_track.ui.agenda.AgendaViewModel
import com.example.lince_track.ui.components.EstadoServicioBadge
import com.example.lince_track.ui.components.RejectServicioDialog
import java.time.LocalDate
import java.time.YearMonth

private val weekDayLabels = listOf("L", "M", "X", "J", "V", "S", "D")

private enum class FiltroAgenda(val label: String) {
    TODOS("Todos"),
    PENDIENTES("Pendientes"),
    ACEPTADAS("Aceptadas"),
    RECHAZADAS("Rechazadas")
}

@Composable
fun CalendarioScreen(onServiceClick: (String) -> Unit = {}) {
    val viewModel: AgendaViewModel = viewModel()
    val servicios by viewModel.servicios.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val month by viewModel.month.collectAsState()
    val processingIds by viewModel.processingIds.collectAsState()
    val message by viewModel.message.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    val serviciosPorFecha = remember(servicios) { servicios.groupBy { it.fecha } }
    val conflictos = remember(servicios) { detectarConflictos(servicios) }

    var filtro by remember { mutableStateOf(FiltroAgenda.TODOS) }
    var rechazoPendiente by remember { mutableStateOf<Servicio?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AgendaTopBar()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CalendarioSection(
                    month = month,
                    selectedDate = selectedDate,
                    serviciosPorFecha = serviciosPorFecha,
                    onMesAnterior = viewModel::mesAnterior,
                    onMesSiguiente = viewModel::mesSiguiente,
                    onHoy = viewModel::irHoy,
                    onSeleccionar = viewModel::seleccionarFecha
                )

                FiltroChips(filtro = filtro, onFiltro = { filtro = it })

                AgendaDelDia(
                    fecha = selectedDate,
                    servicios = servicios,
                    filtro = filtro,
                    conflictoIds = conflictos,
                    processingIds = processingIds,
                    onAceptar = viewModel::aceptar,
                    onRechazar = { rechazoPendiente = it },
                    onVerDetalle = onServiceClick
                )
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
                viewModel.rechazar(servicio)
                rechazoPendiente = null
            }
        )
    }
}

@Composable
private fun AgendaTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Agenda",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun CalendarioSection(
    month: YearMonth,
    selectedDate: LocalDate,
    serviciosPorFecha: Map<LocalDate, List<Servicio>>,
    onMesAnterior: () -> Unit,
    onMesSiguiente: () -> Unit,
    onHoy: () -> Unit,
    onSeleccionar: (LocalDate) -> Unit
) {
    val hoy = LocalDate.now()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMesAnterior) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Mes anterior",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = capitalizar(month.month.toString()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = month.year.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onMesSiguiente) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Mes siguiente",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            TextButton(
                onClick = onHoy,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Hoy",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                weekDayLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val cells = buildCalendarCells(month)
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        DayCell(
                            date = date,
                            selected = date == selectedDate,
                            isToday = date == hoy,
                            statuses = date?.let { estadosPresentes(serviciosPorFecha[it]) } ?: emptyList(),
                            onClick = { if (date != null) onSeleccionar(date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    date: LocalDate?,
    selected: Boolean,
    isToday: Boolean,
    statuses: List<EstadoServicio>,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val background = when {
        selected -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(shape)
            .then(if (date != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(background)
            .then(
                if (isToday && !selected) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.secondary, shape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    statuses.take(3).forEach { estado ->
                        StatusDot(estado)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDot(estado: EstadoServicio) {
    val color = when (estado) {
        EstadoServicio.PENDIENTE -> MaterialTheme.colorScheme.tertiary
        EstadoServicio.ACEPTADO -> MaterialTheme.colorScheme.secondary
        EstadoServicio.RECHAZADO -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltroChips(filtro: FiltroAgenda, onFiltro: (FiltroAgenda) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FiltroAgenda.entries.forEach { opcion ->
            FilterChip(
                selected = filtro == opcion,
                onClick = { onFiltro(opcion) },
                label = { Text(opcion.label) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                )
            )
        }
    }
}

@Composable
private fun AgendaDelDia(
    fecha: LocalDate,
    servicios: List<Servicio>,
    filtro: FiltroAgenda,
    conflictoIds: Set<String>,
    processingIds: Set<String>,
    onAceptar: (Servicio) -> Unit,
    onRechazar: (Servicio) -> Unit,
    onVerDetalle: (String) -> Unit
) {
    val delDia = remember(servicios, fecha, filtro) {
        servicios
            .filter { it.fecha == fecha }
            .filter { coincideFiltro(it.estado, filtro) }
            .sortedBy { it.horaInicio }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = formatFecha(fecha),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (delDia.isEmpty()) {
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
                        text = "Sin trabajos para este día",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            delDia.forEach { servicio ->
                AgendaCard(
                    servicio = servicio,
                    enConflicto = servicio.id in conflictoIds,
                    procesando = servicio.id in processingIds,
                    onAceptar = { onAceptar(servicio) },
                    onRechazar = { onRechazar(servicio) },
                    onClick = { onVerDetalle(servicio.id) }
                )
            }
        }
    }
}

@Composable
private fun AgendaCard(
    servicio: Servicio,
    enConflicto: Boolean,
    procesando: Boolean,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
    onClick: () -> Unit
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${formatHora(servicio.horaInicio)} – ${formatHora(servicio.horaFin)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${servicio.pasajeros} pasajeros",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = servicio.origen,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Guía: ${servicio.guia}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (enConflicto && servicio.estado == EstadoServicio.ACEPTADO) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Conflicto de horario con otro trabajo aceptado",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatPrecioCLP(servicio.tarifa),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (servicio.estado == EstadoServicio.PENDIENTE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (procesando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Button(
                                onClick = onAceptar,
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("Aceptar", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onRechazar,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Rechazar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildCalendarCells(month: YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - 1
    val days = month.lengthOfMonth()
    val cells = ArrayList<LocalDate?>(leading + days)
    repeat(leading) { cells.add(null) }
    for (d in 1..days) cells.add(month.atDay(d))
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

private fun estadosPresentes(lista: List<Servicio>?): List<EstadoServicio> {
    val estados = lista?.map { it.estado }?.toSet() ?: emptySet()
    return EstadoServicio.entries.filter { it in estados }
}

private fun coincideFiltro(estado: EstadoServicio, filtro: FiltroAgenda): Boolean = when (filtro) {
    FiltroAgenda.TODOS -> true
    FiltroAgenda.PENDIENTES -> estado == EstadoServicio.PENDIENTE
    FiltroAgenda.ACEPTADAS -> estado == EstadoServicio.ACEPTADO
    FiltroAgenda.RECHAZADAS -> estado == EstadoServicio.RECHAZADO
}

private fun detectarConflictos(servicios: List<Servicio>): Set<String> {
    val aceptados = servicios.filter { it.estado == EstadoServicio.ACEPTADO }.groupBy { it.fecha }
    val conflictos = mutableSetOf<String>()
    aceptados.forEach { (_, lista) ->
        val ordenados = lista.sortedBy { it.horaInicio }
        for (i in ordenados.indices) {
            for (j in i + 1 until ordenados.size) {
                val a = ordenados[i]
                val b = ordenados[j]
                if (b.horaInicio.isBefore(a.horaFin) && a.horaInicio.isBefore(b.horaFin)) {
                    conflictos.add(a.id)
                    conflictos.add(b.id)
                }
            }
        }
    }
    return conflictos
}

private fun capitalizar(texto: String): String =
    texto.lowercase().replaceFirstChar { it.titlecase() }
