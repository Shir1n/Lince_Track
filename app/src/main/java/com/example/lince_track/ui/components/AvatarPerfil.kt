package com.example.lince_track.ui.components

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import androidx.core.content.FileProvider
import android.media.ExifInterface
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.lince_track.R
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.FotoPerfilRepository
import com.example.lince_track.util.LinceLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Foto de perfil de la cuenta con sesión, guardada en Firebase Storage. Al tocarla se elige cámara o galería; la
 * imagen se achica y comprime antes de subirla. Sin foto, muestra el ícono de persona.
 */
@Composable
fun AvatarPerfil(tamano: Dp = 76.dp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uid = AuthRepository.usuarioActual?.uid
    var url by remember(uid) { mutableStateOf<String?>(null) }
    var subiendo by remember { mutableStateOf(false) }
    var mostrarSelector by remember { mutableStateOf(false) }
    // Foto elegida que se muestra recortada en círculo antes de subirla.
    var vistaPrevia by remember { mutableStateOf<Bitmap?>(null) }
    val errorSubir = stringResource(R.string.perfil_foto_error)

    LaunchedEffect(uid) {
        if (uid != null) url = FotoPerfilRepository.url(uid)
    }

    fun subir(bitmap: Bitmap) {
        val cuenta = uid ?: return
        scope.launch {
            subiendo = true
            val jpeg = withContext(Dispatchers.Default) { comprimir(bitmap) }
            FotoPerfilRepository.subir(cuenta, jpeg)
                .onSuccess { url = it }
                .onFailure { Toast.makeText(context, errorSubir, Toast.LENGTH_LONG).show() }
            subiendo = false
        }
    }

    // La cámara guarda la foto completa en un archivo temporal (la miniatura de TakePicturePreview se ve pixelada).
    val archivoCamara = remember { archivoTemporal(context) }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { tomada ->
        if (!tomada) return@rememberLauncherForActivityResult
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { leer(context, archivoCamara) }
            if (bitmap != null) vistaPrevia = bitmap else Toast.makeText(context, errorSubir, Toast.LENGTH_LONG).show()
        }
    }
    val permisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { autorizado ->
        if (autorizado) camara.launch(archivoCamara)
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) { leer(context, uri) }
                if (bitmap != null) vistaPrevia = bitmap else Toast.makeText(context, errorSubir, Toast.LENGTH_LONG).show()
            }
        }
    }

    if (mostrarSelector) {
        AlertDialog(
            onDismissRequest = { mostrarSelector = false },
            title = { Text(stringResource(R.string.perfil_cambiar_foto)) },
            text = { Text(stringResource(R.string.perfil_elige_opcion)) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarSelector = false
                    permisoCamara.launch(Manifest.permission.CAMERA)
                }) { Text(stringResource(R.string.perfil_tomar_foto)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarSelector = false
                    galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(stringResource(R.string.perfil_elegir_galeria)) }
            }
        )
    }

    vistaPrevia?.let { foto ->
        EditorFotoPerfil(
            foto = foto,
            onCancelar = { vistaPrevia = null },
            onElegirOtra = {
                vistaPrevia = null
                mostrarSelector = true
            },
            onUsar = { recorte ->
                vistaPrevia = null
                subir(recorte)
            }
        )
    }

    val editable = uid != null && !subiendo
    Box(contentAlignment = Alignment.BottomEnd) {
        val circulo = Modifier
            .size(tamano)
            .clip(CircleShape)
            .border(3.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(enabled = editable) { mostrarSelector = true }
        Box(modifier = circulo, contentAlignment = Alignment.Center) {
            val actual = url
            if (actual != null) {
                AsyncImage(
                    model = actual,
                    contentDescription = stringResource(R.string.perfil_cd_foto_conductor),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(tamano)
                )
            } else {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = stringResource(R.string.perfil_cd_foto_conductor),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(tamano)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .padding(tamano / 4)
                )
            }
            if (subiendo) CircularProgressIndicator(modifier = Modifier.size(tamano / 2))
        }
        if (uid != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                shape = CircleShape,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(enabled = editable) { mostrarSelector = true }
            ) {
                Icon(
                    Icons.Outlined.PhotoCamera,
                    contentDescription = stringResource(R.string.perfil_cambiar_foto),
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

/**
 * Recorta un cuadrado al centro (lo mismo que se ve en el círculo), lo achica a 512 px como máximo y lo comprime
 * en JPEG.
 */
private fun comprimir(original: Bitmap): ByteArray {
    val lado = minOf(original.width, original.height)
    val cuadrada = Bitmap.createBitmap(original, (original.width - lado) / 2, (original.height - lado) / 2, lado, lado)
    val (ancho, alto) = FotoPerfilRepository.medidas(lado, lado)
    val achicada = if (ancho != lado) Bitmap.createScaledBitmap(cuadrada, ancho, alto, true) else cuadrada
    return ByteArrayOutputStream().use { salida ->
        achicada.compress(Bitmap.CompressFormat.JPEG, FotoPerfilRepository.CALIDAD_JPEG, salida)
        salida.toByteArray()
    }
}

/** Uri (FileProvider) del archivo temporal donde la cámara deja la foto; se reemplaza en cada toma. */
private fun archivoTemporal(context: Context): Uri {
    val carpeta = File(context.cacheDir, "fotos").apply { mkdirs() }
    return FileProvider.getUriForFile(context, "${context.packageName}.fotos", File(carpeta, "camara.jpg"))
}

/**
 * Lee una foto (galería o cámara) ya reducida con inSampleSize, para no cargar en memoria una foto de 12 MP completa,
 * y la endereza según su EXIF (las cámaras suelen guardarla girada). Usa BitmapFactory porque ImageDecoder exige
 * Android 9 y la app parte en Android 8.1.
 */
private fun leer(context: Context, uri: Uri): Bitmap? = try {
    val bordes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bordes) }
    var muestra = 1
    // Tres veces el tamaño final: así se puede acercar en el editor sin que se pixele.
    val minimo = FotoPerfilRepository.LADO_MAXIMO * 3
    while (bordes.outWidth / (muestra * 2) >= minimo && bordes.outHeight / (muestra * 2) >= minimo) {
        muestra *= 2
    }
    val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
    val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
    val grados = context.contentResolver.openInputStream(uri)?.use {
        when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } ?: 0f
    if (bitmap == null || grados == 0f) {
        bitmap
    } else {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(grados) }, true)
    }
} catch (e: Exception) {
    LinceLogger.w("FOTO", "No se pudo leer la imagen: ${e.message}")
    null
}
