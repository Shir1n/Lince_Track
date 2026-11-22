package com.example.lince_track.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lince_track.R
import com.example.lince_track.data.MotivoRechazo
import com.example.lince_track.data.PersonaDisponible
import com.example.lince_track.data.IncidenciaServicio
import com.example.lince_track.data.ReglasAsignacion
import com.example.lince_track.data.ReglasIncidencia
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.ServicioRepository
import com.example.lince_track.data.formatFechaCorta
import com.example.lince_track.data.formatHora
import com.example.lince_track.ui.components.AvisoServiciosCard
import com.example.lince_track.ui.components.EstadoServicioBadge
import com.example.lince_track.ui.components.IncidenciaCard
import com.example.lince_track.ui.components.MensajeIncidenciaDialog
import com.example.lince_track.util.abrirEnMapa
import com.example.lince_track.util.abrirWhatsApp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch
import com.example.lince_track.ui.theme.BarraSuperior
import com.example.lince_track.util.textoUi

/**
 * Panel del administrador: estado de los servicios del día elegido y respuestas de conductores
 * y guías. Se actualiza solo cuando alguien responde.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPanelScreen(onServiceClick: (String) -> Unit = {}) {
    val servicios by ServicioRepository.servicios.collectAsState()
    val respuestas by ServicioRepository.respuestas.collectAsState()
    val checkins by ServicioRepository.checkins.collectAsState()
    val incidencias by ServicioRepository.incidencias.collectAsState()
    // En el idioma de la app (el error del repositorio viene como UiText).
    val error = ServicioRepository.errorTexto.collectAsState().value?.asString()
    var filtro by rememberSaveable { mutableStateOf(FiltroPanel.TODOS) }
    var porReasignar by remember { mutableStateOf<Servicio?>(null) }
    var incidenciaDelReemplazo by remember { mutableStateOf<IncidenciaServicio?>(null) }
    var porResponder by remember { mutableStateOf<IncidenciaServicio?>(null) }
    var porResolver by remember { mutableStateOf<IncidenciaServicio?>(null) }
    var verResueltas by rememberSaveable { mutableStateOf(false) }
    var telefonos by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val hoy = LocalDate.now()
    // Sin un día elegido se sigue a hoy, aunque la app quede abierta de un día para otro.
    var diaElegido by rememberSaveable { mutableStateOf<Long?>(null) }
    val fecha = diaElegido?.let { LocalDate.ofEpochDay(it) } ?: hoy
    val serviciosDelDia = remember(servicios, fecha) { servicios.filter { it.fecha == fecha } }
    val totales = remember(serviciosDelDia, checkins) { PanelAdmin.totales(serviciosDelDia, checkins) }
    val filas = remember(serviciosDelDia, respuestas, checkins, filtro, incidencias, hoy) {
        PanelAdmin.filas(serviciosDelDia, respuestas, filtro, checkins, incidencias, hoy)
    }
    val incidenciasAbiertas = remember(incidencias) { ReglasIncidencia.abiertas(incidencias) }
    val incidenciasResueltas = remember(incidencias) { ReglasIncidencia.resueltas(incidencias) }

    LaunchedEffect(Unit) { telefonos = ServicioRepository.telefonosDelPersonal() }

    fun mostrarResultado(resultado: Result<String?>, exito: String? = null) {
        scope.launch {
            resultado
                .onSuccess { aviso ->
                    (aviso?.let { ServicioRepository.textoAviso(it).asString(context) } ?: exito)
                        ?.let { snackbarHostState.showSnackbar(it) }
                }
                .onFailure { e -> snackbarHostState.showSnackbar(e.textoUi(R.string.error_operacion).asString(context)) }
        }
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
                text = stringResource(R.string.admin_panel_titulo),
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
            SelectorFecha(
                fecha = fecha,
                hoy = hoy,
                onElegir = { elegida -> diaElegido = elegida.takeIf { it != hoy }?.toEpochDay() }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TotalCard(stringResource(R.string.admin_total_pendientes), totales.pendientes, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                TotalCard(stringResource(R.string.admin_total_aceptados), totales.aceptados, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                TotalCard(stringResource(R.string.admin_total_terminados), totales.terminados, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                TotalCard(stringResource(R.string.admin_total_rechazados), totales.rechazados, MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }

            if (incidenciasAbiertas.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.admin_incidencias_abiertas, incidenciasAbiertas.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                incidenciasAbiertas.forEach { incidencia ->
                    IncidenciaCard(
                        incidencia = incidencia,
                        mostrarServicio = true,
                        onClick = { onServiceClick(incidencia.servicioId) },
                        onResolver = { porResolver = incidencia },
                        acciones = {
                            val servicio = servicios.find { it.id == incidencia.servicioId }
                            val telefono = telefonos[incidencia.uid]
                            FlowRow {
                                TextButton(onClick = { porResponder = incidencia }) {
                                    Text(
                                        stringResource(
                                            if (incidencia.enGestion) R.string.admin_actualizar_respuesta
                                            else R.string.admin_responder
                                        )
                                    )
                                }
                                val ubicacion = incidencia.ubicacion
                                TextButton(
                                    onClick = { ubicacion?.let { abrirEnMapa(context, it) } },
                                    enabled = ubicacion != null
                                ) {
                                    Text(
                                        stringResource(
                                            if (ubicacion != null) R.string.admin_ver_ubicacion
                                            else R.string.admin_sin_ubicacion
                                        )
                                    )
                                }
                                if (servicio != null && ReglasAsignacion.puedeReemplazar(servicio, checkins[servicio.id])) {
                                    TextButton(onClick = {
                                        incidenciaDelReemplazo = incidencia
                                        porReasignar = servicio
                                    }) {
                                        Text(stringResource(R.string.admin_enviar_reemplazo))
                                    }
                                }
                                if (telefono != null) {
                                    TextButton(onClick = {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$telefono")))
                                    }) {
                                        Text(stringResource(R.string.admin_llamar))
                                    }
                                    TextButton(onClick = { abrirWhatsApp(context, telefono) }) {
                                        Text(stringResource(R.string.admin_whatsapp))
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (incidenciasResueltas.isNotEmpty()) {
                TextButton(onClick = { verResueltas = !verResueltas }) {
                    Text(
                        if (verResueltas) stringResource(R.string.admin_ocultar_resueltas)
                        else stringResource(R.string.admin_ver_resueltas, incidenciasResueltas.size)
                    )
                }
                if (verResueltas) {
                    incidenciasResueltas.forEach { incidencia ->
                        IncidenciaCard(
                            incidencia = incidencia,
                            mostrarServicio = true,
                            onClick = { onServiceClick(incidencia.servicioId) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FiltroPanel.entries.forEach { opcion ->
                    val labelRes = when (opcion) {
                        FiltroPanel.TODOS -> R.string.admin_filtro_todos
                        FiltroPanel.PENDIENTES -> R.string.admin_filtro_pendientes
                        FiltroPanel.ACEPTADOS -> R.string.admin_filtro_aceptados
                        FiltroPanel.TERMINADOS -> R.string.admin_filtro_terminados
                        FiltroPanel.RECHAZADOS -> R.string.admin_filtro_rechazados
                    }
                    FilterChip(
                        selected = filtro == opcion,
                        onClick = { filtro = opcion },
                        label = { Text(stringResource(labelRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                }
            }

            error?.let { AvisoServiciosCard(it) }

            if (filas.isEmpty() && error == null) {
                Text(
                    text = stringResource(R.string.admin_sin_servicios),
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
                    text = stringResource(R.string.admin_rechazados_proximos),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                rechazadosProximos.forEach { fila ->
                    FilaCard(
                        fila = fila,
                        onClick = { onServiceClick(fila.servicio.id) },
                        onReasignar = { porReasignar = fila.servicio },
                        fecha = formatFechaCorta(fila.servicio.fecha, LocalConfiguration.current.locales[0])
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
            onDismiss = {
                porReasignar = null
                incidenciaDelReemplazo = null
            },
            onElegir = { persona ->
                val incidencia = incidenciaDelReemplazo
                porReasignar = null
                incidenciaDelReemplazo = null
                scope.launch {
                    val resultado = ServicioRepository.reasignar(servicio, persona)
                    mostrarResultado(resultado, context.getString(R.string.admin_asignado_a, persona.nombre))
                    // Quien reportó deja de ver el servicio: hay que contarle quién lo reemplaza.
                    if (resultado.isSuccess && incidencia != null) {
                        ServicioRepository.responderIncidencia(
                            incidencia,
                            context.getString(R.string.admin_reemplazo_enviado, persona.nombre)
                        )
                    }
                }
            }
        )
    }

    porResponder?.let { incidencia ->
        MensajeIncidenciaDialog(
            titulo = stringResource(R.string.admin_responder_a, incidencia.nombre),
            obligatorio = true,
            textoInicial = incidencia.respuesta,
            onDismiss = { porResponder = null },
            onConfirm = { mensaje ->
                porResponder = null
                scope.launch {
                    mostrarResultado(
                        ServicioRepository.responderIncidencia(incidencia, mensaje),
                        context.getString(R.string.admin_respuesta_enviada)
                    )
                }
            }
        )
    }

    porResolver?.let { incidencia ->
        MensajeIncidenciaDialog(
            titulo = stringResource(R.string.admin_marcar_resuelta_titulo),
            obligatorio = false,
            onDismiss = { porResolver = null },
            onConfirm = { mensaje ->
                porResolver = null
                scope.launch {
                    mostrarResultado(
                        ServicioRepository.resolverIncidencia(incidencia, mensaje),
                        context.getString(R.string.admin_incidencia_resuelta)
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorFecha(fecha: LocalDate, hoy: LocalDate, onElegir: (LocalDate) -> Unit) {
    val rango = PanelAdmin.rangoFechas(hoy)
    var verCalendario by rememberSaveable { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onElegir(fecha.minusDays(1)) },
                enabled = fecha.minusDays(1) in rango
            ) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = stringResource(R.string.cd_dia_anterior))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { verCalendario = true }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(
                        when (fecha) {
                            hoy -> R.string.admin_servicios_hoy
                            hoy.minusDays(1) -> R.string.admin_servicios_ayer
                            hoy.plusDays(1) -> R.string.admin_servicios_manana
                            else -> R.string.admin_servicios_dia
                        }
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatFechaCorta(fecha, LocalConfiguration.current.locales[0]),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = { onElegir(fecha.plusDays(1)) },
                enabled = fecha.plusDays(1) in rango
            ) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.cd_dia_siguiente))
            }
        }
        if (fecha != hoy) {
            TextButton(
                onClick = { onElegir(hoy) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.admin_volver_hoy))
            }
        }
    }

    if (verCalendario) {
        // El calendario trabaja con medianoche en UTC, sin importar la zona del teléfono.
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() in rango
            }
        )
        DatePickerDialog(
            onDismissRequest = { verCalendario = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        estado.selectedDateMillis?.let {
                            onElegir(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        verCalendario = false
                    }
                ) {
                    Text(stringResource(R.string.accion_elegir))
                }
            },
            dismissButton = {
                TextButton(onClick = { verCalendario = false }) { Text(stringResource(R.string.accion_cancelar_normal)) }
            }
        ) {
            DatePicker(state = estado)
        }
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
    val context = LocalContext.current

    LaunchedEffect(servicio.id) {
        ServicioRepository.candidatosPara(servicio)
            .onSuccess { candidatos = it }
            .onFailure { error = it.textoUi(R.string.error_operacion).asString(context) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.admin_reasignar_titulo)) },
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
                    lista.isEmpty() -> Text(stringResource(R.string.admin_sin_candidatos))
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.accion_cancelar_normal)) }
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
                    if (servicio.reasignado) stringResource(R.string.admin_sufijo_reemplazo) else "",
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
            if (fila.incidenciasAbiertas > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (fila.incidenciasAbiertas == 1) stringResource(R.string.admin_una_incidencia_abierta)
                    else stringResource(R.string.admin_n_incidencias_abiertas, fila.incidenciasAbiertas),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            fila.motivo?.let { motivo ->
                val motivoLocalizado = when (motivo) {
                    PanelAdmin.RECHAZADO_EN_API -> stringResource(R.string.admin_motivo_rechazado_api)
                    PanelAdmin.MOTIVO_DESCONOCIDO -> stringResource(R.string.admin_motivo_desconocido)
                    MotivoRechazo.FALLA_MECANICA.etiqueta -> stringResource(R.string.motivo_falla_mecanica)
                    MotivoRechazo.FUERZA_MAYOR.etiqueta -> stringResource(R.string.motivo_fuerza_mayor)
                    MotivoRechazo.CRUCE_HORARIO.etiqueta -> stringResource(R.string.motivo_cruce_horario)
                    MotivoRechazo.OTRO.etiqueta -> stringResource(R.string.motivo_otro)
                    else -> motivo
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.admin_motivo_prefijo, motivoLocalizado),
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
                    Text(stringResource(R.string.admin_reasignar), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
