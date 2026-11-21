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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.Bloqueo
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.ReglasBloqueo
import com.example.lince_track.data.Rol
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.formatFecha
import com.example.lince_track.data.formatHora
import com.example.lince_track.data.formatNombreMes
import com.example.lince_track.data.formatPrecioCLP
import com.example.lince_track.data.servicios.ServicioMapper
import com.example.lince_track.ui.agenda.AgendaViewModel
import com.example.lince_track.ui.components.EstadoServicioBadge
import com.example.lince_track.ui.components.RejectServicioDialog
import java.time.LocalDate
import java.time.YearMonth
import com.example.lince_track.ui.theme.BarraSuperior

private val weekDayLabels = listOf("L", "M", "X", "J", "V", "S", "D")

private enum class FiltroAgenda(val label: String) {
    TODOS("Todos"),
    PENDIENTES("Pendientes"),
    ACEPTADAS("Aceptadas"),
    RECHAZADAS("Rechazadas")
}

@Composable
fun CalendarioScreen(
    onServiceClick: (String) -> Unit = {},
    // Los tests de Compose pasan uno con datos fijos (y el rol); la app usa el de siempre y el rol de la sesión.
    viewModel: AgendaViewModel = viewModel(),
    rol: Rol? = null
) {
    val servicios by viewModel.servicios.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val month by viewModel.month.collectAsState()
    val processingIds by viewModel.processingIds.collectAsState()
    val message by viewModel.message.collectAsState()

    val bloqueos by viewModel.bloqueos.collectAsState()
    val diasAnticipacion by viewModel.diasAnticipacion.collectAsState()
    val operandoBloqueo by viewModel.operandoBloqueo.collectAsState()
    val bloqueosPorFecha = remember(bloqueos) { bloqueos.associateBy { it.fecha } }

    var dialogoBloquearFecha by remember { mutableStateOf<LocalDate?>(null) }
    var dialogoDesbloquear by remember { mutableStateOf<Bloqueo?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    val serviciosPorFecha = remember(servicios) { servicios.groupBy { it.fecha } }
    // El administrador ve servicios de varias personas: que coincidan en hora no es un conflicto.
    val rolSesion by AuthRepository.rolActual.collectAsState()
    val rolActual = rol ?: rolSesion
    // Solo conductores y guías bloquean sus días: el administrador recibiría un 403 y vería los de todos como propios.
    val gestionaDisponibilidad = rolActual == Rol.CONDUCTOR || rolActual == Rol.GUIA
    val bloqueosVisibles = if (gestionaDisponibilidad) bloqueosPorFecha else emptyMap()
    val conflictos = remember(servicios, rolActual) {
        if (rolActual == Rol.ADMINISTRADOR) emptySet() else detectarConflictos(servicios)
    }

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
                    bloqueosPorFecha = bloqueosVisibles,
                    diasAnticipacion = diasAnticipacion,
                    mostrarDisponibilidad = gestionaDisponibilidad,
                    onMesAnterior = viewModel::mesAnterior,
                    onMesSiguiente = viewModel::mesSiguiente,
                    onHoy = viewModel::irHoy,
                    onSeleccionar = { date ->
                        if (date == selectedDate && gestionaDisponibilidad) {
                            val b = bloqueosPorFecha[date]
                            if (b != null) {
                                dialogoDesbloquear = b
                            } else if (viewModel.esDiaBloqueable(date)) {
                                dialogoBloquearFecha = date
                            }
                        } else {
                            viewModel.seleccionarFecha(date)
                        }
                    }
                )

                if (gestionaDisponibilidad) {
                    TarjetaDisponibilidad(
                        fecha = selectedDate,
                        bloqueo = bloqueosPorFecha[selectedDate],
                        esBloqueable = viewModel.esDiaBloqueable(selectedDate),
                        tieneServicios = ReglasBloqueo.tieneServicioAsignado(servicios, selectedDate),
                        diasAnticipacion = diasAnticipacion,
                        primerDiaBloqueable = viewModel.primerDiaBloqueable(),
                        onBloquear = { dialogoBloquearFecha = selectedDate },
                        onDesbloquear = { bloqueosPorFecha[selectedDate]?.let { dialogoDesbloquear = it } }
                    )
                }

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
            onConfirm = { motivo, observaciones ->
                viewModel.rechazar(servicio, motivo, observaciones)
                rechazoPendiente = null
            }
        )
    }

    dialogoBloquearFecha?.let { fecha ->
        BloquearDiaDialog(
            fecha = fecha,
            procesando = operandoBloqueo,
            onDismiss = { dialogoBloquearFecha = null },
            onConfirm = { motivo ->
                viewModel.bloquearDia(fecha, motivo) { exito ->
                    if (exito) dialogoBloquearFecha = null
                }
            }
        )
    }

    dialogoDesbloquear?.let { bloqueo ->
        DesbloquearDiaDialog(
            bloqueo = bloqueo,
            procesando = operandoBloqueo,
            onDismiss = { dialogoDesbloquear = null },
            onConfirm = {
                viewModel.desbloquearDia(bloqueo.id) { exito ->
                    if (exito) dialogoDesbloquear = null
                }
            }
        )
    }
}

@Composable
private fun TarjetaDisponibilidad(
    fecha: LocalDate,
    bloqueo: Bloqueo?,
    esBloqueable: Boolean,
    tieneServicios: Boolean,
    diasAnticipacion: Int,
    primerDiaBloqueable: LocalDate,
    onBloquear: () -> Unit,
    onDesbloquear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bloqueo != null) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (bloqueo != null) Icons.Outlined.Lock else Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = if (bloqueo != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                    Column {
                        Text(
                            text = if (bloqueo != null) "Día bloqueado" else "Disponibilidad",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (bloqueo != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        val subtitulo = when {
                            bloqueo != null -> if (!bloqueo.motivo.isNullOrBlank()) "Motivo: ${bloqueo.motivo}" else "No disponible para servicios"
                            tieneServicios -> "Tiene servicios asignados este día"
                            esBloqueable -> "Día libre disponible para bloquear"
                            else -> "Mínimo $diasAnticipacion días de anticipación (desde ${formatFecha(primerDiaBloqueable)})"
                        }
                        Text(
                            text = subtitulo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (bloqueo != null) {
                    Button(
                        onClick = onDesbloquear,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Text("Desbloquear día", style = MaterialTheme.typography.labelMedium)
                    }
                } else if (!tieneServicios) {
                    Button(
                        onClick = onBloquear,
                        enabled = esBloqueable,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag("btn_bloquear_dia")
                    ) {
                        Text("Bloquear día", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun BloquearDiaDialog(
    fecha: LocalDate,
    procesando: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var motivo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!procesando) onDismiss() },
        title = {
            Text(
                text = "Bloquear día",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "¿Deseas bloquear el ${formatFecha(fecha)}? Durante este día no recibirás servicios ni traslados.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { if (it.length <= 200) motivo = it },
                    label = { Text("Motivo (opcional)") },
                    placeholder = { Text("Ej. Trámite personal o descanso") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3,
                    supportingText = {
                        Text(
                            text = "${motivo.length}/200",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(motivo.ifBlank { null }) },
                enabled = !procesando
            ) {
                if (procesando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Bloquear")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !procesando
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DesbloquearDiaDialog(
    bloqueo: Bloqueo,
    procesando: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!procesando) onDismiss() },
        title = {
            Text(
                text = "Desbloquear día",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "¿Deseas desbloquear el día ${formatFecha(bloqueo.fecha)}? Volverás a estar disponible para recibir servicios.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!bloqueo.motivo.isNullOrBlank()) {
                    Text(
                        text = "Motivo registrado: \"${bloqueo.motivo}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !procesando,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (procesando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Text("Desbloquear")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !procesando
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun AgendaTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(51.dp)
            .background(BarraSuperior.fondo),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Agenda",
            color = BarraSuperior.contenido,
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
    bloqueosPorFecha: Map<LocalDate, Bloqueo> = emptyMap(),
    diasAnticipacion: Int = 7,
    mostrarDisponibilidad: Boolean = true,
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
                        text = formatNombreMes(month.month),
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
                        val bloqueo = date?.let { bloqueosPorFecha[it] }
                        // Sin gestión de disponibilidad (administrador) ningún día se atenúa.
                        val esAnticipacionValida = !mostrarDisponibilidad ||
                            (date?.let { ReglasBloqueo.tieneAnticipacion(it, hoy, diasAnticipacion) } ?: false)
                        DayCell(
                            date = date,
                            selected = date == selectedDate,
                            isToday = date == hoy,
                            statuses = date?.let { estadosPresentes(serviciosPorFecha[it]) } ?: emptyList(),
                            serviciosPorFecha = serviciosPorFecha,
                            bloqueo = bloqueo,
                            esAnticipacionValida = esAnticipacionValida,
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
    serviciosPorFecha: Map<LocalDate, List<Servicio>>,
    bloqueo: Bloqueo? = null,
    esAnticipacionValida: Boolean = false,
    onClick: () -> Unit
) {
    val isBlocked = bloqueo != null
    val shape = RoundedCornerShape(12.dp)
    val background = when {
        selected && isBlocked -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.secondary
        isBlocked -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        isToday && !selected -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(shape)
            .then(
                if (date != null) {
                    Modifier
                        .testTag("day_cell_$date")
                        .clickable(onClick = onClick)
                } else Modifier
            )
            .background(background)
            .then(
                when {
                    isBlocked && !selected -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.error, shape)
                    isToday && !selected -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.secondary, shape)
                    else -> Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected || isToday || isBlocked) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        selected -> MaterialTheme.colorScheme.onSecondary
                        isBlocked -> MaterialTheme.colorScheme.error
                        isToday -> MaterialTheme.colorScheme.secondary
                        !esAnticipacionValida && statuses.isEmpty() -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (isBlocked) {
                        Text(
                            text = "BLOQ",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (selected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                .padding(horizontal = 3.dp, vertical = 0.5.dp)
                        )
                    } else if (statuses.isNotEmpty()) {
                        val cantidad = serviciosPorFecha[date]?.size ?: statuses.size
                        val primerEstado = statuses.first()
                        val color = when (primerEstado) {
                            EstadoServicio.PENDIENTE -> MaterialTheme.colorScheme.tertiary
                            EstadoServicio.ACEPTADO -> MaterialTheme.colorScheme.secondary
                            EstadoServicio.RECHAZADO -> MaterialTheme.colorScheme.error
                        }
                        Text(
                            text = "$cantidad",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(color.copy(alpha = 0.12f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
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
        EstadoServicio.RECHAZADO -> MaterialTheme.colorScheme.error
    }
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun FiltroChips(filtro: FiltroAgenda, onFiltro: (FiltroAgenda) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FiltroAgenda.entries.forEach { opcion ->
            FilterChip(
                selected = filtro == opcion,
                onClick = { onFiltro(opcion) },
                label = {
                    Text(
                        text = opcion.label,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
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

            if (servicio.guia != ServicioMapper.SIN_GUIA) {
                Text(
                    text = "Guía: ${servicio.guia}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
                // La API aún no trae tarifa: sin ella no se muestra. El espacio mantiene los botones a la derecha.
                if (servicio.tarifa > 0) {
                    Text(
                        text = formatPrecioCLP(servicio.tarifa),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Spacer(modifier = Modifier.width(0.dp))
                }

                val rol by AuthRepository.rolActual.collectAsState()
                if (servicio.estado == EstadoServicio.PENDIENTE && rol != Rol.ADMINISTRADOR) {
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

