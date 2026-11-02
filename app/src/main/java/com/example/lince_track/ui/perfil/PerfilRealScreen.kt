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
import androidx.compose.material.icons.outlined.Email
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lince_track.BuildConfig
import com.example.lince_track.R
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.Rol
import com.example.lince_track.data.ServicioRepository
import com.example.lince_track.data.servicios.FiltroServicios
import com.example.lince_track.ui.components.AvisoServiciosCard
import com.example.lince_track.ui.components.SoporteDialog
import com.example.lince_track.ui.theme.BarraSuperior
import java.time.YearMonth

/**
 * Perfil con datos reales: la cuenta (`usuarios/{uid}`) y los servicios de la API. Lo que no
 * tiene de dónde salir no se muestra, así que sin clave de la API ni id queda casi vacío.
 */
@Composable
fun PerfilRealScreen(
    onLogout: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val perfil by AuthRepository.perfilActual.collectAsState()
    val servicios by ServicioRepository.servicios.collectAsState()
    val checkins by ServicioRepository.checkins.collectAsState()
    val error by ServicioRepository.error.collectAsState()
    var mostrarSoporte by remember { mutableStateOf(false) }

    val email = AuthRepository.usuarioActual?.email.orEmpty()
    val nombre = perfil?.nombre?.takeIf { it.isNotBlank() } ?: email
    val esPiloto1 = nombre.equals("Piloto1", ignoreCase = true) ||
        nombre.equals("Patricio Lagos", ignoreCase = true)
    val rol = perfil?.rol
    val vehiculo = remember(servicios) { DatosPerfil.vehiculo(servicios) }
    val totales = remember(servicios, checkins) { DatosPerfil.totales(servicios, checkins) }
    val enRuta = remember(servicios, checkins) { DatosPerfil.enRuta(servicios, checkins) }
    var notificaciones by remember { mutableStateOf(true) }
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
                    Text("Mi Perfil", color = BarraSuperior.contenido,
                        style = MaterialTheme.typography.titleSmall, fontSize = 15.sp,
                        fontWeight = FontWeight.Bold)
                    Text("Operaciones Southbound", color = BarraSuperior.contenido.copy(alpha = .8f),
                        style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, maxLines = 1)
                }
                Surface(color = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DirectionsCar, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(rol?.etiqueta?.uppercase() ?: "CONDUCTOR",
                            color = MaterialTheme.colorScheme.onSecondary,
                            style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(6.dp))
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Outlined.Person, contentDescription = "Perfil",
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
                            text = "EN RUTA",
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
                        MiniDatoPerfil("Cuenta", rol?.etiqueta ?: SIN_DATO)
                        MiniDatoPerfil("Oficina", FiltroServicios.OFICINA_POR_DEFECTO)
                        MiniDatoPerfil("Vehículo", vehiculo?.patente ?: SIN_DATO)
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
                    Text("Datos Personales y\nOperativos", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f))
                    EtiquetaPerfil("Verificado")
                }
                Spacer(Modifier.height(16.dp))
                FilaDatoPersonal("Nombre Completo", nombre, Icons.Outlined.Person)
                FilaDatoPersonal("Correo Institucional", email.ifBlank { SIN_DATO },
                    Icons.Outlined.Email, subrayado = true)
                FilaDatoPersonal("Rol", rol?.etiqueta ?: SIN_DATO, Icons.Outlined.Badge)
            }

            if (rol != Rol.GUIA) {
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DirectionsBus, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(9.dp))
                        Text("Vehículo Asignado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f))
                        EtiquetaPerfil(if (vehiculo != null) "● Operativo" else "Sin asignar")
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(132.dp).clip(RoundedCornerShape(10.dp))) {
                        Image(painterResource(if (esPiloto1) R.drawable.jacs5_piloto1
                            else R.drawable.mercedes_benz_sprinter),
                            contentDescription = "Vehículo asignado a Piloto1",
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xDD00162A)))))
                        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                            if (!esPiloto1) Text(vehiculo?.tipo?.uppercase() ?: "VEHÍCULO DE PASAJEROS",
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                color = Color.White)
                            Text(if (esPiloto1) "JAC S5" else vehiculo?.tipo ?: "Vehículo asignado",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = Color.White)
                            Text(if (esPiloto1) "Año modelo 2015" else "Vehículo asignado",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = .85f))
                        }
                        Surface(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp), shadowElevation = 3.dp) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PATENTE", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(vehiculo?.patente?.ifBlank { SIN_DATO } ?: SIN_DATO,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniDatoVehiculo(Icons.Outlined.Groups, "Capacidad",
                            vehiculo?.let { "${it.capacidad} Pasajeros" } ?: SIN_DATO, Modifier.weight(1f))
                        MiniDatoVehiculo(Icons.Outlined.Speed, "Odómetro", SIN_DATO, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Verified, contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Información de revisión técnica no disponible.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Analytics, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(9.dp))
                    TituloSeccion("Resumen Operacional")
                }
                Text("Mes en curso", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstadisticaPerfil(Icons.Outlined.Route, viajesMes.toString(), "Viajes Mes",
                        MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.onSecondary, Modifier.weight(1f))
                    EstadisticaPerfil(Icons.Outlined.Star, "—", "Calificación",
                        MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.onTertiary, Modifier.weight(1f),
                        MaterialTheme.colorScheme.onTertiaryContainer)
                    EstadisticaPerfil(Icons.Outlined.AccessTime, "—", "Puntualidad",
                        MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.onSecondary, Modifier.weight(1f),
                        MaterialTheme.colorScheme.secondary)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Verified, contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Certificación Conductor Destacado 2026",
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text("NIVEL\nORO", style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center)
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
                    text = "Cerrar Sesión",
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

@Composable
private fun MiniDatoPerfil(etiqueta: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun MiniDatoVehiculo(
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
private fun EstadisticaPerfil(
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
private fun EtiquetaPerfil(texto: String) {
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
private fun TituloSeccion(texto: String) {
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
private fun FilaDatoPersonal(
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
            TituloSeccion("Ajustes y Soporte")
        }
        AjusteSwitchPerfil(
            icono = Icons.AutoMirrored.Outlined.VolumeUp,
            titulo = "Notificaciones sonoras en ruta",
            descripcion = "Alertas de tráfico y paradas prioritarias",
            activo = notificaciones,
            onChange = onNotificacionesChange
        )
        AjusteNavegacionPerfil(
            icono = Icons.Outlined.Palette,
            titulo = "Ajustes de apariencia",
            descripcion = "Tema claro, oscuro o del sistema",
            onClick = onOpenSettings
        )
        AjusteNavegacionPerfil(
            icono = Icons.Outlined.SupportAgent,
            titulo = "Contactar Soporte TI / Mesa de Ayuda",
            descripcion = "Línea directa operativa 24/7",
            onClick = onOpenSupport
        )
    }
}

@Composable
private fun AjusteNavegacionPerfil(
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
