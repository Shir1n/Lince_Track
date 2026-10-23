package com.example.lince_track.ui.ruta

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lince_track.data.clima.ClimaInfo
import com.example.lince_track.data.clima.NivelRiesgo
import com.example.lince_track.data.clima.PresetClima
import com.example.lince_track.data.clima.RecomendacionConduccion
import com.example.lince_track.data.clima.TipoClima
import kotlin.math.roundToInt

@Composable
fun ClimaConduccionCard(
    clima: ClimaInfo,
    recomendacion: RecomendacionConduccion,
    presetActual: PresetClima,
    cargando: Boolean,
    onPresetSelected: (PresetClima) -> Unit,
    onRefrescarGps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var showPresetDialog by remember { mutableStateOf(false) }

    // Fondo dinámico según tipo de clima con animación suave
    val targetBgColor = if (isDark) {
        when (clima.tipo) {
            TipoClima.LLUVIA_FUERTE, TipoClima.LLUVIA_MODERADA, TipoClima.LLUVIA_LIGERA -> Color(0xFF233649)
            TipoClima.NIEBLA -> Color(0xFF2C343D)
            TipoClima.VIENTO_FUERTE -> Color(0xFF1E3A3F)
            TipoClima.TORMENTA -> Color(0xFF3F2327)
            TipoClima.NIEVE -> Color(0xFF203746)
            TipoClima.DESPEJADO, TipoClima.PARCIALMENTE_NUBLADO -> Color(0xFF3A311F)
            TipoClima.NUBLADO -> Color(0xFF29323A)
        }
    } else {
        when (clima.tipo) {
            TipoClima.LLUVIA_FUERTE, TipoClima.LLUVIA_MODERADA, TipoClima.LLUVIA_LIGERA -> Color(0xFF93B3D2)
            TipoClima.NIEBLA -> Color(0xFFB5BDC6)
            TipoClima.VIENTO_FUERTE -> Color(0xFF87B8BE)
            TipoClima.TORMENTA -> Color(0xFFD69994)
            TipoClima.NIEVE -> Color(0xFFA6C5D6)
            TipoClima.DESPEJADO, TipoClima.PARCIALMENTE_NUBLADO -> Color(0xFFE4C586)
            TipoClima.NUBLADO -> Color(0xFFB5BFCA)
        }
    }

    val animatedBg by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 400),
        label = "climaBgAnimation"
    )

    val contentColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val secondaryTextColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF1E293B)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = animatedBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Fila superior: Ilustración vectorial + Temperatura + Estado + Botón simular
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ilustración vectorial pura (sin emojis)
                ClimaVectorIlustracion(
                    tipo = clima.tipo,
                    modifier = Modifier.size(68.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${clima.tempC.roundToInt()}°C",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = contentColor
                        )
                        Text(
                            text = "• ${clima.tipo.label}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = secondaryTextColor,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Text(
                        text = "${clima.descripcionCorta} • Viento ${clima.windSpeedKmH.roundToInt()} km/h",
                        fontSize = 12.sp,
                        color = secondaryTextColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Botón discreto para abrir presets de prueba
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (presetActual == PresetClima.REAL) {
                        IconButton(
                            onClick = onRefrescarGps,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refrescar GPS",
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = contentColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { showPresetDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "Simular",
                                tint = contentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (presetActual == PresetClima.REAL) "Real" else "Simulado",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Caja de recomendación personalizada con nivel de alerta
            val alertBadgeBg = when (recomendacion.nivel) {
                NivelRiesgo.PELIGRO -> Color(0xFFDC2626)
                NivelRiesgo.PRECAUCION -> Color(0xFFD97706)
                NivelRiesgo.OPTIMO -> Color(0xFF059669)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = contentColor.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = contentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(10.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(alertBadgeBg, CircleShape)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = recomendacion.nivel.label.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = recomendacion.alertaCorta,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = recomendacion.mensajePersonalizado,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    recomendacion.consejos.take(2).forEach { consejo ->
                        Row(
                            modifier = Modifier.padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(contentColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = consejo,
                                fontSize = 11.sp,
                                color = secondaryTextColor
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPresetDialog) {
        ClimaPresetDialog(
            presetActual = presetActual,
            onDismiss = { showPresetDialog = false },
            onSelectPreset = { preset ->
                onPresetSelected(preset)
                showPresetDialog = false
            }
        )
    }
}

/**
 * Dibuja de manera 100% vectorial la ilustración meteorológica (cero emojis).
 */
@Composable
private fun ClimaVectorIlustracion(
    tipo: TipoClima,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (tipo) {
            TipoClima.DESPEJADO -> {
                drawSun(w, h)
            }
            TipoClima.PARCIALMENTE_NUBLADO -> {
                drawSun(w * 0.9f, h * 0.9f, offset = Offset(w * 0.1f, 0f))
                drawCloud(w, h, cloudColor = Color.White.copy(alpha = 0.9f))
            }
            TipoClima.NUBLADO -> {
                drawCloud(w * 0.85f, h * 0.85f, cloudColor = Color(0xFFCBD5E1), offset = Offset(w * 0.15f, 0f))
                drawCloud(w, h, cloudColor = Color.White.copy(alpha = 0.95f))
            }
            TipoClima.NIEBLA -> {
                drawCloud(w, h * 0.85f, cloudColor = Color.White.copy(alpha = 0.85f))
                drawFogLines(w, h)
            }
            TipoClima.LLUVIA_LIGERA -> {
                drawCloud(w, h * 0.8f, cloudColor = Color.White.copy(alpha = 0.95f))
                drawRainDrops(w, h, count = 3)
            }
            TipoClima.LLUVIA_MODERADA, TipoClima.LLUVIA_FUERTE -> {
                drawCloud(w, h * 0.8f, cloudColor = Color(0xFFE2E8F0))
                drawRainDrops(w, h, count = 5)
            }
            TipoClima.TORMENTA -> {
                drawCloud(w, h * 0.8f, cloudColor = Color(0xFF64748B))
                drawLightning(w, h)
                drawRainDrops(w, h, count = 4)
            }
            TipoClima.NIEVE -> {
                drawCloud(w, h * 0.8f, cloudColor = Color.White.copy(alpha = 0.95f))
                drawSnowDots(w, h)
            }
            TipoClima.VIENTO_FUERTE -> {
                drawWindGusts(w, h)
            }
        }
    }
}

private fun DrawScope.drawSun(w: Float, h: Float, offset: Offset = Offset.Zero) {
    val center = Offset(w * 0.5f + offset.x, h * 0.5f + offset.y)
    val radius = w * 0.26f

    // Rayos
    val rayColor = Color(0xFFF59E0B)
    for (i in 0 until 8) {
        val angle = Math.toRadians((i * 45).toDouble())
        val startX = center.x + (radius + 4f) * Math.cos(angle).toFloat()
        val startY = center.y + (radius + 4f) * Math.sin(angle).toFloat()
        val endX = center.x + (radius + 12f) * Math.cos(angle).toFloat()
        val endY = center.y + (radius + 12f) * Math.sin(angle).toFloat()
        drawLine(
            color = rayColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
    }

    // Sol central
    drawCircle(
        color = Color(0xFFFBBF24),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawCloud(
    w: Float,
    h: Float,
    cloudColor: Color,
    offset: Offset = Offset.Zero
) {
    val ox = offset.x
    val oy = offset.y

    // Base redondeada
    drawRoundRect(
        color = cloudColor,
        topLeft = Offset(ox + w * 0.15f, oy + h * 0.35f),
        size = Size(w * 0.70f, h * 0.32f),
        cornerRadius = CornerRadius(h * 0.16f, h * 0.16f)
    )
    // Cúpula izquierda
    drawCircle(
        color = cloudColor,
        radius = w * 0.16f,
        center = Offset(ox + w * 0.35f, oy + h * 0.36f)
    )
    // Cúpula central/alta
    drawCircle(
        color = cloudColor,
        radius = w * 0.21f,
        center = Offset(ox + w * 0.55f, oy + h * 0.30f)
    )
    // Cúpula derecha
    drawCircle(
        color = cloudColor,
        radius = w * 0.14f,
        center = Offset(ox + w * 0.72f, oy + h * 0.38f)
    )
}

private fun DrawScope.drawRainDrops(w: Float, h: Float, count: Int) {
    val dropColor = Color(0xFF38BDF8)
    val step = (w * 0.60f) / (count + 1)
    val startX = w * 0.20f

    for (i in 1..count) {
        val x = startX + i * step
        val y = h * 0.74f + (if (i % 2 == 0) 4f else -2f)
        drawLine(
            color = dropColor,
            start = Offset(x, y),
            end = Offset(x - 4f, y + 14f),
            strokeWidth = 3.2f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawFogLines(w: Float, h: Float) {
    val fogColor = Color.White.copy(alpha = 0.85f)
    drawLine(
        color = fogColor,
        start = Offset(w * 0.15f, h * 0.72f),
        end = Offset(w * 0.85f, h * 0.72f),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = fogColor,
        start = Offset(w * 0.22f, h * 0.82f),
        end = Offset(w * 0.78f, h * 0.82f),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawWindGusts(w: Float, h: Float) {
    val windColor = Color(0xFF0284C7)
    drawLine(
        color = windColor,
        start = Offset(w * 0.15f, h * 0.35f),
        end = Offset(w * 0.85f, h * 0.35f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = windColor,
        start = Offset(w * 0.10f, h * 0.52f),
        end = Offset(w * 0.90f, h * 0.52f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = windColor,
        start = Offset(w * 0.25f, h * 0.70f),
        end = Offset(w * 0.75f, h * 0.70f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawLightning(w: Float, h: Float) {
    val path = Path().apply {
        moveTo(w * 0.52f, h * 0.50f)
        lineTo(w * 0.44f, h * 0.68f)
        lineTo(w * 0.54f, h * 0.68f)
        lineTo(w * 0.46f, h * 0.92f)
    }
    drawPath(
        path = path,
        color = Color(0xFFFACC15),
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawSnowDots(w: Float, h: Float) {
    val snowColor = Color.White
    val points = listOf(
        Offset(w * 0.25f, h * 0.75f),
        Offset(w * 0.42f, h * 0.82f),
        Offset(w * 0.58f, h * 0.73f),
        Offset(w * 0.75f, h * 0.80f)
    )
    points.forEach { pt ->
        drawCircle(color = snowColor, radius = 3.5f, center = pt)
    }
}

@Composable
fun ClimaPresetDialog(
    presetActual: PresetClima,
    onDismiss: () -> Unit,
    onSelectPreset: (PresetClima) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Modo de Clima y Evaluación",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Selecciona cómo obtener el clima para probar en vivo las recomendaciones viales:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PresetClima.entries.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPreset(preset) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preset == presetActual,
                            onClick = { onSelectPreset(preset) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (preset == presetActual) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
