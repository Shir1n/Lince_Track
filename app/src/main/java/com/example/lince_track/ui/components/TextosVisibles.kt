package com.example.lince_track.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.example.lince_track.data.TextosServicio

/** Nombre de la actividad en el idioma de la app (los nombres de lugares quedan igual). */
@Composable
fun tituloVisible(titulo: String): String = TextosServicio.titulo(titulo, LocalConfiguration.current.locales[0])

/** Nombre de la persona; "Sin guía", "Sin asignar" y similares se muestran en el idioma de la app. */
@Composable
fun nombreVisible(nombre: String): String = TextosServicio.nombre(nombre).asString()
