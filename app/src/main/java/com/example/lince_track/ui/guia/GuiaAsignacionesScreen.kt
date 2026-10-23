package com.example.lince_track.ui.guia

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.Composable
import com.example.lince_track.ui.components.PantallaEnConstruccion

/**
 * Lista de excursiones/traslados asignados al guía logueado.
 *
 * TODO: implementar el listado filtrado por el nombre/ID del guía (campo `guia`
 * de Servicio). Se puede reutilizar la lógica de ServiciosScreen y, al tocar un
 * item, navegar a Routes.detalleServicio(id) reutilizando DetalleServicioScreen.
 */
@Composable
fun GuiaAsignacionesScreen() {
    PantallaEnConstruccion(
        titulo = "Mis Asignaciones",
        descripcion = "Aquí se listarán las excursiones y traslados asignados a este guía.",
        icono = Icons.Outlined.Home
    )
}
