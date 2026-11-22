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
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lince_track.R
import com.example.lince_track.data.ServicioRepository
import com.example.lince_track.ui.components.AvisoServiciosCard
import com.example.lince_track.ui.theme.BarraSuperior
import com.example.lince_track.ui.components.tituloVisible

enum class FiltroAlertas(val label: String, @param:StringRes val labelRes: Int) {
    TODAS("Todas", R.string.alertas_filtro_todas),
    ATRASOS("Atrasos", R.string.alertas_filtro_atrasos),
    SERVICIOS("Servicios", R.string.alertas_filtro_servicios)
}

@Composable
fun AlertasScreen(onServiceClick: (String) -> Unit = {}) {
    var filtroSeleccionado by remember { mutableStateOf(FiltroAlertas.TODAS) }

    val servicios by ServicioRepository.servicios.collectAsState()
    val checkins by ServicioRepository.checkins.collectAsState()
    val respuestas by ServicioRepository.respuestas.collectAsState()
    // En el idioma de la app (el error del repositorio viene como UiText).
    val error = ServicioRepository.errorTexto.collectAsState().value?.asString()

    val alertas = remember(servicios, checkins, respuestas) {
        GeneradorAlertas.generar(servicios, checkins, respuestas)
    }

    val alertasFiltradas = remember(filtroSeleccionado, alertas) {
        when (filtroSeleccionado) {
            FiltroAlertas.TODAS -> alertas
            FiltroAlertas.ATRASOS -> alertas.filter { it.clase.tipo == TipoAlerta.ATRASO }
            FiltroAlertas.SERVICIOS -> alertas.filter { it.clase.tipo == TipoAlerta.SERVICIO }
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
            error?.let { AvisoServiciosCard(it) }

            if (alertasFiltradas.isEmpty() && error == null) {
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
                            text = stringResource(R.string.alertas_vacio),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                alertasFiltradas.forEach { alerta ->
                    AlertaCard(alerta = alerta, onClick = { onServiceClick(alerta.servicioId) })
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
            .background(BarraSuperior.fondo)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.alertas_titulo),
                color = BarraSuperior.contenido,
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
                        color = if (selected) MaterialTheme.colorScheme.secondary else BarraSuperior.contenido.copy(alpha = 0.15f),
                        border = if (selected) null else BorderStroke(1.dp, BarraSuperior.contenido.copy(alpha = 0.35f)),
                        modifier = Modifier.clickable { onSeleccionarFiltro(filtro) }
                    ) {
                        Text(
                            text = stringResource(filtro.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onSecondary else BarraSuperior.contenido,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertaCard(alerta: Alerta, onClick: () -> Unit) {
    val colorFondo: Color
    val colorContenido: Color
    val icono: ImageVector
    val claseLabelRes: Int
    val claseDescRes: Int
    when (alerta.clase) {
        ClaseAlerta.CHECKIN_ATRASADO -> {
            colorFondo = MaterialTheme.colorScheme.tertiary
            colorContenido = MaterialTheme.colorScheme.onTertiary
            icono = Icons.Outlined.NotificationsActive
            claseLabelRes = R.string.alerta_clase_atrasado
            claseDescRes = R.string.alerta_desc_atrasado
        }
        ClaseAlerta.POR_RESPONDER -> {
            colorFondo = MaterialTheme.colorScheme.primary
            colorContenido = MaterialTheme.colorScheme.onPrimary
            icono = Icons.Outlined.Schedule
            claseLabelRes = R.string.alerta_clase_por_responder
            claseDescRes = R.string.alerta_desc_por_responder
        }
        ClaseAlerta.EN_RUTA -> {
            colorFondo = MaterialTheme.colorScheme.secondary
            colorContenido = MaterialTheme.colorScheme.onSecondary
            icono = Icons.Filled.CheckCircle
            claseLabelRes = R.string.alerta_clase_en_ruta
            claseDescRes = R.string.alerta_desc_en_ruta
        }
        ClaseAlerta.CANCELADO -> {
            colorFondo = MaterialTheme.colorScheme.error
            colorContenido = MaterialTheme.colorScheme.onError
            icono = Icons.Outlined.Cancel
            claseLabelRes = R.string.alerta_clase_cancelado
            claseDescRes = R.string.alerta_desc_cancelado
        }
    }
    val tiempoLocalizado = if (alerta.tiempo.startsWith("Hoy • ")) {
        stringResource(R.string.agenda_hoy) + alerta.tiempo.removePrefix("Hoy")
    } else {
        alerta.tiempo
    }

    Card(
        onClick = onClick,
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
                color = colorFondo,
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = colorContenido,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(claseLabelRes),
                        color = colorContenido,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = tituloVisible(alerta.titulo),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(claseDescRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = tiempoLocalizado,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
