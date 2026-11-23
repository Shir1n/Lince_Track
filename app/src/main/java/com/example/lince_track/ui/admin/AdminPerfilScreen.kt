package com.example.lince_track.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lince_track.BuildConfig
import com.example.lince_track.R
import com.example.lince_track.ui.components.AvatarPerfil
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.IncidenciaServicio
import com.example.lince_track.data.Rol
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.ReglasIncidencia
import com.example.lince_track.data.ServicioRepository
import com.example.lince_track.ui.components.SoporteDialog
import com.example.lince_track.ui.perfil.AjusteNavegacionPerfil
import com.example.lince_track.ui.perfil.EstadisticaPerfil
import com.example.lince_track.ui.perfil.EtiquetaPerfil
import com.example.lince_track.ui.perfil.FilaDatoPersonal
import com.example.lince_track.ui.perfil.MiniDatoPerfil
import com.example.lince_track.ui.perfil.MiniDatoVehiculo
import com.example.lince_track.ui.perfil.TituloSeccion
import com.example.lince_track.ui.theme.BarraSuperior
import java.time.LocalDate

/**
 * Perfil del administrador, con el mismo estilo que el de conductores y guías: la cuenta
 * (`usuarios/{uid}`) y un resumen real del día, calculado igual que en el Panel.
 */
@Composable
fun AdminPerfilScreen(
    onLogout: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val perfil by AuthRepository.perfilActual.collectAsState()
    val servicios by ServicioRepository.servicios.collectAsState()
    val checkins by ServicioRepository.checkins.collectAsState()
    val incidencias by ServicioRepository.incidencias.collectAsState()
    val email = AuthRepository.usuarioActual?.email.orEmpty()
    val nombre = perfil?.nombre?.takeIf { it.isNotBlank() } ?: email
    AdminPerfilContenido(
        nombre = nombre,
        email = email,
        telefono = perfil?.telefono,
        rol = perfil?.rol,
        servicios = servicios,
        checkins = checkins,
        incidencias = incidencias,
        onLogout = onLogout,
        onOpenSettings = onOpenSettings
    )
}

@Composable
internal fun AdminPerfilContenido(
    nombre: String,
    email: String,
    telefono: String?,
    rol: Rol?,
    servicios: List<Servicio>,
    checkins: Map<String, CheckinServicio>,
    incidencias: List<IncidenciaServicio>,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    hoy: LocalDate = LocalDate.now()
) {
    var mostrarSoporte by remember { mutableStateOf(false) }
    val serviciosHoy = remember(servicios, hoy) { servicios.filter { it.fecha == hoy } }
    val totales = remember(serviciosHoy, checkins) { PanelAdmin.totales(serviciosHoy, checkins) }
    val abiertas = remember(incidencias) { ReglasIncidencia.abiertas(incidencias).size }
    val rolLabel = when (rol) {
        Rol.ADMINISTRADOR, null -> stringResource(R.string.rol_administrador)
        Rol.CONDUCTOR -> stringResource(R.string.rol_conductor)
        Rol.GUIA -> stringResource(R.string.rol_guia)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BarraSuperior.fondo)
                .height(43.dp)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(28.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.perfil_mi_perfil), color = BarraSuperior.contenido,
                        style = MaterialTheme.typography.titleSmall, fontSize = 15.sp,
                        fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.login_subtitulo), color = BarraSuperior.contenido.copy(alpha = .8f),
                        style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, maxLines = 1)
                }
                Surface(color = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(16.dp)) {
                    Text(stringResource(R.string.admin_perfil_badge), color = MaterialTheme.colorScheme.onSecondary,
                        style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TarjetaAdmin {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    AvatarPerfil()
                    Spacer(Modifier.height(8.dp))
                    Text(nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.admin_perfil_cargo), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MiniDatoPerfil(stringResource(R.string.perfil_cuenta), rolLabel)
                        MiniDatoPerfil(stringResource(R.string.perfil_oficina), "Puerto Varas")
                    }
                }
            }

            TarjetaAdmin {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Today, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(9.dp))
                    TituloSeccion(stringResource(R.string.admin_perfil_operacion_hoy))
                }
                Text(stringResource(R.string.admin_perfil_operacion_desc), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstadisticaPerfil(Icons.Outlined.Event, serviciosHoy.size.toString(), stringResource(R.string.admin_perfil_servicios),
                        MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.onSecondary, Modifier.weight(1f))
                    EstadisticaPerfil(Icons.Outlined.Warning, abiertas.toString(), stringResource(R.string.admin_perfil_incidencias),
                        MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.onTertiary, Modifier.weight(1f),
                        MaterialTheme.colorScheme.onTertiaryContainer)
                    EstadisticaPerfil(Icons.Outlined.Cancel, totales.rechazados.toString(), stringResource(R.string.admin_perfil_rechazos),
                        MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.onSecondary, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniDatoVehiculo(Icons.Outlined.ThumbUp, stringResource(R.string.admin_total_aceptados),
                        totales.aceptados.toString(), Modifier.weight(1f))
                    MiniDatoVehiculo(Icons.Outlined.CheckCircle, stringResource(R.string.admin_total_terminados),
                        totales.terminados.toString(), Modifier.weight(1f))
                }
            }

            TarjetaAdmin {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Badge, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(stringResource(R.string.admin_perfil_datos_cuenta), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f))
                    EtiquetaPerfil(stringResource(R.string.perfil_verificado))
                }
                Spacer(Modifier.height(16.dp))
                FilaDatoPersonal(stringResource(R.string.perfil_nombre_completo), nombre, Icons.Outlined.Person)
                FilaDatoPersonal(stringResource(R.string.perfil_correo_institucional), email.ifBlank { stringResource(R.string.perfil_sin_dato) },
                    Icons.Outlined.Email, subrayado = true)
                telefono?.takeIf { it.isNotBlank() }?.let { telefonoActual ->
                    FilaDatoPersonal(stringResource(R.string.perfil_telefono), telefonoActual, Icons.Outlined.Phone)
                }
                FilaDatoPersonal(stringResource(R.string.perfil_rol), rolLabel, Icons.Outlined.Badge)
            }

            TarjetaAdmin {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(9.dp))
                    TituloSeccion(stringResource(R.string.perfil_ajustes_soporte))
                }
                AjusteNavegacionPerfil(
                    icono = Icons.Outlined.Palette,
                    titulo = stringResource(R.string.perfil_ajustes_apariencia),
                    descripcion = stringResource(R.string.perfil_ajustes_apariencia_desc),
                    onClick = onOpenSettings
                )
                AjusteNavegacionPerfil(
                    icono = Icons.Outlined.SupportAgent,
                    titulo = stringResource(R.string.perfil_contactar_soporte),
                    descripcion = stringResource(R.string.perfil_contactar_soporte_desc),
                    onClick = { mostrarSoporte = true }
                )
            }

            Button(
                onClick = onLogout,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text(stringResource(R.string.perfil_cerrar_sesion), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            }

            Text(
                text = "Lince Track v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (mostrarSoporte) {
        SoporteDialog(onDismiss = { mostrarSoporte = false })
    }
}

@Composable
private fun TarjetaAdmin(contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = contenido)
    }
}
