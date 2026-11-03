package com.example.lince_track.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.Rol
import com.example.lince_track.ui.admin.AdminPanelScreen
import com.example.lince_track.ui.admin.AdminPerfilScreen
import com.example.lince_track.ui.alertas.AlertasScreen
import com.example.lince_track.ui.calendario.CalendarioScreen
import com.example.lince_track.ui.guia.GuiaAsignacionesScreen
import com.example.lince_track.ui.perfil.PerfilRealScreen
import com.example.lince_track.ui.perfil.PerfilScreen
import com.example.lince_track.ui.servicios.ServiciosScreen
import kotlinx.coroutines.launch

/**
 * Pestañas principales con navegación por deslizamiento horizontal (swipe).
 *
 * Soporta de manera global todos los roles:
 * - Conductor: 0 = Servicios, 1 = Calendario, 2 = Alertas, 3 = Perfil.
 * - Guía: 0 = Asignaciones, 1 = Calendario, 2 = Alertas, 3 = Perfil.
 * - Administrador: 0 = Panel, 1 = Calendario, 2 = Perfil.
 */
@Composable
fun PestanasPrincipales(
    rol: Rol?,
    pagerState: PagerState,
    onVerTerminados: () -> Unit,
    onServiceClick: (String) -> Unit,
    onRouteClick: (String) -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    BackHandler(enabled = pagerState.currentPage != 0) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { pagina ->
        when (rol) {
            Rol.ADMINISTRADOR -> {
                when (pagina) {
                    0 -> AdminPanelScreen(onServiceClick = onServiceClick)
                    1 -> CalendarioScreen(onServiceClick = onServiceClick)
                    else -> AdminPerfilScreen(onLogout = onLogout, onOpenSettings = onOpenSettings)
                }
            }
            Rol.GUIA -> {
                when (pagina) {
                    0 -> GuiaAsignacionesScreen(
                        onVerTerminados = onVerTerminados,
                        onServiceClick = onServiceClick,
                        onRouteClick = onRouteClick
                    )
                    1 -> CalendarioScreen(onServiceClick = onServiceClick)
                    2 -> AlertasScreen(onServiceClick = onServiceClick)
                    else -> PerfilRealScreen(onLogout = onLogout, onOpenSettings = onOpenSettings)
                }
            }
            else -> {
                // Conductor / Transportista
                when (pagina) {
                    0 -> ServiciosScreen(
                        onVerTerminados = onVerTerminados,
                        onServiceClick = onServiceClick,
                        onRouteClick = onRouteClick
                    )
                    1 -> CalendarioScreen(onServiceClick = onServiceClick)
                    2 -> AlertasScreen(onServiceClick = onServiceClick)
                    else -> {
                        val perfil by AuthRepository.perfilActual.collectAsState()
                        if (perfil?.perfilNuevo == true) {
                            PerfilRealScreen(onLogout = onLogout, onOpenSettings = onOpenSettings)
                        } else {
                            PerfilScreen(onLogout = onLogout, onOpenSettings = onOpenSettings)
                        }
                    }
                }
            }
        }
    }
}
