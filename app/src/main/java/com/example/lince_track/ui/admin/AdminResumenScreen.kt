package com.example.lince_track.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lince_track.data.ReglasCheckin
import com.example.lince_track.data.ServicioRepository
import com.example.lince_track.data.remote.LinceApiClient
import com.example.lince_track.ui.components.AvisoServiciosCard
import com.example.lince_track.ui.theme.BarraSuperior
import java.time.LocalDate
import java.time.LocalDateTime

/** Cifras de la operación para los dueños: puntualidad, terminados, rechazos e incidencias. */
@Composable
fun AdminResumenScreen() {
    val servicios by ServicioRepository.servicios.collectAsState()
    val respuestas by ServicioRepository.historialRespuestas.collectAsState()
    val checkins by ServicioRepository.checkins.collectAsState()
    val incidencias by ServicioRepository.incidencias.collectAsState()
    val asignaciones by ServicioRepository.asignaciones.collectAsState()
    val error by ServicioRepository.error.collectAsState()
    var periodo by rememberSaveable { mutableStateOf(PeriodoResumen.SIETE_DIAS) }

    val hoy = LocalDate.now()
    val conDemostracion = !LinceApiClient.estaConfigurada && periodo == PeriodoResumen.SIETE_DIAS
    val resumen = remember(servicios, respuestas, checkins, incidencias, asignaciones, periodo, hoy) {
        val reales = DatosResumen(servicios, respuestas, checkins, incidencias, asignaciones.keys)
        ResumenOperacion.calcular(
            datos = if (conDemostracion) reales + ResumenDemostracion.datos(hoy) else reales,
            rango = ResumenOperacion.rango(periodo, hoy),
            ahora = LocalDateTime.now()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BarraSuperior.fondo)
                .padding(horizontal = 16.dp, vertical = 11.dp)
        ) {
            Text(
                text = "Resumen de la Operación",
                color = BarraSuperior.contenido,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodoResumen.entries.forEach { opcion ->
                    FilterChip(
                        selected = periodo == opcion,
                        onClick = { periodo = opcion },
                        label = { Text(opcion.etiqueta) }
                    )
                }
            }

            error?.let { AvisoServiciosCard(it) }

            if (conDemostracion) AvisoDemostracion()

            Puntualidad(resumen)
            Terminados(resumen)
            Rechazos(resumen)
            Distancia(resumen)
            Incidencias(resumen)
            Reemplazos(resumen)
        }
    }
}

@Composable
private fun AvisoDemostracion() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Incluye datos de demostración",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = "Las cifras de días anteriores son de ejemplo y no corresponden a la operación real.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun Puntualidad(resumen: Resumen) {
    TarjetaResumen("Puntualidad") {
        val porcentaje = ResumenOperacion.porcentaje(resumen.aTiempo, resumen.conCheckIn)
        if (porcentaje == null) {
            SinDatos()
        } else {
            CifraPrincipal("$porcentaje % a tiempo")
            Detalle("${resumen.aTiempo} de ${resumen.conCheckIn} check-ins, con ${ResumenOperacion.MARGEN_PUNTUALIDAD_MIN} minutos de margen")
        }
        if (resumen.sinCheckIn > 0) {
            Detalle(if (resumen.sinCheckIn == 1) "1 servicio sin check-in" else "${resumen.sinCheckIn} servicios sin check-in")
        }
    }
}

@Composable
private fun Terminados(resumen: Resumen) {
    TarjetaResumen("Servicios terminados") {
        val porcentaje = ResumenOperacion.porcentaje(resumen.terminados, resumen.aceptados)
        if (porcentaje == null) {
            SinDatos()
        } else {
            CifraPrincipal("${resumen.terminados} de ${resumen.aceptados}")
            Barra(porcentaje / 100f, MaterialTheme.colorScheme.secondary)
            Detalle("$porcentaje % de los aceptados tiene check-out")
        }
    }
}

@Composable
private fun Rechazos(resumen: Resumen) {
    TarjetaResumen("Rechazos por motivo") {
        if (resumen.rechazos.isEmpty()) {
            Detalle("Sin rechazos en este periodo")
        } else {
            CifraPrincipal(resumen.totalRechazos.toString())
            ListaCantidades(resumen.rechazos, MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun Distancia(resumen: Resumen) {
    TarjetaResumen("Distancia del check-in") {
        val promedio = resumen.distanciaPromedioM
        val porcentaje = ResumenOperacion.porcentaje(resumen.cercanos, resumen.conGpsReal)
        if (promedio == null || porcentaje == null) {
            SinDatos()
        } else {
            CifraPrincipal("${ReglasCheckin.formatoDistancia(promedio)} en promedio")
            Detalle("$porcentaje % a menos de ${ResumenOperacion.DISTANCIA_CERCANA_M} m del punto de inicio")
        }
    }
}

@Composable
private fun Incidencias(resumen: Resumen) {
    TarjetaResumen("Incidencias") {
        if (resumen.incidenciasPorTipo.isEmpty()) {
            Detalle("Sin incidencias en este periodo")
        } else {
            CifraPrincipal("${resumen.incidenciasAbiertas} abiertas • ${resumen.incidenciasResueltas} resueltas")
            ListaCantidades(resumen.incidenciasPorTipo, MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun Reemplazos(resumen: Resumen) {
    TarjetaResumen("Reemplazos") {
        CifraPrincipal(resumen.reemplazos.toString())
        Detalle(if (resumen.reemplazos == 1) "servicio reasignado" else "servicios reasignados")
    }
}

@Composable
private fun TarjetaResumen(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            contenido()
        }
    }
}

@Composable
private fun CifraPrincipal(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun Detalle(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SinDatos() = Detalle("Sin datos en este periodo")

/** El largo de cada barra es relativo a la cantidad mayor de la lista. */
@Composable
private fun ListaCantidades(cantidades: List<CantidadResumen>, color: Color) {
    val mayor = cantidades.maxOf { it.cantidad }
    cantidades.forEach { fila ->
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = fila.etiqueta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = fila.cantidad.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Barra(fila.cantidad.toFloat() / mayor, color)
    }
}

@Composable
private fun Barra(fraccion: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (fraccion > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraccion)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
