package com.example.lince_track.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lince_track.BuildConfig
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.Rol
import com.example.lince_track.data.ServicioRepository
import com.example.lince_track.data.servicios.FiltroServicios
import com.example.lince_track.ui.components.AvisoServiciosCard
import com.example.lince_track.ui.components.SoporteDialog
import com.example.lince_track.ui.theme.BarraSuperior

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
    val rol = perfil?.rol
    val vehiculo = remember(servicios) { DatosPerfil.vehiculo(servicios) }
    val totales = remember(servicios, checkins) { DatosPerfil.totales(servicios, checkins) }
    val enRuta = remember(servicios, checkins) { DatosPerfil.enRuta(servicios, checkins) }

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
                text = "Mi Perfil",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            error?.let { AvisoServiciosCard(it) }

            Tarjeta {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
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
                }
            }

            Tarjeta {
                TituloSeccion("Datos de la cuenta")
                FilaDato("Nombre", perfil?.nombre?.takeIf { it.isNotBlank() } ?: SIN_DATO)
                FilaDato("Correo", email.ifBlank { SIN_DATO })
                FilaDato("Rol", rol?.etiqueta ?: SIN_DATO)
                FilaDato("Oficina", FiltroServicios.OFICINA_POR_DEFECTO)
            }

            if (rol != Rol.GUIA) {
                Tarjeta {
                    TituloSeccion("Vehículo asignado")
                    if (vehiculo == null) {
                        Text(
                            text = "Sin vehículo asignado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FilaDato("Patente", vehiculo.patente.ifBlank { SIN_DATO })
                        FilaDato("Tipo", vehiculo.tipo.ifBlank { SIN_DATO })
                        FilaDato("Capacidad", "${vehiculo.capacidad} pasajeros")
                    }
                }
            }

            Tarjeta {
                TituloSeccion("Resumen")
                Text(
                    text = "Últimos 7 días y próximos 30",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Total("Servicios", totales.servicios, Modifier.weight(1f))
                    Total("Aceptados", totales.aceptados, Modifier.weight(1f))
                    Total("Finalizados", totales.finalizados, Modifier.weight(1f))
                }
            }

            Tarjeta {
                TituloSeccion("Ajustes y soporte")
                FilaAccion(Icons.Outlined.Palette, "Ajustes de apariencia", onOpenSettings)
                FilaAccion(Icons.Outlined.SupportAgent, "Contactar soporte") { mostrarSoporte = true }
            }

            Button(
                onClick = onLogout,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "Cerrar Sesión",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError
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
