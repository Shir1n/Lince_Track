package com.example.lince_track.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lince_track.data.EstadoServicio

@Composable
fun EstadoServicioBadge(estado: EstadoServicio) {
    val style = when (estado) {
        EstadoServicio.PENDIENTE -> EstadoBadgeStyle(
            container = MaterialTheme.colorScheme.tertiary,
            content = MaterialTheme.colorScheme.onTertiary,
            icon = Icons.Outlined.Schedule,
            label = "Pendiente"
        )
        EstadoServicio.ACEPTADO -> EstadoBadgeStyle(
            container = MaterialTheme.colorScheme.secondary,
            content = MaterialTheme.colorScheme.onSecondary,
            icon = Icons.Filled.CheckCircle,
            label = "Aceptado"
        )
        EstadoServicio.RECHAZADO -> EstadoBadgeStyle(
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = Icons.Outlined.Close,
            label = "Rechazado"
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(style.container, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            tint = style.content,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = style.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = style.content
        )
    }
}

private data class EstadoBadgeStyle(
    val container: Color,
    val content: Color,
    val icon: ImageVector,
    val label: String
)
