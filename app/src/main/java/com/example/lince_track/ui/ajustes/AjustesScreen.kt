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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import com.example.lince_track.R
import com.example.lince_track.data.version.ControlVersionRepository
import com.example.lince_track.data.version.InfoVersion
import com.example.lince_track.ui.components.AvisoPrivacidadDialog
import com.example.lince_track.ui.components.LanguageSwitcherButton
import com.example.lince_track.ui.components.VersionActualizadaDialog
import com.example.lince_track.ui.components.VersionDesactualizadaDialog
import com.example.lince_track.ui.components.label
import com.example.lince_track.ui.theme.AppLanguage
import com.example.lince_track.ui.theme.ThemeMode
import com.example.lince_track.ui.theme.idiomaActual
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
    val appLanguage = remember { idiomaActual() }
    val avisosVersionActivos by ControlVersionRepository.avisosActivosFlow(context)
        .collectAsState(initial = true)
    val logs by LinceLogger.logs.collectAsState()
    var showLogConsole by remember { mutableStateOf(false) }
    var mostrarNovedadesLocales by remember { mutableStateOf(false) }
    var versionRemotaPendiente by remember { mutableStateOf<InfoVersion?>(null) }
    var verificandoVersion by remember { mutableStateOf(false) }
    var mostrarPrivacidad by remember { mutableStateOf(false) }
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
                text = stringResource(R.string.ajustes_apariencia),
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
                text = stringResource(R.string.idioma_titulo),
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.cd_cambiar_idioma),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.idioma_actual, appLanguage.label()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LanguageSwitcherButton()
                }
            }

            Text(
                text = stringResource(R.string.ajustes_privacidad),
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.privacidad_titulo),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.privacidad_ajustes_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { mostrarPrivacidad = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = stringResource(R.string.privacidad_ver_aviso), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = stringResource(R.string.ajustes_diagnostico),
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
                        text = stringResource(R.string.ajustes_version_titulo),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val versionInstalada = stringResource(
                        R.string.ajustes_version_instalada,
                        versionLocal.versionName,
                        versionLocal.versionCode
                    )
                    val versionAutor = if (versionLocal.autorUltimoCambio.isNotBlank()) {
                        stringResource(R.string.ajustes_version_autor, versionLocal.autorUltimoCambio)
                    } else ""
                    Text(
                        text = versionInstalada + versionAutor,
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
                                text = stringResource(R.string.ajustes_avisos_version),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.ajustes_avisos_version_desc),
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
                            text = stringResource(
                                if (verificandoVersion) R.string.ajustes_verificando
                                else R.string.ajustes_verificar_actualizacion
                            ),
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
                        text = stringResource(R.string.ajustes_consola_logs),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.ajustes_consola_logs_desc, logs.size),
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
                            text = stringResource(R.string.ajustes_abrir_consola),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (mostrarPrivacidad) {
        AvisoPrivacidadDialog(primeraVez = false, onCerrar = { mostrarPrivacidad = false })
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
                contentDescription = stringResource(R.string.cd_volver),
                tint = BarraSuperior.contenido
            )
        }
        Text(
            text = stringResource(R.string.ajustes_titulo),
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
                text = mode.title(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = mode.subtitle(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThemeMode.title(): String = when (this) {
    ThemeMode.LIGHT -> stringResource(R.string.tema_claro)
    ThemeMode.DARK -> stringResource(R.string.tema_oscuro)
    ThemeMode.SYSTEM -> stringResource(R.string.tema_sistema)
}

@Composable
private fun ThemeMode.subtitle(): String = when (this) {
    ThemeMode.LIGHT -> stringResource(R.string.tema_claro_desc)
    ThemeMode.DARK -> stringResource(R.string.tema_oscuro_desc)
    ThemeMode.SYSTEM -> stringResource(R.string.tema_sistema_desc)
}
