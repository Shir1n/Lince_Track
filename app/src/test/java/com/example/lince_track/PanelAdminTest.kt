package com.example.lince_track

import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.MotivoRechazo
import com.example.lince_track.data.ReglasCheckin
import com.example.lince_track.data.ReglasRespuesta
import com.example.lince_track.data.RespuestaServicio
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.servicios.FakeServiciosDataSource
import com.example.lince_track.data.servicios.ServicioMapper
import com.example.lince_track.ui.admin.FilaPanel
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

    private fun terminado(servicio: Servicio) =
        servicio.id to CheckinServicio(servicio.id, 0L, distanciaM = 10, gpsReal = true, checkOutEn = 1L)

    @Test
    fun `totales no cuenta dos veces lo terminado`() {
        val hecho = servicio("1", EstadoServicio.ACEPTADO)
        val servicios = listOf(hecho, servicio("2", EstadoServicio.ACEPTADO), servicio("3", EstadoServicio.PENDIENTE))
        assertEquals(
            TotalesPanel(pendientes = 1, aceptados = 1, rechazados = 0, terminados = 1),
            PanelAdmin.totales(servicios, mapOf(terminado(hecho)))
        )
    }

    @Test
    fun `cancelado por la API con check-out no cuenta como terminado`() {
        val cancelado = servicio("1", EstadoServicio.RECHAZADO)
        val checkins = mapOf(terminado(cancelado))
        assertEquals(
            TotalesPanel(pendientes = 0, aceptados = 0, rechazados = 1, terminados = 0),
            PanelAdmin.totales(listOf(cancelado), checkins)
        )
        assertEquals(emptyList<FilaPanel>(), PanelAdmin.filas(listOf(cancelado), emptyMap(), FiltroPanel.TERMINADOS, checkins))
    }

    @Test
    fun `filtros separan aceptados de terminados`() {
        val hecho = servicio("hecho", EstadoServicio.ACEPTADO)
        val enCurso = servicio("en-curso", EstadoServicio.ACEPTADO)
        val checkins = mapOf(terminado(hecho))
        val ids = { filtro: FiltroPanel ->
            PanelAdmin.filas(listOf(hecho, enCurso), emptyMap(), filtro, checkins).map { it.servicio.id }
        }
        assertEquals(listOf("en-curso"), ids(FiltroPanel.ACEPTADOS))
        assertEquals(listOf("hecho"), ids(FiltroPanel.TERMINADOS))
        assertEquals(setOf("hecho", "en-curso"), ids(FiltroPanel.TODOS).toSet())
    }

    @Test
    fun `los terminados van al final aunque sean mas temprano`() {
        val hecho = servicio("hecho", EstadoServicio.ACEPTADO, hora = 7)
        val servicios = listOf(
            hecho,
            servicio("tarde", EstadoServicio.PENDIENTE, hora = 15),
            servicio("rechazado", EstadoServicio.RECHAZADO, hora = 18)
        )
        val filas = PanelAdmin.filas(servicios, emptyMap(), FiltroPanel.TODOS, mapOf(terminado(hecho)))
        assertEquals(listOf("rechazado", "tarde", "hecho"), filas.map { it.servicio.id })
    }

    @Test
    fun `lo pasado ya no se puede reasignar`() {
        val hoy = LocalDate.of(2026, 11, 2)
        val ayer = servicio("ayer", EstadoServicio.RECHAZADO).copy(fecha = hoy.minusDays(1))
        val deHoy = servicio("hoy", EstadoServicio.RECHAZADO).copy(fecha = hoy)
        val filas = PanelAdmin.filas(listOf(ayer, deHoy), emptyMap(), FiltroPanel.TODOS, hoy = hoy)
        assertEquals(false, filas.single { it.servicio.id == "ayer" }.puedeReasignar)
        assertEquals(true, filas.single { it.servicio.id == "hoy" }.puedeReasignar)
    }

    @Test
    fun `rango de fechas coincide con lo cargado`() {
        val hoy = LocalDate.of(2026, 11, 2)
        val rango = PanelAdmin.rangoFechas(hoy)
        assertEquals(hoy.minusDays(7), rango.start)
        assertEquals(hoy.plusDays(30), rango.endInclusive)
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
    fun `fila muestra el check-in solo si existe`() {
        val checkin = CheckinServicio("1", 0L, distanciaM = 120, gpsReal = true)
        val servicios = listOf(servicio("1", EstadoServicio.ACEPTADO), servicio("2", EstadoServicio.ACEPTADO))

        val filas = PanelAdmin.filas(servicios, emptyMap(), FiltroPanel.TODOS, mapOf("1" to checkin))

        assertEquals(ReglasCheckin.resumen(checkin), filas.single { it.servicio.id == "1" }.estadoRuta)
        assertNull(filas.single { it.servicio.id == "2" }.estadoRuta)
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
