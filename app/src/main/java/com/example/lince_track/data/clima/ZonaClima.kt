package com.example.lince_track.data.clima

import java.util.Locale

/**
 * Zona de unos 1 km alrededor de una posición. El clima solo se vuelve a consultar al cambiar
 * de zona, no en cada lectura de GPS.
 */
fun zonaClima(lat: Double, lon: Double): String = "%.2f,%.2f".format(Locale.US, lat, lon)
