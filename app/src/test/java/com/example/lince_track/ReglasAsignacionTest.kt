package com.example.lince_track

import com.example.lince_track.data.AsignacionServicio
import com.example.lince_track.data.AvisoLocal
import com.example.lince_track.data.CambiosServicios
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.PerfilUsuario
import com.example.lince_track.data.PersonaDisponible
import com.example.lince_track.data.ReglasAsignacion
import com.example.lince_track.data.ReglasCompanero
import com.example.lince_track.data.RespuestaServicio
import com.example.lince_track.data.Rol
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.servicios.FakeServiciosDataSource
import com.example.lince_track.data.servicios.ServicioMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** El flujo del video: Carlos rechaza, el administrador elige a Miguel y Miguel acepta. */
class ReglasAsignacionTest {

    private val hoy = LocalDate.of(2026, 11, 2)
    private val oficina = FakeServiciosDataSource.servicios(hoy).mapNotNull { ServicioMapper.aDominio(it) }

    private val carlos = PerfilUsuario(Rol.CONDUCTOR, nombre = "Pedro Rojas", transportistaId = 12)
    private val miguel = PerfilUsuario(Rol.CONDUCTOR, nombre = "Miguel Díaz", transportistaId = 14)
    private val andrea = PerfilUsuario(Rol.GUIA, nombre = "Alfredini Pastelini", guiaId = 7)
    private val admin = PerfilUsuario(Rol.ADMINISTRADOR)

    private val reemplazo = mapOf("8941" to AsignacionServicio("8941", "uid-miguel", "Miguel Díaz"))
    private val rechazoCarlos = RespuestaServicio("8941", EstadoServicio.RECHAZADO, uid = "uid-carlos")
    private val aceptaMiguel = RespuestaServicio("8941", EstadoServicio.ACEPTADO, uid = "uid-miguel")

    private fun deId(idApi: Long) = oficina.filter { it.responsableId == idApi }

    private fun List<Servicio>.conId(id: String): Servicio = first { it.id == id }

    @Test
    fun `el reemplazo recibe el servicio por responder`() {
        val lista = ReglasAsignacion.armar(
            deId(14), oficina, reemplazo, emptyMap(), emptyMap(), miguel, "uid-miguel"
        )

        val servicio = lista.conId("8941")
        assertEquals(EstadoServicio.PENDIENTE, servicio.estado)
        assertEquals("Miguel Díaz", servicio.conductor)
    }

    @Test
    fun `quien rechazo mantiene el rechazo en su historial`() {
        val lista = ReglasAsignacion.armar(
            deId(12), oficina, reemplazo, mapOf("8941" to rechazoCarlos), emptyMap(), carlos, "uid-carlos"
        )

        assertEquals(EstadoServicio.RECHAZADO, lista.conId("8941").estado)
    }

    @Test
    fun `quien no respondio deja de ver el servicio reasignado`() {
        val lista = ReglasAsignacion.armar(deId(12), oficina, reemplazo, emptyMap(), emptyMap(), carlos, "uid-carlos")

        assertFalse(lista.any { it.id == "8941" })
    }

    @Test
    fun `el guia ve sin asignar hasta que el reemplazo acepta`() {
        val antes = ReglasAsignacion.armar(deId(7), oficina, reemplazo, emptyMap(), emptyMap(), andrea, "uid-andrea")
        assertEquals(ReglasCompanero.SIN_ASIGNAR, antes.conId("8942").conductor)

        val publicado = mapOf("8941" to RespuestaServicio("8941", EstadoServicio.ACEPTADO, nombre = "Miguel Díaz"))
        val despues = ReglasAsignacion.armar(deId(7), oficina, reemplazo, emptyMap(), publicado, andrea, "uid-andrea")
        assertEquals("Miguel Díaz", despues.conId("8942").conductor)
    }

    @Test
    fun `el panel ignora el rechazo anterior y muestra al reemplazo`() {
        val lista = ReglasAsignacion.armar(oficina, emptyList(), reemplazo, mapOf("8941" to rechazoCarlos), emptyMap(), admin, "uid-admin")

        val servicio = lista.conId("8941")
        assertEquals(EstadoServicio.PENDIENTE, servicio.estado)
        assertEquals("Miguel Díaz", servicio.conductor)
        assertTrue(servicio.reasignado)
    }

    @Test
    fun `el panel muestra la respuesta del reemplazo`() {
        val lista = ReglasAsignacion.armar(oficina, emptyList(), reemplazo, mapOf("8941" to aceptaMiguel), emptyMap(), admin, "uid-admin")

        assertEquals(EstadoServicio.ACEPTADO, lista.conId("8941").estado)
    }

    @Test
    fun `lo cancelado en la API no se reasigna`() {
        val cancelado = oficina.conId("8941").copy(estado = EstadoServicio.RECHAZADO, cancelado = true)

        assertFalse(ReglasAsignacion.puedeReasignar(cancelado, null))
        assertEquals(EstadoServicio.RECHAZADO, ReglasAsignacion.aplicar(cancelado, reemplazo["8941"]).estado)
    }

    @Test
    fun `lo aceptado no se ofrece para reasignar`() {
        assertFalse(ReglasAsignacion.puedeReasignar(oficina.conId("8941").copy(estado = EstadoServicio.ACEPTADO), null))
        assertTrue(ReglasAsignacion.puedeReasignar(oficina.conId("8941"), null))
    }

    private val personal = listOf(
        PersonaDisponible("uid-carlos", "Pedro Rojas", Rol.CONDUCTOR, 12),
        PersonaDisponible("uid-miguel", "Miguel Díaz", Rol.CONDUCTOR, 14),
        PersonaDisponible("uid-sinid", "Sin Vincular", Rol.CONDUCTOR, null),
        PersonaDisponible("uid-andrea", "Alfredini Pastelini", Rol.GUIA, 7)
    )

    @Test
    fun `solo se ofrece personal del rol y sin quien ya tiene el servicio`() {
        val candidatos = ReglasAsignacion.candidatos(oficina.conId("8941"), personal, null, emptySet())

        assertEquals(listOf("uid-miguel"), candidatos.map { it.uid })
    }

    @Test
    fun `no se ofrece a quien ya rechazo`() {
        val candidatos = ReglasAsignacion.candidatos(
            oficina.conId("8941"), personal, reemplazo["8941"], setOf("uid-carlos")
        )

        assertTrue(candidatos.isEmpty())
    }

    @Test
    fun `junta quienes rechazaron cada servicio`() {
        val rechazos = ReglasAsignacion.rechazos(listOf(rechazoCarlos, aceptaMiguel))

        assertEquals(mapOf("8941" to setOf("uid-carlos")), rechazos)
    }

    private fun aviso(id: String, titulo: String, texto: String) = AvisoLocal(id, titulo, texto)

    @Test
    fun `avisa al administrador de un rechazo`() {
        val antes = listOf(oficina.conId("8941"))
        val ahora = listOf(oficina.conId("8941").copy(estado = EstadoServicio.RECHAZADO))

        val avisos = CambiosServicios.detectar(antes, ahora, Rol.ADMINISTRADOR, hoy)

        assertEquals("Servicio rechazado", avisos.single().titulo)
    }

    @Test
    fun `avisa al reemplazo de su nuevo servicio`() {
        val avisos = CambiosServicios.detectar(emptyList(), listOf(oficina.conId("8941")), Rol.CONDUCTOR, hoy)

        assertEquals("Nuevo servicio asignado", avisos.single().titulo)
    }

    @Test
    fun `avisa al guia cuando el conductor confirma`() {
        val antes = listOf(oficina.conId("8942").copy(conductor = ReglasCompanero.SIN_ASIGNAR))
        val ahora = listOf(oficina.conId("8942").copy(conductor = "Miguel Díaz"))

        val avisos = CambiosServicios.detectar(antes, ahora, Rol.GUIA, hoy)

        assertEquals(
            listOf(aviso("8942", "Conductor confirmado", "Miguel Díaz confirmó Guiado Saltos del Petrohué")),
            avisos
        )
    }

    @Test
    fun `avisa cuando un servicio se quita de la lista`() {
        val avisos = CambiosServicios.detectar(listOf(oficina.conId("8941")), emptyList(), Rol.CONDUCTOR, hoy)

        assertEquals("Servicio reasignado", avisos.single().titulo)
    }

    @Test
    fun `sin cambios no hay avisos`() {
        assertTrue(CambiosServicios.detectar(oficina, oficina, Rol.CONDUCTOR, hoy).isEmpty())
        assertTrue(CambiosServicios.detectar(oficina, oficina, Rol.ADMINISTRADOR, hoy).isEmpty())
    }
}
