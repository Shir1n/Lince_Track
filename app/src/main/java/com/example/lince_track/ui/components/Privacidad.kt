package com.example.lince_track.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.lince_track.data.privacidad.PrivacidadRepository
import com.example.lince_track.util.PERMISOS_UBICACION
import com.example.lince_track.util.tienePermisoUbicacion
import kotlinx.coroutines.launch

/** Cada sección del aviso: título y texto. Está aparte para poder probarlo y para cambiarlo en un solo lugar. */
internal val SECCIONES_AVISO_PRIVACIDAD = listOf(
    "Qué datos usamos" to
        "Tu nombre, correo y teléfono de la cuenta; los servicios que te asigna Southbound; tus respuestas " +
        "(aceptar o rechazar, con su motivo), tus incidencias y la ubicación de tu check-in, check-out e " +
        "incidencias.",
    "Para qué" to
        "Para coordinar los servicios, comprobar la llegada al punto de encuentro y la puntualidad, y atender " +
        "las incidencias.",
    "Quién los ve" to
        "Coordinación (administración) ve todo lo anterior. La otra persona de tu tour ve tu nombre, tu teléfono " +
        "y si aceptaste. Tu ubicación, tus motivos de rechazo y tus incidencias solo los ve Coordinación.",
    "Tu ubicación" to
        "Mientras tienes abierto el mapa de un servicio, el GPS se lee cada pocos segundos para mostrarte tu " +
        "posición, la distancia al punto de encuentro y tu velocidad. Eso queda solo en tu teléfono y no se " +
        "guarda ni se envía. Solo se envía la ubicación del momento del check-in, el check-out o una incidencia. " +
        "Al salir del mapa el GPS se detiene: no te seguimos en segundo plano ni fuera de un servicio.",
    "Dónde se guardan" to
        "En Firebase (Google), cuyos servidores pueden estar fuera de Chile, y en el servidor del proyecto. Se " +
        "guardan mientras dure el proyecto. Para mostrar el clima del mapa se consulta a Open-Meteo solo la " +
        "zona aproximada (unos 1 km), no tu posición exacta.",
    "Tus derechos" to
        "Puedes pedir a Coordinación ver, corregir o eliminar tus datos, u oponerte a su uso.",
    "Proyecto académico" to
        "Lince Track es un prototipo académico (Duoc UC, DSY1105). Los servicios, pasajeros y agencias que " +
        "muestra son ficticios."
)

/**
 * Aviso de privacidad. [primeraVez] lo muestra al abrir la app hasta que la persona pulsa "Entendido"; desde
 * Ajustes se puede volver a leer y cerrar tocando fuera.
 */
@Composable
fun AvisoPrivacidadDialog(primeraVez: Boolean, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!primeraVez) onCerrar() },
        properties = DialogProperties(dismissOnBackPress = !primeraVez, dismissOnClickOutside = !primeraVez),
        shape = RoundedCornerShape(28.dp),
        icon = { Icon(Icons.Outlined.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Tus datos en Lince Track", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SECCIONES_AVISO_PRIVACIDAD.forEach { (titulo, texto) ->
                    Column {
                        Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Entendido") } }
    )
}

/** Muestra el aviso la primera vez que se abre la app (o cuando cambia su versión). */
@Composable
fun AvisoPrivacidadAlIniciar() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mostrar by PrivacidadRepository.debeMostrarAviso(context).collectAsState(initial = false)
    if (mostrar) {
        AvisoPrivacidadDialog(primeraVez = true) {
            scope.launch { PrivacidadRepository.marcarAvisoVisto(context) }
        }
    }
}

/** Explicación propia que se muestra antes del permiso de Android, para que la persona sepa para qué se pide. */
@Composable
fun ExplicacionUbicacionDialog(motivo: String, onContinuar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        shape = RoundedCornerShape(28.dp),
        icon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Permiso de ubicación", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "$motivo\n\nSolo con la app abierta: no te seguimos en segundo plano. Lo que se envía (check-in, " +
                    "check-out e incidencias) solo lo ve Coordinación."
            )
        },
        confirmButton = { TextButton(onClick = onContinuar) { Text("Continuar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Ahora no") } }
    )
}

/**
 * Devuelve una función que pide la ubicación: si ya hay permiso responde al tiro; si no, primero muestra
 * [ExplicacionUbicacionDialog] con [motivo] y después el permiso de Android. [onResultado] recibe si quedó permitido.
 */
@Composable
fun rememberPermisoUbicacion(motivo: String, onResultado: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    var explicar by remember { mutableStateOf(false) }
    val pedirAndroid = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        onResultado(r.values.any { it })
    }
    if (explicar) {
        ExplicacionUbicacionDialog(
            motivo = motivo,
            onContinuar = {
                explicar = false
                pedirAndroid.launch(PERMISOS_UBICACION)
            },
            onCancelar = {
                explicar = false
                onResultado(false)
            }
        )
    }
    return { if (tienePermisoUbicacion(context)) onResultado(true) else explicar = true }
}
