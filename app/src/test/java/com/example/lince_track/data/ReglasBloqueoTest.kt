package com.example.lince_track.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Reglas del bloqueo de días: 7 días de anticipación (o lo que diga coordinación) y nunca sobre un servicio. */
class ReglasBloqueoTest {

    private val hoy = LocalDate.of(2026, 11, 20)
    private val diaConServicio = LocalDate.of(2026, 11, 28)
    private val servicio = Datos.servicio(3008).copy(fecha = diaConServicio)
    private val bloqueo = Bloqueo(id = 1, uid = "uid", fecha = LocalDate.of(2026, 12, 5))

    @Test
    fun elPrimerDiaBloqueableEsHoyMasLaAnticipacion() {
        assertEquals(LocalDate.of(2026, 11, 27), ReglasBloqueo.primerDiaBloqueable(hoy, 7))
        assertEquals(LocalDate.of(2026, 11, 30), ReglasBloqueo.primerDiaBloqueable(hoy, 10))
    }

    @Test
    fun soloSePuedeBloquearConLaAnticipacionMinima() {
        assertFalse(ReglasBloqueo.tieneAnticipacion(hoy, hoy, 7))
        assertFalse(ReglasBloqueo.tieneAnticipacion(LocalDate.of(2026, 11, 23), hoy, 7)) // a 3 días
        assertFalse(ReglasBloqueo.tieneAnticipacion(LocalDate.of(2026, 11, 26), hoy, 7))
        assertTrue(ReglasBloqueo.tieneAnticipacion(LocalDate.of(2026, 11, 27), hoy, 7))
        assertTrue(ReglasBloqueo.tieneAnticipacion(LocalDate.of(2026, 12, 15), hoy, 7))
        // Si coordinación sube la anticipación, el 27-11 deja de servir.
        assertFalse(ReglasBloqueo.tieneAnticipacion(LocalDate.of(2026, 11, 27), hoy, 10))
    }

    @Test
    fun noCuentanLosServiciosCanceladosNiReasignados() {
        assertTrue(ReglasBloqueo.tieneServicioAsignado(listOf(servicio), diaConServicio))
        assertFalse(ReglasBloqueo.tieneServicioAsignado(listOf(servicio), diaConServicio.plusDays(1)))
        assertFalse(ReglasBloqueo.tieneServicioAsignado(listOf(servicio.copy(cancelado = true)), diaConServicio))
        assertFalse(ReglasBloqueo.tieneServicioAsignado(listOf(servicio.copy(reasignado = true)), diaConServicio))
        assertFalse(ReglasBloqueo.tieneServicioAsignado(emptyList(), diaConServicio))
    }

    @Test
    fun reconoceLosDiasYaBloqueados() {
        assertTrue(ReglasBloqueo.estaBloqueado(LocalDate.of(2026, 12, 5), listOf(bloqueo)))
        assertFalse(ReglasBloqueo.estaBloqueado(LocalDate.of(2026, 12, 6), listOf(bloqueo)))
        assertFalse(ReglasBloqueo.estaBloqueado(LocalDate.of(2026, 12, 5), emptyList()))
    }

    @Test
    fun unDiaEsBloqueableSoloSiCumpleLasTresReglas() {
        val servicios = listOf(servicio)
        val bloqueos = listOf(bloqueo)
        assertTrue(ReglasBloqueo.esDiaBloqueable(LocalDate.of(2026, 11, 30), servicios, bloqueos, hoy, 7))
        assertFalse(ReglasBloqueo.esDiaBloqueable(LocalDate.of(2026, 11, 23), servicios, bloqueos, hoy, 7))
        assertFalse(ReglasBloqueo.esDiaBloqueable(diaConServicio, servicios, bloqueos, hoy, 7))
        assertFalse(ReglasBloqueo.esDiaBloqueable(LocalDate.of(2026, 12, 5), servicios, bloqueos, hoy, 7))
    }
}
