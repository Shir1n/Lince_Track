package com.example.lince_track.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.lince_track.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Cálculo del recorte del editor de foto, sin Android: la foto se dibuja cubriendo un cuadrado de [visor] px y se
 * puede acercar ([zoom], desde 1) y mover ([desplazamiento] en px de pantalla, desde el centro).
 */
object RecorteFoto {

    const val ZOOM_MAXIMO = 5f

    /** Píxeles de pantalla por píxel de la foto. Con zoom 1 el lado menor llena el visor. */
    fun escala(ancho: Int, alto: Int, visor: Float, zoom: Float): Float = visor / min(ancho, alto) * zoom

    /** Desplazamiento máximo en cada eje para que la foto siempre cubra todo el visor. */
    fun limite(ancho: Int, alto: Int, visor: Float, zoom: Float): Offset {
        val k = escala(ancho, alto, visor, zoom)
        return Offset(max(0f, (ancho * k - visor) / 2f), max(0f, (alto * k - visor) / 2f))
    }

    fun limitar(desplazamiento: Offset, ancho: Int, alto: Int, visor: Float, zoom: Float): Offset {
        val l = limite(ancho, alto, visor, zoom)
        return Offset(desplazamiento.x.coerceIn(-l.x, l.x), desplazamiento.y.coerceIn(-l.y, l.y))
    }

    /** Esquina superior izquierda de la foto en el visor (px de pantalla). */
    fun origen(ancho: Int, alto: Int, visor: Float, zoom: Float, desplazamiento: Offset): Offset {
        val k = escala(ancho, alto, visor, zoom)
        return Offset((visor - ancho * k) / 2f + desplazamiento.x, (visor - alto * k) / 2f + desplazamiento.y)
    }

    /** Cuadrado de la foto (x, y, lado en px de la foto) que se ve dentro del visor. */
    fun cuadrado(ancho: Int, alto: Int, visor: Float, zoom: Float, desplazamiento: Offset): Triple<Int, Int, Int> {
        val k = escala(ancho, alto, visor, zoom)
        val o = origen(ancho, alto, visor, zoom, limitar(desplazamiento, ancho, alto, visor, zoom))
        val lado = min((visor / k).roundToInt(), min(ancho, alto)).coerceAtLeast(1)
        val x = (-o.x / k).roundToInt().coerceIn(0, ancho - lado)
        val y = (-o.y / k).roundToInt().coerceIn(0, alto - lado)
        return Triple(x, y, lado)
    }
}

/**
 * Editor a pantalla casi completa: la foto se ve dentro del mismo círculo del perfil, y se puede arrastrar para
 * moverla y pellizcar para acercarla. [onUsar] recibe solo el cuadrado que quedó dentro del círculo.
 */
@Composable
fun EditorFotoPerfil(
    foto: Bitmap,
    onCancelar: () -> Unit,
    onElegirOtra: () -> Unit,
    onUsar: (Bitmap) -> Unit
) {
    val imagen = remember(foto) { foto.asImageBitmap() }
    var zoom by remember(foto) { mutableFloatStateOf(1f) }
    var desplazamiento by remember(foto) { mutableStateOf(Offset.Zero) }
    var visorPx by remember { mutableFloatStateOf(0f) }

    val configuracion = LocalConfiguration.current
    val horizontal = configuracion.orientation == Configuration.ORIENTATION_LANDSCAPE
    // El visor siempre cabe en la pantalla: en vertical, a lo ancho (sin pasar de ~60 % del alto); en horizontal,
    // a lo alto, con los textos y botones al lado.
    val altoPantalla = configuracion.screenHeightDp.dp
    val anchoPantalla = configuracion.screenWidthDp.dp

    val visor: @Composable (Dp) -> Unit = { lado ->
        val colorBorde = MaterialTheme.colorScheme.secondary
        Canvas(
            modifier = Modifier
                .size(lado)
                // Capa propia para poder "perforar" el círculo en el velo oscuro.
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .pointerInput(foto) {
                    detectTransformGestures { _, mover, acercar, _ ->
                        val v = size.width.toFloat()
                        zoom = (zoom * acercar).coerceIn(1f, RecorteFoto.ZOOM_MAXIMO)
                        desplazamiento = RecorteFoto.limitar(desplazamiento + mover, foto.width, foto.height, v, zoom)
                    }
                }
        ) {
            val v = size.width
            visorPx = v
            val k = RecorteFoto.escala(foto.width, foto.height, v, zoom)
            val o = RecorteFoto.origen(foto.width, foto.height, v, zoom, desplazamiento)
            val destino = IntOffset(o.x.roundToInt(), o.y.roundToInt())
            val tamanoDestino = IntSize((foto.width * k).roundToInt(), (foto.height * k).roundToInt())
            drawImage(image = imagen, dstOffset = destino, dstSize = tamanoDestino)
            // Velo fuera del círculo: lo que queda oscuro no se verá en el perfil.
            drawRect(Color.Black.copy(alpha = 0.55f))
            drawCircle(Color.Transparent, radius = v / 2f, blendMode = BlendMode.Clear)
            drawImage(image = imagen, dstOffset = destino, dstSize = tamanoDestino, blendMode = BlendMode.DstOver)
            drawCircle(colorBorde, radius = v / 2f - 2.dp.toPx(), style = Stroke(width = 3.dp.toPx()))
        }
    }

    val textos: @Composable () -> Unit = {
        Text(
            text = stringResource(R.string.perfil_vista_previa_titulo),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.perfil_vista_previa_texto) + " " + stringResource(R.string.perfil_editor_ayuda),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }

    val botones: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onElegirOtra) { Text(stringResource(R.string.perfil_vista_previa_otra)) }
            Button(onClick = {
                val v = visorPx.takeIf { it > 0f } ?: return@Button
                val (x, y, l) = RecorteFoto.cuadrado(foto.width, foto.height, v, zoom, desplazamiento)
                onUsar(Bitmap.createBitmap(foto, x, y, l, l))
            }) {
                Text(stringResource(R.string.perfil_vista_previa_usar), fontWeight = FontWeight.Bold)
            }
        }
    }

    Dialog(onDismissRequest = onCancelar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            if (horizontal) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    visor(minOf(altoPantalla - 80.dp, anchoPantalla * 0.5f))
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        textos()
                        Spacer(Modifier.height(20.dp))
                        botones()
                    }
                }
            } else {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    textos()
                    Spacer(Modifier.height(16.dp))
                    visor(minOf(anchoPantalla * 0.94f - 40.dp, altoPantalla * 0.6f))
                    Spacer(Modifier.height(16.dp))
                    botones()
                }
            }
        }
    }
}
