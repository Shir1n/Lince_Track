package com.example.lince_track.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration

enum class LogLevel(val label: String) {
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERROR")
}

data class LogEntry(
    val id: Long,
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val details: String? = null
)

object LinceLogger {
    private const val LOGCAT_PREFIX = "LINCE_LOG"
    private const val MAX_LOGS = 250
    private val counter = AtomicLong(1L)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun i(tag: String, message: String, details: String? = null) {
        append(LogLevel.INFO, tag, message, details)
        Log.i("$LOGCAT_PREFIX-$tag", if (details != null) "$message | $details" else message)
    }

    fun w(tag: String, message: String, details: String? = null) {
        append(LogLevel.WARN, tag, message, details)
        Log.w("$LOGCAT_PREFIX-$tag", if (details != null) "$message | $details" else message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null, details: String? = null) {
        val fullDetails = buildString {
            if (!details.isNullOrBlank()) append(details)
            if (throwable != null) {
                if (isNotEmpty()) append("\n")
                append("${throwable.javaClass.simpleName}: ${throwable.message}\n")
                append(throwable.stackTrace.take(5).joinToString("\n") { "  at $it" })
            }
        }.ifBlank { null }

        append(LogLevel.ERROR, tag, message, fullDetails)
        Log.e("$LOGCAT_PREFIX-$tag", if (fullDetails != null) "$message | $fullDetails" else message, throwable)
    }

    private fun append(level: LogLevel, tag: String, message: String, details: String?) {
        val entry = LogEntry(
            id = counter.getAndIncrement(),
            timestamp = LocalTime.now().format(timeFormatter),
            level = level,
            tag = tag.uppercase(),
            message = message,
            details = details
        )
        _logs.update { current ->
            (listOf(entry) + current).take(MAX_LOGS)
        }
    }

    fun clear() {
        _logs.value = emptyList()
        i("SISTEMA", "Historial de consola limpiado por el usuario")
    }

    fun exportAsText(): String {
        val list = _logs.value
        if (list.isEmpty()) return "Sin registros en LinceLogger."
        return list.reversed().joinToString("\n") { entry ->
            val base = "[${entry.timestamp}] [${entry.level.label}] [${entry.tag}] ${entry.message}"
            if (entry.details != null) "$base\n    -> ${entry.details}" else base
        }
    }

    suspend fun runMapTileDiagnostic() {
        withContext(Dispatchers.IO) {
            val cfg = Configuration.getInstance()
            i(
                tag = "DIAG",
                message = "Iniciando diagnóstico de configuración osmdroid",
                details = "userAgent='${cfg.userAgentValue}', normalizedUA='${cfg.normalizedUserAgent}', cacheDir='${cfg.osmdroidTileCache?.absolutePath}'"
            )

            val testEndpoints = listOf(
                "OSM-DE (FOSSGIS)" to "https://tile.openstreetmap.de/13/2435/5131.png",
                "OSM-ORG (Principal)" to "https://tile.openstreetmap.org/13/2435/5131.png"
            )

            for ((name, urlStr) in testEndpoints) {
                var conn: HttpURLConnection? = null
                try {
                    val startMs = System.currentTimeMillis()
                    conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 6000
                        readTimeout = 6000
                        setRequestProperty(cfg.userAgentHttpHeader, cfg.userAgentValue)
                    }
                    conn.connect()
                    val code = conn.responseCode
                    val contentType = conn.contentType ?: "desconocido"
                    val contentLength = conn.contentLength
                    val xBlocked = conn.getHeaderField("x-blocked")
                    val xTileRender = conn.getHeaderField("x-tilerender")
                    val elapsedMs = System.currentTimeMillis() - startMs

                    val detailText = "HTTP $code | ${elapsedMs}ms | type=$contentType | bytes=$contentLength | x-blocked=${xBlocked ?: "ninguno"} | render=${xTileRender ?: "ok"}"
                    if (code == 200 && xBlocked == null && contentLength != 6996) {
                        i("RED", "Prueba exitosa en $name", detailText)
                    } else {
                        w("RED", "Bloqueo o advertencia en $name", detailText)
                    }
                } catch (ex: Exception) {
                    e("RED", "Fallo de conexión en $name ($urlStr)", ex)
                } finally {
                    conn?.disconnect()
                }
            }
        }
    }
}

@Composable
fun LinceLogConsoleDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allLogs by LinceLogger.logs.collectAsState()
    var selectedCategory by remember { mutableStateOf("TODOS") }
    var isRunningTest by remember { mutableStateOf(false) }

    val categories = listOf("TODOS", "MAPA", "GPS", "RED", "NAV", "ERROR")

    val filteredLogs = remember(allLogs, selectedCategory) {
        when (selectedCategory) {
            "TODOS" -> allLogs
            "ERROR" -> allLogs.filter { it.level == LogLevel.ERROR || it.level == LogLevel.WARN }
            else -> allLogs.filter { it.tag.contains(selectedCategory, ignoreCase = true) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Consola de Diagnóstico Lince",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "${allLogs.size} eventos registrados en sesión",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar consola",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { category ->
                        val selected = selectedCategory == category
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }
                }

                // Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isRunningTest) {
                                isRunningTest = true
                                scope.launch {
                                    LinceLogger.runMapTileDiagnostic()
                                    isRunningTest = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRunningTest) "Probando..." else "Probar Red / Mapa",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("LinceLogs", LinceLogger.exportAsText())
                            )
                            Toast.makeText(context, "Logs copiados al portapapeles", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Copiar logs",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copiar", style = MaterialTheme.typography.labelMedium)
                    }

                    IconButton(onClick = { LinceLogger.clear() }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Limpiar logs",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                // Log list
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay eventos para el filtro '$selectedCategory'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLogs, key = { it.id }) { entry ->
                            LogEntryItem(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryItem(entry: LogEntry) {
    val levelColor = when (entry.level) {
        LogLevel.INFO -> MaterialTheme.colorScheme.secondary
        LogLevel.WARN -> MaterialTheme.colorScheme.tertiary
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, levelColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(levelColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.level.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = levelColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.tag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = entry.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!entry.details.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.details,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
