
package com.example.lince_track.ui.perfil

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lince_track.R

private val Azul = Color(0xFF062344)
private val AzulTexto = Color(0xFF102F52)
private val Celeste = Color(0xFF087F9D)
private val Fondo = Color(0xFFF6F7FC)
private val TarjetaAzul = Color(0xFFEEF3FF)
private val Turquesa = Color(0xFF75EBDD)
private val Verde = Color(0xFF006D63)
private val Gris = Color(0xFF647080)
private val Naranja = Color(0xFFFFE8D3)
private val RojoClaro = Color(0xFFFFDAD6)

@Composable
fun PerfilScreen(
    onLogout: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var notificaciones by remember { mutableStateOf(true) }
    var compartirGps by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = Fondo,
        topBar = { PerfilTopBar() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ResumenPerfil()
            DatosPersonales()
            VehiculoAsignado()
            ResumenOperacional()
            AjustesSeguridad(
                notificaciones = notificaciones,
                onNotificacionesChange = { notificaciones = it },
                compartirGps = compartirGps,
                onGpsChange = { compartirGps = it }
            )
            AjustesAparienciaCard(onClick = onOpenSettings)
            CerrarSesion(onLogout = onLogout)
            PieDePagina()
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PerfilTopBar() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Azul)
                .statusBarsPadding()
                .height(62.dp)
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(29.dp)
                    .background(Color.White, RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    tint = Celeste,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Mi Perfil",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Operaciones Southbound",
                    color = Color(0xFFB9C9DE),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }

            Surface(
                color = Color(0xFF087D76),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 5.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "TRANSPORTISTA",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(7.dp))

            Surface(
                shape = CircleShape,
                color = Color(0xFF183C62),
                modifier = Modifier.size(29.dp)
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = "Perfil",
                    tint = Color.White,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}

@Composable
private fun ResumenPerfil() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Image(
                painter = painterResource(R.drawable.perfil1),
                contentDescription = "Foto del conductor",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFFDDE6F0), CircleShape)
            )
            Surface(
                color = Verde,
                shape = CircleShape,
                modifier = Modifier.size(21.dp)
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Verificado",
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Carlos Morales R.",
            style = MaterialTheme.typography.titleLarge,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Transportista Oficial Southbound",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF374151)
        )

        Spacer(Modifier.height(10.dp))

        Surface(
            color = Turquesa,
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 4.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .background(Verde, CircleShape)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "EN TURNO ACTIVO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AzulTexto,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(Modifier.height(13.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(Fondo)
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            DatoPerfil(
                titulo = "RUT",
                valor = "15.842.309-K",
                modifier = Modifier.weight(1.2f)
            )
            DatoPerfil(
                titulo = "Base",
                valor = "Patagonia\nHub",
                modifier = Modifier.weight(1f)
            )
            DatoPerfil(
                titulo = "Flota",
                valor = "Sprinter 516",
                modifier = Modifier.weight(1.1f),
                color = Verde
            )
        }
    }
}

@Composable
private fun DatoPerfil(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Black
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            titulo,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B)
        )
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DatosPersonales() {
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoCircular(Icons.Outlined.Badge)
            Spacer(Modifier.width(9.dp))
            Text(
                "Datos Personales y\nOperativos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Etiqueta("Verificado", Turquesa)
        }

        Spacer(Modifier.height(16.dp))

        FilaDato(
            "Nombre Completo",
            "Carlos Morales Riquelme",
            Icons.Outlined.Person
        )
        FilaDato(
            "Correo Institucional",
            "cmorales@southbound.cl",
            Icons.Outlined.Email,
            subrayado = true
        )
        FilaDato(
            "Teléfono Institucional",
            "+56 9 7842 1190",
            Icons.Outlined.Phone
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(TarjetaAzul)
                .padding(9.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Outlined.DirectionsCar,
                contentDescription = null,
                tint = Verde,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(19.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "Licencia de Conducir",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Profesional A2",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(Modifier.width(5.dp))
                    Etiqueta("Clase A2", Turquesa)
                }
                Text(
                    "Vigencia hasta Noviembre 2028 (Al día)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun FilaDato(
    titulo: String,
    valor: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    subrayado: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                titulo,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B)
            )
            Text(
                valor,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                fontWeight = FontWeight.SemiBold
            )
            if (subrayado) {
                Spacer(Modifier.height(1.dp))
                Box(
                    Modifier
                        .width(145.dp)
                        .height(1.dp)
                        .background(Color.Black)
                )
            }
        }
        Icon(
            icono,
            contentDescription = null,
            tint = Gris,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun VehiculoAsignado() {
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoCircular(Icons.Outlined.DirectionsBus)
            Spacer(Modifier.width(9.dp))
            Text(
                "Vehículo Asignado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Etiqueta("● Operativo", Turquesa)
        }

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Image(
                painter = painterResource(R.drawable.mercedes_benz_sprinter),
                contentDescription = "Mercedes-Benz Sprinter en la Patagonia",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xDD00162A)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    "VAN DE PASAJEROS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Mercedes-Benz Sprinter\n516 CDI",
                    style = MaterialTheme.typography.titleMedium,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Año modelo 2023",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp),
                color = Color.White,
                shape = RoundedCornerShape(5.dp),
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 7.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "CL  LK · VF -",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF29384B)
                    )
                    Text(
                        "84",
                        style = MaterialTheme.typography.titleMedium,
                        color = Azul,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(13.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MiniDato(
                Icons.Outlined.Groups,
                "Capacidad",
                "19 Pasajeros",
                Modifier.weight(1f)
            )
            MiniDato(
                Icons.Outlined.Speed,
                "Odómetro",
                "64.280 km",
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(7.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE3EDFF))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Verified,
                contentDescription = null,
                tint = Verde,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Revisión técnica y homologación SERNATUR al día (Sep 2024).",
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 15.sp,
                color = Color(0xFF1E293B)
            )
        }
    }
}

@Composable
private fun MiniDato(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TarjetaAzul)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icono,
            contentDescription = null,
            tint = Verde,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                titulo,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B)
            )
            Text(
                valor,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ResumenOperacional() {
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoCircular(Icons.Outlined.Analytics)
            Spacer(Modifier.width(9.dp))
            Text(
                "Resumen Operacional",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Text(
                "Mes en curso",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B)
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Estadistica(
                Icons.Outlined.Route,
                "42",
                "Viajes Mes",
                TarjetaAzul,
                Modifier.weight(1f)
            )
            Estadistica(
                Icons.Outlined.Star,
                "4.9 ★",
                "Calificación",
                Naranja,
                Modifier.weight(1f)
            )
            Estadistica(
                Icons.Outlined.AccessTime,
                "98.4%",
                "Puntualidad",
                TarjetaAzul,
                Modifier.weight(1f),
                Verde
            )
        }

        Spacer(Modifier.height(9.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE3EDFF))
                .padding(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Verified,
                contentDescription = null,
                tint = Verde,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Certificación Conductor Destacado\n2024",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 15.sp,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Text(
                "NIVEL\nORO",
                style = MaterialTheme.typography.labelSmall,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Verde,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Estadistica(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    valor: String,
    titulo: String,
    fondo: Color,
    modifier: Modifier = Modifier,
    colorValor: Color = Color.Black
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(fondo)
            .padding(vertical = 9.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(25.dp)
                .clip(CircleShape)
                .background(
                    if (titulo == "Calificación") Color(0xFFFFD4AF)
                    else Turquesa
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = if (titulo == "Calificación") Color(0xFF74400E)
                else Verde,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            valor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = colorValor
        )
        Text(
            titulo,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF374151)
        )
    }
}

@Composable
private fun AjustesSeguridad(
    notificaciones: Boolean,
    onNotificacionesChange: (Boolean) -> Unit,
    compartirGps: Boolean,
    onGpsChange: (Boolean) -> Unit
) {
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoCircular(Icons.Outlined.Tune)
            Spacer(Modifier.width(9.dp))
            Text(
                "Ajustes y Seguridad",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(Modifier.height(13.dp))

        AjusteSwitch(
            Icons.AutoMirrored.Outlined.VolumeUp,
            "Notificaciones sonoras en ruta",
            "Alertas de tráfico y paradas prioritarias",
            notificaciones,
            onNotificacionesChange
        )
        AjusteSwitch(
            Icons.Outlined.GpsFixed,
            "Compartir GPS en tiempo real",
            "Requerido por protocolo Southbound",
            compartirGps,
            onGpsChange
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(TarjetaAzul)
                .clickable { }
                .padding(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Azul,
                shape = CircleShape,
                modifier = Modifier.size(27.dp)
            ) {
                Icon(
                    Icons.Outlined.SupportAgent,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(6.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Contactar Soporte TI / Mesa de Ayuda",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Text(
                    "Línea directa operativa 24/7",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Gris
            )
        }
    }
}

@Composable
private fun AjusteSwitch(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    descripcion: String,
    activo: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icono,
            contentDescription = null,
            tint = AzulTexto,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                titulo,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Text(
                descripcion,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 15.sp,
                color = Color(0xFF64748B)
            )
        }
        Switch(
            checked = activo,
            onCheckedChange = onChange,
            modifier = Modifier.height(30.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Verde,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}

@Composable
private fun AjustesAparienciaCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Ajustes de apariencia",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CerrarSesion(onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(43.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(RojoClaro)
            .clickable { onLogout() }
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.Logout,
            contentDescription = null,
            tint = Color(0xFFAC1919),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            "Cerrar Sesión",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFAC1919)
        )
    }
}

@Composable
private fun PieDePagina() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Lince Track v2.4.1 (Build 8904)",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B)
        )
        Text(
            "Operaciones Cono Sur · Southbound Logistics",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Tarjeta(
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White)
            .padding(14.dp),
        content = contenido
    )
}

@Composable
private fun IconoCircular(
    icono: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(Color(0xFFE7EBF0)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icono,
            contentDescription = null,
            tint = AzulTexto,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun Etiqueta(
    texto: String,
    color: Color
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = AzulTexto,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 3.dp
            )
        )
    }
}
