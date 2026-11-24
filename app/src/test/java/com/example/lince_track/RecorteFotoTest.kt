package com.example.lince_track

import androidx.compose.ui.geometry.Offset
import com.example.lince_track.ui.components.RecorteFoto
import org.junit.Assert.assertEquals
import org.junit.Test

/** Cálculo del recorte del editor de foto de perfil (visor de 1000 px). */
class RecorteFotoTest {

    private val visor = 1000f

    @Test
    fun sinMoverNiAcercarSeRecortaElCuadradoDelCentro() {
        // Foto horizontal 2000 × 1000: el alto llena el visor y sobran 500 px a cada lado.
        assertEquals(Triple(500, 0, 1000), RecorteFoto.cuadrado(2000, 1000, visor, 1f, Offset.Zero))
        // Foto vertical 1000 × 3000.
        assertEquals(Triple(0, 1000, 1000), RecorteFoto.cuadrado(1000, 3000, visor, 1f, Offset.Zero))
    }

    @Test
    fun moverLaFotoCambiaLaParteQueQuedaEnElCirculo() {
        // Arrastrar 200 px a la derecha muestra la parte de más a la izquierda de la foto.
        assertEquals(Triple(300, 0, 1000), RecorteFoto.cuadrado(2000, 1000, visor, 1f, Offset(200f, 0f)))
    }

    @Test
    fun nuncaSeSaleDeLaFoto() {
        val lejos = Offset(5000f, 5000f)
        assertEquals(Offset(500f, 0f), RecorteFoto.limitar(lejos, 2000, 1000, visor, 1f))
        assertEquals(Triple(0, 0, 1000), RecorteFoto.cuadrado(2000, 1000, visor, 1f, lejos))
        assertEquals(Triple(1000, 0, 1000), RecorteFoto.cuadrado(2000, 1000, visor, 1f, Offset(-5000f, 0f)))
    }

    @Test
    fun acercarRecortaUnCuadradoMasChico() {
        // Zoom 2 sobre 1000 × 1000: se ve la mitad central.
        assertEquals(Triple(250, 250, 500), RecorteFoto.cuadrado(1000, 1000, visor, 2f, Offset.Zero))
        assertEquals(Offset(500f, 500f), RecorteFoto.limite(1000, 1000, visor, 2f))
        assertEquals(2f, RecorteFoto.escala(1000, 1000, visor, 2f))
        assertEquals(Offset(-500f, -500f), RecorteFoto.origen(1000, 1000, visor, 2f, Offset.Zero))
    }
}
