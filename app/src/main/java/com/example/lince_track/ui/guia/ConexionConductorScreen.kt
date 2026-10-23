package com.example.lince_track.ui.guia

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.runtime.Composable
import com.example.lince_track.ui.components.PantallaEnConstruccion

/**
 * Conexión con el conductor asignado: ubicación en tiempo real, estado del viaje
 * y mensajería entre el guía y el conductor.
 *
 * TODO: implementar el mix de:
 *  - Ubicación del conductor en el mapa (check-in GPS).
 *  - Estado del viaje (en ruta, llegó, retrasado, etc.).
 *  - Mensajería/chat simple con el conductor.
 */
@Composable
fun ConexionConductorScreen() {
    PantallaEnConstruccion(
        titulo = "Conexión Conductor",
        descripcion = "Aquí podrás ver la ubicación del conductor, el estado del viaje y comunicarte con él.",
        icono = Icons.Outlined.LocationOn
    )
}
