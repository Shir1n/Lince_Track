package com.example.lince_track.ui.guia

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.lince_track.R
import com.example.lince_track.ui.components.PantallaEnConstruccion

@Composable
fun ConexionConductorScreen() {
    PantallaEnConstruccion(
        titulo = stringResource(R.string.guia_conexion_titulo),
        descripcion = stringResource(R.string.guia_conexion_desc),
        icono = Icons.Outlined.LocationOn
    )
}
