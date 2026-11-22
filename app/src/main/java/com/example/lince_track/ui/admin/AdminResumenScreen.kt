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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lince_track.R
import com.example.lince_track.data.ReglasCheckin
import com.example.lince_track.data.ResumenSouthboundRepository
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
    // En el idioma de la app (el error del repositorio viene como UiText).
    val error = ServicioRepository.errorTexto.collectAsState().value?.asString()
    var periodo by rememberSaveable { mutableStateOf(PeriodoResumen.SIETE_DIAS) }

    // Datos de Southbound (/api/v1/resumen) para el mismo periodo.
    val southbound: ResumenSouthboundViewModel = viewModel()
    val estadoSouthbound by southbound.estado.collectAsState()
    LaunchedEffect(periodo) { southbound.cargar(periodo) }

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
                text = stringResource(R.string.admin_resumen_titulo),
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
                    val labelRes = when (opcion) {
                        PeriodoResumen.HOY -> R.string.admin_periodo_hoy
                        PeriodoResumen.SIETE_DIAS -> R.string.admin_periodo_7dias
                    }
                    FilterChip(
                        selected = periodo == opcion,
                        onClick = { periodo = opcion },
                        label = { Text(stringResource(labelRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                }
            }

            error?.let { AvisoServiciosCard(it) }

            if (conDemostracion) AvisoDemostracion()

            // Sin clave de la API no hay /resumen: la tarjeta no se muestra.
            if (southbound.disponible) {
                TarjetaSouthbound(estadoSouthbound, onReintentar = southbound::reintentar)
            }

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
                text = stringResource(R.string.admin_demo_titulo),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = stringResource(R.string.admin_demo_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/** Totales de Southbound para el periodo: servicios, pasajeros y cuántos hay en cada statusPL. */
@Composable
private fun TarjetaSouthbound(estado: EstadoResumenSouthbound, onReintentar: () -> Unit) {
    TarjetaResumen(stringResource(R.string.southbound_titulo)) {
        val resumen = estado.resumen
        when {
            estado.cargando -> Detalle(stringResource(R.string.southbound_cargando))
            estado.error != null -> {
                Detalle(estado.error.asString())
                TextButton(onClick = onReintentar) { Text(stringResource(R.string.ruta_reintentar)) }
            }
            resumen != null -> {
                CifraPrincipal(stringResource(R.string.southbound_totales, resumen.totalServicios, resumen.totalPax))
                Detalle(stringResource(R.string.southbound_detalle, ResumenSouthboundRepository.OFICINA))
                ListaCantidades(
                    resumen.porStatusPL.map { conteo ->
                        CantidadResumen(
                            stringResource(R.string.southbound_fila, etiquetaStatusPL(conteo.clave), conteo.pax),
                            conteo.servicios
                        )
                    },
                    MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/** Nombre del statusPL en el idioma de la app; un código nuevo de la API se muestra tal cual. */
@Composable
private fun etiquetaStatusPL(clave: String): String = when (clave) {
    "CF" -> stringResource(R.string.southbound_cf)
    "RQ" -> stringResource(R.string.southbound_rq)
    "DL" -> stringResource(R.string.southbound_dl)
    "CX" -> stringResource(R.string.southbound_cx)
    "SIN_ASIGNAR" -> stringResource(R.string.southbound_sin_asignar)
    else -> clave
}

@Composable
private fun Puntualidad(resumen: Resumen) {
    TarjetaResumen(stringResource(R.string.admin_resumen_puntualidad)) {
        val porcentaje = ResumenOperacion.porcentaje(resumen.aTiempo, resumen.conCheckIn)
        if (porcentaje == null) {
            SinDatos()
        } else {
            CifraPrincipal(stringResource(R.string.admin_resumen_a_tiempo, porcentaje))
            Detalle(
                stringResource(
                    R.string.admin_resumen_puntualidad_detalle,
                    resumen.aTiempo,
                    resumen.conCheckIn,
                    ResumenOperacion.MARGEN_PUNTUALIDAD_MIN
                )
            )
        }
        if (resumen.sinCheckIn > 0) {
            Detalle(
                if (resumen.sinCheckIn == 1) stringResource(R.string.admin_resumen_un_sin_checkin)
                else stringResource(R.string.admin_resumen_n_sin_checkin, resumen.sinCheckIn)
            )
        }
    }
}

@Composable
private fun Terminados(resumen: Resumen) {
    TarjetaResumen(stringResource(R.string.admin_resumen_terminados)) {
        val porcentaje = ResumenOperacion.porcentaje(resumen.terminados, resumen.aceptados)
        if (porcentaje == null) {
            SinDatos()
        } else {
            CifraPrincipal(stringResource(R.string.admin_resumen_de, resumen.terminados, resumen.aceptados))
            Barra(porcentaje / 100f, MaterialTheme.colorScheme.secondary)
            Detalle(stringResource(R.string.admin_resumen_checkout_pct, porcentaje))
        }
    }
}

@Composable
private fun Rechazos(resumen: Resumen) {
    TarjetaResumen(stringResource(R.string.admin_resumen_rechazos)) {
        if (resumen.rechazos.isEmpty()) {
            Detalle(stringResource(R.string.admin_resumen_sin_rechazos))
        } else {
            CifraPrincipal(resumen.totalRechazos.toString())
            ListaCantidades(resumen.rechazos, MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun Distancia(resumen: Resumen) {
    TarjetaResumen(stringResource(R.string.admin_resumen_distancia)) {
        val promedio = resumen.distanciaPromedioM
        val porcentaje = ResumenOperacion.porcentaje(resumen.cercanos, resumen.conGpsReal)
        if (promedio == null || porcentaje == null) {
            SinDatos()
        } else {
            CifraPrincipal(
                stringResource(R.string.admin_resumen_distancia_prom, ReglasCheckin.formatoDistancia(promedio))
            )
            Detalle(
                stringResource(
                    R.string.admin_resumen_distancia_cerca,
                    porcentaje,
                    ResumenOperacion.DISTANCIA_CERCANA_M
                )
            )
        }
    }
}

@Composable
private fun Incidencias(resumen: Resumen) {
    TarjetaResumen(stringResource(R.string.admin_resumen_incidencias)) {
        if (resumen.incidenciasPorTipo.isEmpty()) {
            Detalle(stringResource(R.string.admin_resumen_sin_incidencias))
        } else {
            CifraPrincipal(
                stringResource(
                    R.string.admin_resumen_incidencias_conteo,
                    resumen.incidenciasAbiertas,
                    resumen.incidenciasResueltas
                )
            )
            ListaCantidades(resumen.incidenciasPorTipo, MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun Reemplazos(resumen: Resumen) {
    TarjetaResumen(stringResource(R.string.admin_resumen_reemplazos)) {
        CifraPrincipal(resumen.reemplazos.toString())
        Detalle(
            if (resumen.reemplazos == 1) stringResource(R.string.admin_resumen_un_reemplazo)
            else stringResource(R.string.admin_resumen_n_reemplazos)
        )
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
private fun SinDatos() = Detalle(stringResource(R.string.admin_resumen_sin_datos))

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
