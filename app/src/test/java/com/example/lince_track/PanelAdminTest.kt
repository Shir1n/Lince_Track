package com.example.lince_track

import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.MotivoRechazo
import com.example.lince_track.data.ReglasRespuesta
import com.example.lince_track.data.RespuestaServicio
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.servicios.FakeServiciosDataSource
import com.example.lince_track.data.servicios.ServicioMapper
import com.example.lince_track.ui.admin.FiltroPanel
import com.example.lince_track.ui.admin.PanelAdmin
import com.example.lince_track.ui.admin.TotalesPanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** Conteos, filtros, orden y motivos del panel del administrador. */
class PanelAdminTest {

    private val base: Servicio = ServicioMapper.aDominio(
        FakeServiciosDataSource.servicios(LocalDate.of(2026, 11, 1)).first()
    )!!

    private fun servicio(id: String, estado: EstadoServicio, hora: Int = 8) =
        base.copy(id = id, estado = estado, horaInicio = LocalTime.of(hora, 0))

    @Test
    fun `totales cuenta cada estado`() {
        val servicios = listOf(
            servicio("1", EstadoServicio.PENDIENTE),
            servicio("2", EstadoServicio.PENDIENTE),
            servicio("3", EstadoServicio.ACEPTADO),
            servicio("4", EstadoServicio.RECHAZADO)
        )
        assertEquals(TotalesPanel(pendientes = 2, aceptados = 1, rechazados = 1), PanelAdmin.totales(servicios))
    }

    @Test
    fun `filtro deja solo el estado elegido`() {
        val servicios = listOf(
            servicio("1", EstadoServicio.PENDIENTE),
            servicio("2", EstadoServicio.ACEPTADO)
        )
        val filas = PanelAdmin.filas(servicios, emptyMap(), FiltroPanel.ACEPTADOS)
        assertEquals(listOf("2"), filas.map { it.servicio.id })
    }

    @Test
    fun `los rechazados van primero y luego por hora`() {
        val servicios = listOf(
            servicio("tarde", EstadoServicio.PENDIENTE, hora = 15),
            servicio("temprano", EstadoServicio.ACEPTADO, hora = 7),
            servicio("rechazado", EstadoServicio.RECHAZADO, hora = 18)
        )
        val filas = PanelAdmin.filas(servicios, emptyMap(), FiltroPanel.TODOS)
        assertEquals(listOf("rechazado", "temprano", "tarde"), filas.map { it.servicio.id })
    }

    @Test
    fun `rechazo de la persona muestra su motivo y observaciones`() {
        val respuesta = RespuestaServicio("1", EstadoServicio.RECHAZADO, MotivoRechazo.OTRO, "Se atrasó el ferry")
        val aplicado = ReglasRespuesta.aplicar(servicio("1", EstadoServicio.PENDIENTE), respuesta)

        val fila = PanelAdmin.filas(listOf(aplicado), mapOf("1" to respuesta), FiltroPanel.TODOS).single()

        assertEquals("Otro", fila.motivo)
        assertEquals("Se atrasó el ferry", fila.observaciones)
    }

    @Test
    fun `rechazo de la API se indica como tal`() {
        val fila = PanelAdmin.filas(listOf(servicio("1", EstadoServicio.RECHAZADO)), emptyMap(), FiltroPanel.TODOS).single()
        assertEquals(PanelAdmin.RECHAZADO_EN_API, fila.motivo)
    }

    @Test
    fun `aceptado por la persona y cancelado por la API muestra a la API`() {
        val respuesta = RespuestaServicio("1", EstadoServicio.ACEPTADO)
        val aplicado = ReglasRespuesta.aplicar(servicio("1", EstadoServicio.RECHAZADO), respuesta)

        val fila = PanelAdmin.filas(listOf(aplicado), mapOf("1" to respuesta), FiltroPanel.TODOS).single()

        assertEquals(PanelAdmin.RECHAZADO_EN_API, fila.motivo)
    }

    @Test
    fun `servicio no rechazado no lleva motivo`() {
        val fila = PanelAdmin.filas(listOf(servicio("1", EstadoServicio.ACEPTADO)), emptyMap(), FiltroPanel.TODOS).single()
        assertNull(fila.motivo)
    }

    @Test
    fun `responsable depende del tipo de servicio`() {
        val guiado = base.copy(tipo = "GUIDED_SERVICE", guia = "Andrea Soto", conductor = "Sin transportista")
        val traslado = base.copy(tipo = "TRANSFER", guia = "Sin guía", conductor = "Miguel Soto")

        val filas = PanelAdmin.filas(listOf(guiado), emptyMap(), FiltroPanel.TODOS) +
            PanelAdmin.filas(listOf(traslado), emptyMap(), FiltroPanel.TODOS)

        assertEquals(listOf("Andrea Soto", "Miguel Soto"), filas.map { it.responsable })
    }
}
