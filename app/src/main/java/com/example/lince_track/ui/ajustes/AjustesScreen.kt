package com.example.lince_track.ui.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.lince_track.data.version.ControlVersionRepository
import com.example.lince_track.data.version.InfoVersion
import com.example.lince_track.ui.components.VersionActualizadaDialog
import com.example.lince_track.ui.components.VersionDesactualizadaDialog
import com.example.lince_track.ui.theme.ThemeMode
import com.example.lince_track.ui.theme.setThemeMode
import com.example.lince_track.ui.theme.themeModeFlow
import com.example.lince_track.util.LinceLogConsoleDialog
import com.example.lince_track.util.LinceLogger
import kotlinx.coroutines.launch
import com.example.lince_track.ui.theme.BarraSuperior

@Composable
fun AjustesScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val themeMode by context.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
    val avisosVersionActivos by ControlVersionRepository.avisosActivosFlow(context)
        .collectAsState(initial = true)
    val logs by LinceLogger.logs.collectAsState()
    var showLogConsole by remember { mutableStateOf(false) }
    var mostrarNovedadesLocales by remember { mutableStateOf(false) }
    var versionRemotaPendiente by remember { mutableStateOf<InfoVersion?>(null) }
    var verificandoVersion by remember { mutableStateOf(false) }
    val versionLocal = ControlVersionRepository.versionLocal
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AjustesTopBar(onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Apariencia",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        ThemeModeOption(
                            mode = mode,
                            selected = themeMode == mode,
                            onClick = {
                                LinceLogger.i("AJUSTES", "Cambio de tema visual a ${mode.name}")
                                scope.launch { context.setThemeMode(mode) }
                            }
                        )
                    }
                }
            }

            Text(
                text = "Diagnóstico y Desarrollo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Versión de la Aplicación",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = buildString {
                            append("Instalada: v${versionLocal.versionName} (código ${versionLocal.versionCode})")
                            if (versionLocal.autorUltimoCambio.isNotBlank()) {
                                append(" · Último cambio por ${versionLocal.autorUltimoCambio}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Avisos de versión al iniciar",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Notificar si hay cambios nuevos en master",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = avisosVersionActivos,
                            onCheckedChange = { activo ->
                                scope.launch {
                                    ControlVersionRepository.setAvisosActivos(context, activo)
                                }
                            }
                        )
                    }
                    Button(
                        onClick = {
                            if (!verificandoVersion) {
                                verificandoVersion = true
                                scope.launch {
                                    val remota = ControlVersionRepository.consultarVersionRemota()
                                    verificandoVersion = false
                                    if (remota != null && remota.versionCode > versionLocal.versionCode) {
                                        versionRemotaPendiente = remota
                                    } else {
                                        mostrarNovedadesLocales = true
                                    }
                                }
                            }
                        },
                        enabled = !verificandoVersion,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (verificandoVersion) "VERIFICANDO..." else "VERIFICAR ACTUALIZACIÓN / NOVEDADES",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Consola de Logs Lince",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Inspecciona eventos de GPS, mapa, red y navegación (${logs.size} registros activos).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { showLogConsole = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "ABRIR CONSOLA DE LOGS",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showLogConsole) {
        LinceLogConsoleDialog(onDismiss = { showLogConsole = false })
    }

    if (mostrarNovedadesLocales) {
        VersionActualizadaDialog(
            version = versionLocal,
            onDismiss = {
                mostrarNovedadesLocales = false
                scope.launch {
                    ControlVersionRepository.marcarVersionVista(context, versionLocal.versionCode)
                }
            }
        )
    }

    versionRemotaPendiente?.let { remota ->
        VersionDesactualizadaDialog(
            local = versionLocal,
            remota = remota,
            onDismiss = { versionRemotaPendiente = null }
        )
    }
}

@Composable
private fun AjustesTopBar(onBack: () -> Unit) {
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
            text = "Ajustes",
            color = BarraSuperior.contenido,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ThemeModeOption(mode: ThemeMode, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = mode.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = mode.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val ThemeMode.title: String
    get() = when (this) {
        ThemeMode.LIGHT -> "Claro"
        ThemeMode.DARK -> "Oscuro"
        ThemeMode.SYSTEM -> "Sistema"
    }

private val ThemeMode.subtitle: String
    get() = when (this) {
        ThemeMode.LIGHT -> "Usar siempre el tema claro"
        ThemeMode.DARK -> "Usar siempre el tema oscuro"
        ThemeMode.SYSTEM -> "Seguir la configuración del dispositivo"
    }
