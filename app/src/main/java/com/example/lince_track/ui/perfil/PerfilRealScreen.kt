package com.example.lince_track.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lince_track.BuildConfig
import com.example.lince_track.R
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.Rol
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.ServicioRepository
import com.example.lince_track.data.formatFecha
import com.example.lince_track.data.formatHora
import com.example.lince_track.ui.components.AvisoServiciosCard
import com.example.lince_track.ui.components.EstadoServicioBadge
import com.example.lince_track.ui.components.SoporteDialog
import com.example.lince_track.ui.theme.BarraSuperior
import java.time.YearMonth

/**
 * Perfil de la cuenta (`usuarios/{uid}`) con sus servicios y check-ins. Por ahora, para que se vea
 * completo en el video, el vehículo y la calificación de Patricio, "Verificado", la certificación
 * y el interruptor de notificaciones son datos fijos.
 */
@Composable
fun PerfilRealScreen(
    onLogout: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onServiceClick: (String) -> Unit = {}
) {
    val perfil by AuthRepository.perfilActual.collectAsState()
    val servicios by ServicioRepository.servicios.collectAsState()
    val checkins by ServicioRepository.checkins.collectAsState()
    // En el idioma de la app (el error del repositorio viene como UiText).
    val error = ServicioRepository.errorTexto.collectAsState().value?.asString()
    val email = AuthRepository.usuarioActual?.email.orEmpty()
    val nombre = perfil?.nombre?.takeIf { it.isNotBlank() } ?: email
    PerfilRealContenido(
        nombre = nombre,
        email = email,
        telefono = perfil?.telefono,
        rol = perfil?.rol,
        servicios = servicios,
        checkins = checkins,
        error = error,
        onLogout = onLogout,
        onOpenSettings = onOpenSettings,
        onServiceClick = onServiceClick
    )
}

@Composable
internal fun PerfilRealContenido(
    nombre: String,
    email: String,
    telefono: String?,
    rol: Rol?,
    servicios: List<Servicio>,
    checkins: Map<String, CheckinServicio>,
    error: String?,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    onServiceClick: (String) -> Unit
) {
    var mostrarSoporte by remember { mutableStateOf(false) }
    var notificaciones by remember { mutableStateOf(true) }
    val esPiloto1 = nombre.equals("Piloto1", ignoreCase = true) ||
        nombre.equals("Patricio Lagos", ignoreCase = true)
    val rolEtiqueta = when (rol) {
        Rol.ADMINISTRADOR -> stringResource(R.string.rol_administrador)
        Rol.CONDUCTOR -> stringResource(R.string.rol_conductor)
        Rol.GUIA -> stringResource(R.string.rol_guia)
        null -> stringResource(R.string.perfil_sin_dato)
    }
    val sinDato = stringResource(R.string.perfil_sin_dato)
    val vehiculo = remember(servicios) { DatosPerfil.vehiculo(servicios) }
    val enRuta = remember(servicios, checkins) { DatosPerfil.enRuta(servicios, checkins) }
    val proximo = remember(servicios, checkins) { DatosPerfil.proximoServicio(servicios, checkins) }
    val actividad = remember(servicios, checkins) { DatosPerfil.actividad(servicios, checkins) }
    val viajesMes = remember(servicios) {
        val mesActual = YearMonth.now()
        servicios.count { YearMonth.from(it.fecha) == mesActual }
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
                Box(Modifier.size(28.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.DirectionsCar, contentDescription = null,
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
                    Row(Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DirectionsCar, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(
                            if (rol != null) rolEtiqueta.uppercase() else stringResource(R.string.rol_conductor).uppercase(),
                            color = MaterialTheme.colorScheme.onSecondary,
                            style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Outlined.Person, contentDescription = stringResource(R.string.nav_perfil),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(5.dp))
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
            error?.let { AvisoServiciosCard(it) }

            Tarjeta {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(76.dp).background(
                            MaterialTheme.colorScheme.primaryContainer, CircleShape).padding(18.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = DatosPerfil.cargo(rol),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (enRuta) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.perfil_en_ruta),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MiniDatoPerfil(stringResource(R.string.perfil_cuenta), rolEtiqueta)
                        MiniDatoPerfil(stringResource(R.string.perfil_oficina), "Puerto Varas")
                        // El guía no maneja: no tiene vehículo que mostrar.
                        if (rol != Rol.GUIA) MiniDatoPerfil(stringResource(R.string.perfil_vehiculo), vehiculo?.patente ?: sinDato)
                    }
                }
            }

            proximo?.let { servicio ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onServiceClick(servicio.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Event, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(9.dp))
                            Text(stringResource(R.string.perfil_proximo_servicio), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f))
                            EstadoServicioBadge(servicio.estado)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(servicio.titulo, style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "${formatFecha(servicio.fecha, androidx.compose.ui.platform.LocalConfiguration.current.locales[0])} • ${formatHora(servicio.horaInicio)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Badge, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(stringResource(R.string.perfil_datos_personales), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f))
                    EtiquetaPerfil(stringResource(R.string.perfil_verificado))
                }
                Spacer(Modifier.height(16.dp))
                FilaDatoPersonal(stringResource(R.string.perfil_nombre_completo), nombre, Icons.Outlined.Person)
                FilaDatoPersonal(stringResource(R.string.perfil_correo_institucional), email.ifBlank { sinDato },
                    Icons.Outlined.Email, subrayado = true)
                telefono?.takeIf { it.isNotBlank() }?.let { telefonoActual ->
                    FilaDatoPersonal(stringResource(R.string.perfil_telefono), telefonoActual, Icons.Outlined.Phone)
                }
                FilaDatoPersonal(stringResource(R.string.perfil_rol), rolEtiqueta, Icons.Outlined.Badge)
            }

            if (rol != Rol.GUIA) {
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DirectionsBus, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(9.dp))
                        Text(stringResource(R.string.perfil_vehiculo_asignado),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f))
                        EtiquetaPerfil(
                            stringResource(
                                if (vehiculo != null) R.string.perfil_operativo else R.string.perfil_sin_asignar
                            )
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(132.dp).clip(RoundedCornerShape(10.dp))) {
                        Image(painterResource(if (esPiloto1) R.drawable.jacs5_piloto1
                            else R.drawable.mercedes_benz_sprinter),
                            contentDescription = stringResource(R.string.perfil_vehiculo_asignado),
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xDD00162A)))))
                        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                            if (!esPiloto1) Text(vehiculo?.tipo?.uppercase() ?: stringResource(R.string.perfil_vehiculo_pasajeros),
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                color = Color.White)
                            Text(if (esPiloto1) "JAC S5" else vehiculo?.tipo ?: stringResource(R.string.perfil_vehiculo_asignado),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = Color.White)
                            Text(if (esPiloto1) stringResource(R.string.perfil_anio_2015) else stringResource(R.string.perfil_vehiculo_asignado),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = .85f))
                        }
                        Surface(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp), shadowElevation = 3.dp) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.perfil_patente), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(vehiculo?.patente?.ifBlank { sinDato } ?: sinDato,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniDatoVehiculo(Icons.Outlined.Groups, stringResource(R.string.perfil_capacidad),
                            if (esPiloto1) stringResource(R.string.chip_pasajeros, CAPACIDAD_JAC_S5)
                            else vehiculo?.let { stringResource(R.string.chip_pasajeros, it.capacidad) } ?: sinDato,
                            Modifier.weight(1f))
                        MiniDatoVehiculo(Icons.Outlined.Speed, stringResource(R.string.perfil_odometro),
                            if (esPiloto1) ODOMETRO_JAC_S5 else sinDato, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Verified, contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(
                                if (esPiloto1) R.string.perfil_revision_al_dia
                                else R.string.perfil_revision_no_disponible
                            ),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Analytics, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(9.dp))
                    TituloSeccion(stringResource(R.string.perfil_resumen_operacional))
                }
                Text(stringResource(R.string.perfil_resumen_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstadisticaPerfil(Icons.Outlined.Route, viajesMes.toString(), stringResource(R.string.perfil_viajes_mes),
                        MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.onSecondary, Modifier.weight(1f))
                    EstadisticaPerfil(Icons.Outlined.Star, when {
                        esPiloto1 -> CALIFICACION_PATRICIO
                        rol == Rol.GUIA -> CALIFICACION_GUIA
                        else -> "—"
                    }, stringResource(R.string.perfil_calificacion),
                        MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.onTertiary, Modifier.weight(1f),
                        MaterialTheme.colorScheme.onTertiaryContainer)
                    EstadisticaPerfil(Icons.Outlined.AccessTime,
                        actividad.puntualidad?.let { "$it %" } ?: "—", stringResource(R.string.perfil_puntualidad),
                        MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.onSecondary, Modifier.weight(1f),
                        MaterialTheme.colorScheme.secondary)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniDatoVehiculo(Icons.Outlined.CheckCircle, stringResource(R.string.perfil_terminados),
                        actividad.terminados.toString(), Modifier.weight(1f))
                    MiniDatoVehiculo(Icons.Outlined.Schedule, stringResource(R.string.perfil_por_realizar),
                        actividad.porRealizar.toString(), Modifier.weight(1f))
                }
                if (rol != Rol.GUIA) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Verified, contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.perfil_certificacion),
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.perfil_nivel_oro), style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center)
                    }
                }
            }

            Tarjeta {
                AjustesPerfil(
                    notificaciones = notificaciones,
                    onNotificacionesChange = { notificaciones = it },
                    onOpenSettings = onOpenSettings,
                    onOpenSupport = { mostrarSoporte = true }
                )
            }

            Button(
                onClick = onLogout,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = stringResource(R.string.perfil_cerrar_sesion),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
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

private const val SIN_DATO = "Sin información"
private const val CAPACIDAD_JAC_S5 = 5
private const val ODOMETRO_JAC_S5 = "86.400 km"
private const val CALIFICACION_PATRICIO = "4,9"
private const val CALIFICACION_GUIA = "4,8"

@Composable
internal fun MiniDatoPerfil(etiqueta: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
internal fun MiniDatoVehiculo(
    icono: ImageVector,
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Row(modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
        .padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(titulo, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun EstadisticaPerfil(
    icono: ImageVector,
    valor: String,
    titulo: String,
    fondo: Color,
    iconFondo: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    colorValor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(fondo)
        .padding(vertical = 10.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(iconFondo), contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black,
            color = colorValor)
        Text(titulo, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun EtiquetaPerfil(texto: String) {
    androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.secondary,
        shape = RoundedCornerShape(12.dp)) {
        Text(texto, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun Tarjeta(contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = contenido)
    }
}

@Composable
internal fun TituloSeccion(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

@Composable
internal fun FilaDatoPersonal(
    titulo: String,
    valor: String,
    icono: ImageVector,
    subrayado: Boolean = false
) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            if (subrayado) {
                Spacer(Modifier.height(2.dp))
                Box(Modifier.width(145.dp).height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .3f)))
            }
        }
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Total(etiqueta: String, valor: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = valor.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilaAccion(icono: ImageVector, texto: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AjustesPerfil(
    notificaciones: Boolean,
    onNotificacionesChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSupport: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Tune, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(9.dp))
            TituloSeccion(stringResource(R.string.perfil_ajustes_soporte))
        }
        AjusteSwitchPerfil(
            icono = Icons.AutoMirrored.Outlined.VolumeUp,
            titulo = stringResource(R.string.perfil_notif_sonoras),
            descripcion = stringResource(R.string.perfil_notif_sonoras_desc),
            activo = notificaciones,
            onChange = onNotificacionesChange
        )
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
            onClick = onOpenSupport
        )
    }
}

@Composable
internal fun AjusteNavegacionPerfil(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    onClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(descripcion, style = MaterialTheme.typography.bodySmall, lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AjusteSwitchPerfil(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    activo: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(descripcion, style = MaterialTheme.typography.bodySmall, lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = activo, onCheckedChange = onChange, modifier = Modifier.height(30.dp),
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant))
    }
}
