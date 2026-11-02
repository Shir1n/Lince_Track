package com.example.lince_track

import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.MotivoRechazo
import com.example.lince_track.data.ReglasRespuesta
import com.example.lince_track.data.RespuestaServicio
import com.example.lince_track.data.servicios.FakeServiciosDataSource
import com.example.lince_track.data.servicios.ServicioMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Validación del motivo de rechazo y prioridad entre la API y la respuesta de la persona. */
class RespuestaServicioTest {

    private val servicioBase = ServicioMapper.aDominio(
        FakeServiciosDataSource.servicios(LocalDate.of(2026, 11, 1)).first()
    )!!

    private fun servicioCon(estado: EstadoServicio) = servicioBase.copy(estado = estado)

    private fun respuesta(estado: EstadoServicio) = RespuestaServicio(servicioBase.id, estado)

    @Test
    fun `sin motivo no se puede rechazar`() {
        assertFalse(ReglasRespuesta.motivoValido(null, "observaciones"))
    }

    @Test
    fun `otro exige observaciones`() {
        assertFalse(ReglasRespuesta.motivoValido(MotivoRechazo.OTRO, ""))
        assertFalse(ReglasRespuesta.motivoValido(MotivoRechazo.OTRO, "   "))
        assertTrue(ReglasRespuesta.motivoValido(MotivoRechazo.OTRO, "Se atrasó el ferry"))
    }

    @Test
    fun `los demas motivos no exigen observaciones`() {
        MotivoRechazo.entries.filterNot { it.requiereTexto }.forEach {
            assertTrue(ReglasRespuesta.motivoValido(it, ""))
        }
    }

    @Test
    fun `observaciones demasiado largas no son validas`() {
        val largo = "x".repeat(ReglasRespuesta.MAX_OBSERVACIONES + 1)
        assertFalse(ReglasRespuesta.motivoValido(MotivoRechazo.FALLA_MECANICA, largo))
    }

    @Test
    fun `la respuesta manda sobre un servicio pendiente o aceptado`() {
        assertEquals(
            EstadoServicio.ACEPTADO,
            ReglasRespuesta.aplicar(servicioCon(EstadoServicio.PENDIENTE), respuesta(EstadoServicio.ACEPTADO)).estado
        )
        assertEquals(
            EstadoServicio.RECHAZADO,
            ReglasRespuesta.aplicar(servicioCon(EstadoServicio.ACEPTADO), respuesta(EstadoServicio.RECHAZADO)).estado
        )
    }

    @Test
    fun `un servicio rechazado o cancelado por la API sigue rechazado`() {
        assertEquals(
            EstadoServicio.RECHAZADO,
            ReglasRespuesta.aplicar(servicioCon(EstadoServicio.RECHAZADO), respuesta(EstadoServicio.ACEPTADO)).estado
        )
    }

    @Test
    fun `sin respuesta se mantiene el estado de la API`() {
        assertEquals(
            EstadoServicio.PENDIENTE,
            ReglasRespuesta.aplicar(servicioCon(EstadoServicio.PENDIENTE), null).estado
        )
    }
}
