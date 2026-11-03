package com.example.lince_track

import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.IncidenciaServicio
import com.example.lince_track.data.MotivoRechazo
import com.example.lince_track.data.RespuestaServicio
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.TipoIncidencia
import com.example.lince_track.data.servicios.FakeServiciosDataSource
import com.example.lince_track.data.servicios.ServicioMapper
import com.example.lince_track.ui.admin.CantidadResumen
import com.example.lince_track.ui.admin.DatosResumen
import com.example.lince_track.ui.admin.PanelAdmin
import com.example.lince_track.ui.admin.PeriodoResumen
import com.example.lince_track.ui.admin.Resumen
import com.example.lince_track.ui.admin.ResumenDemostracion
import com.example.lince_track.ui.admin.ResumenOperacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Cifras del resumen para los dueños y su historial de demostración. */
class ResumenOperacionTest {

    private val hoy = LocalDate.of(2026, 11, 2)
    private val zona = ZoneOffset.UTC
    private val mediodia = hoy.atTime(12, 0)

    private val base: Servicio = ServicioMapper.aDominio(
        FakeServiciosDataSource.servicios(hoy).first()
    )!!

    private fun servicio(
        id: String,
        estado: EstadoServicio = EstadoServicio.ACEPTADO,
        fecha: LocalDate = hoy,
        hora: Int = 8
    ) = base.copy(id = id, estado = estado, fecha = fecha, horaInicio = LocalTime.of(hora, 0))

    /** Check-in a [minutos] de la hora de inicio del servicio. */
    private fun checkin(
        servicio: Servicio,
        minutos: Long = 0,
        distanciaM: Int = 100,
        gpsReal: Boolean = true,
        terminado: Boolean = false
    ): Pair<String, CheckinServicio> {
        val en = servicio.fecha.atTime(servicio.horaInicio).plusMinutes(minutos).toInstant(zona).toEpochMilli()
        return servicio.id to CheckinServicio(servicio.id, en, distanciaM, gpsReal, if (terminado) en + 1 else null)
    }

    private fun incidencia(id: String, fecha: LocalDate, resuelta: Boolean, tipo: TipoIncidencia) = IncidenciaServicio(
        id = id,
        servicioId = "1",
        titulo = "",
        uid = "u",
        nombre = "",
        tipo = tipo,
        detalle = "",
        creadaEn = fecha.atTime(9, 0).toInstant(zona).toEpochMilli(),
        resuelta = resuelta
    )

    private fun calcular(datos: DatosResumen, periodo: PeriodoResumen = PeriodoResumen.HOY) =
        ResumenOperacion.calcular(datos, ResumenOperacion.rango(periodo, hoy), mediodia, zona)

    @Test
    fun `sin datos no hay cifras ni division por cero`() {
        val resumen = calcular(DatosResumen())
        assertEquals(Resumen(), resumen)
        assertNull(ResumenOperacion.porcentaje(resumen.aTiempo, resumen.conCheckIn))
        assertNull(resumen.distanciaPromedioM)
    }

    @Test
    fun `porcentaje redondea`() {
        assertEquals(67, ResumenOperacion.porcentaje(2, 3))
        assertEquals(100, ResumenOperacion.porcentaje(5, 5))
    }

    @Test
    fun `rango de siete dias termina hoy`() {
        val rango = ResumenOperacion.rango(PeriodoResumen.SIETE_DIAS, hoy)
        assertEquals(hoy.minusDays(6), rango.start)
        assertEquals(hoy, rango.endInclusive)
    }

    @Test
    fun `puntualidad acepta hasta quince minutos de atraso`() {
        val temprano = servicio("1")
        val justo = servicio("2")
        val tarde = servicio("3")
        val resumen = calcular(
            DatosResumen(
                servicios = listOf(temprano, justo, tarde),
                checkins = mapOf(checkin(temprano, -20), checkin(justo, 15), checkin(tarde, 16))
            )
        )
        assertEquals(3, resumen.conCheckIn)
        assertEquals(2, resumen.aTiempo)
    }

    @Test
    fun `sin check-in solo cuenta cuando ya paso el margen`() {
        val resumen = calcular(
            DatosResumen(
                servicios = listOf(
                    servicio("paso", hora = 8),
                    servicio("futuro", hora = 18),
                    servicio("pendiente", EstadoServicio.PENDIENTE, hora = 8)
                )
            )
        )
        assertEquals(1, resumen.sinCheckIn)
    }

    @Test
    fun `terminados son los aceptados con check-out`() {
        val hecho = servicio("1")
        val enCurso = servicio("2")
        val cancelado = servicio("3", EstadoServicio.RECHAZADO)
        val resumen = calcular(
            DatosResumen(
                servicios = listOf(hecho, enCurso, cancelado),
                checkins = mapOf(checkin(hecho, terminado = true), checkin(enCurso), checkin(cancelado, terminado = true))
            )
        )
        assertEquals(2, resumen.aceptados)
        assertEquals(1, resumen.terminados)
    }

    @Test
    fun `rechazos incluyen al reemplazado y lo cancelado en la API`() {
        val reemplazado = servicio("1", EstadoServicio.ACEPTADO)
        val porLaApi = servicio("2", EstadoServicio.RECHAZADO)
        val sinMotivo = servicio("3", EstadoServicio.RECHAZADO)
        val otroCruce = servicio("4", EstadoServicio.RECHAZADO)
        val resumen = calcular(
            DatosResumen(
                servicios = listOf(reemplazado, porLaApi, sinMotivo, otroCruce),
                respuestas = listOf(
                    RespuestaServicio("1", EstadoServicio.RECHAZADO, MotivoRechazo.CRUCE_HORARIO, uid = "a"),
                    RespuestaServicio("1", EstadoServicio.ACEPTADO, uid = "b"),
                    RespuestaServicio("3", EstadoServicio.RECHAZADO, uid = "c"),
                    RespuestaServicio("4", EstadoServicio.RECHAZADO, MotivoRechazo.CRUCE_HORARIO, uid = "d")
                )
            )
        )
        assertEquals(
            listOf(
                CantidadResumen(MotivoRechazo.CRUCE_HORARIO.etiqueta, 2),
                CantidadResumen(PanelAdmin.MOTIVO_DESCONOCIDO, 1),
                CantidadResumen(PanelAdmin.RECHAZADO_EN_API, 1)
            ),
            resumen.rechazos
        )
        assertEquals(4, resumen.totalRechazos)
    }

    @Test
    fun `distancia no cuenta el gps simulado`() {
        val cerca = servicio("1")
        val lejos = servicio("2")
        val simulado = servicio("3")
        val resumen = calcular(
            DatosResumen(
                servicios = listOf(cerca, lejos, simulado),
                checkins = mapOf(
                    checkin(cerca, distanciaM = 100),
                    checkin(lejos, distanciaM = 900),
                    checkin(simulado, distanciaM = 50_000, gpsReal = false)
                )
            )
        )
        assertEquals(2, resumen.conGpsReal)
        assertEquals(1, resumen.cercanos)
        assertEquals(500, resumen.distanciaPromedioM)
    }

    @Test
    fun `incidencias se separan por estado y tipo`() {
        val resumen = calcular(
            DatosResumen(
                incidencias = listOf(
                    incidencia("1", hoy, resuelta = false, TipoIncidencia.ATRASO),
                    incidencia("2", hoy, resuelta = true, TipoIncidencia.ATRASO),
                    incidencia("3", hoy, resuelta = true, TipoIncidencia.CAMINO)
                )
            )
        )
        assertEquals(1, resumen.incidenciasAbiertas)
        assertEquals(2, resumen.incidenciasResueltas)
        assertEquals(
            listOf(CantidadResumen(TipoIncidencia.ATRASO.etiqueta, 2), CantidadResumen(TipoIncidencia.CAMINO.etiqueta, 1)),
            resumen.incidenciasPorTipo
        )
    }

    @Test
    fun `lo que esta fuera del periodo no cuenta`() {
        val ayer = servicio("ayer", fecha = hoy.minusDays(1))
        val manana = servicio("manana", fecha = hoy.plusDays(1))
        val datos = DatosResumen(
            servicios = listOf(ayer, manana),
            checkins = mapOf(checkin(ayer), checkin(manana)),
            incidencias = listOf(incidencia("1", hoy.minusDays(1), resuelta = true, TipoIncidencia.OTRO)),
            reemplazos = setOf("ayer", "manana", "no-cargado")
        )
        assertEquals(Resumen(), calcular(datos))

        val semana = calcular(datos, PeriodoResumen.SIETE_DIAS)
        assertEquals(1, semana.conCheckIn)
        assertEquals(1, semana.reemplazos)
        assertEquals(1, semana.incidenciasResueltas)
    }

    @Test
    fun `demostracion no aporta nada a hoy`() {
        assertEquals(Resumen(), calcular(ResumenDemostracion.datos(hoy, zona)))
        assertTrue(ResumenDemostracion.datos(hoy, zona).servicios.all { it.fecha < hoy && it.fecha >= hoy.minusDays(6) })
    }

    @Test
    fun `demostracion da las cifras esperadas`() {
        val resumen = calcular(ResumenDemostracion.datos(hoy, zona), PeriodoResumen.SIETE_DIAS)
        assertEquals(20, resumen.conCheckIn)
        assertEquals(18, resumen.aTiempo)
        assertEquals(1, resumen.sinCheckIn)
        assertEquals(21, resumen.aceptados)
        assertEquals(19, resumen.terminados)
        assertEquals(5, resumen.totalRechazos)
        assertEquals(3, resumen.incidenciasResueltas)
        assertEquals(0, resumen.incidenciasAbiertas)
        assertEquals(2, resumen.reemplazos)
    }
}
