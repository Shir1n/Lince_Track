package com.example.lince_track

import com.example.lince_track.data.version.ControlVersionRepository
import com.example.lince_track.data.version.EstadoAvisoVersion
import com.example.lince_track.data.version.InfoVersion
import com.example.lince_track.data.version.ReglasVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlVersionTest {

    @Test
    fun decodificarJsonValidoRetornaInfoVersionCompleta() {
        val json = """
            {
              "versionCode": 3,
              "versionName": "1.3",
              "autorUltimoCambio": "Benjamín",
              "apkUrl": "https://example.com/app.apk",
              "novedades": ["Cambio 1", "Cambio 2"]
            }
        """.trimIndent()

        val info = ReglasVersion.decodificarJson(json)
        assertNotNull(info)
        assertEquals(3, info?.versionCode)
        assertEquals("1.3", info?.versionName)
        assertEquals("Benjamín", info?.autorUltimoCambio)
        assertEquals("https://example.com/app.apk", info?.apkUrl)
        assertEquals(listOf("Cambio 1", "Cambio 2"), info?.novedades)
    }

    @Test
    fun decodificarJsonInvalidoRetornaNuloSinLanzarExcepcion() {
        assertNull(ReglasVersion.decodificarJson(""))
        assertNull(ReglasVersion.decodificarJson("{no_es_json"))
    }

    @Test
    fun evaluarDetectaVersionDesactualizadaCuandoRemotaEsMayor() {
        val local = InfoVersion(versionCode = 2, versionName = "1.2")
        val remota = InfoVersion(
            versionCode = 3,
            versionName = "1.3",
            autorUltimoCambio = "Shir1n",
            novedades = listOf("Nueva función")
        )

        val estado = ReglasVersion.evaluar(
            local = local,
            ultimaVersionVista = 1,
            remota = remota,
            avisosActivos = true
        )

        assertTrue(estado is EstadoAvisoVersion.Desactualizada)
        val desactualizada = estado as EstadoAvisoVersion.Desactualizada
        assertEquals(2, desactualizada.local.versionCode)
        assertEquals(3, desactualizada.remota.versionCode)
    }

    @Test
    fun evaluarDetectaRecienActualizadaCuandoLocalSuperaUltimaVistaYEstaAlDia() {
        val local = InfoVersion(
            versionCode = 2,
            versionName = "1.2",
            novedades = listOf("Swipe global")
        )
        val remota = InfoVersion(versionCode = 2, versionName = "1.2")

        val estado = ReglasVersion.evaluar(
            local = local,
            ultimaVersionVista = 0,
            remota = remota,
            avisosActivos = true
        )

        assertTrue(estado is EstadoAvisoVersion.RecienActualizada)
        assertEquals(2, (estado as EstadoAvisoVersion.RecienActualizada).version.versionCode)
    }

    @Test
    fun evaluarRetornaNingunoCuandoYaVioLaVersionActualYNoHayNuevaRemota() {
        val local = InfoVersion(versionCode = 2, versionName = "1.2")
        val remota = InfoVersion(versionCode = 2, versionName = "1.2")

        val estado = ReglasVersion.evaluar(
            local = local,
            ultimaVersionVista = 2,
            remota = remota,
            avisosActivos = true
        )

        assertEquals(EstadoAvisoVersion.Ninguno, estado)
    }

    @Test
    fun evaluarRespetaInterruptorDeAvisosDesactivados() {
        val local = InfoVersion(versionCode = 1, versionName = "1.0")
        val remota = InfoVersion(versionCode = 5, versionName = "1.5")

        val estado = ReglasVersion.evaluar(
            local = local,
            ultimaVersionVista = 0,
            remota = remota,
            avisosActivos = false
        )

        assertEquals(EstadoAvisoVersion.Ninguno, estado)
    }

    @Test
    fun versionLocalEmbebidaDesdeVersionJsonEsValida() {
        val local = ControlVersionRepository.versionLocal
        assertTrue(local.versionCode >= 2)
        assertTrue(local.versionName.isNotBlank())
        assertTrue(local.novedades.isNotEmpty())
    }
}
