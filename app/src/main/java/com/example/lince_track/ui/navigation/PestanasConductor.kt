 package com.example.lince_track.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.ui.alertas.AlertasScreen
import com.example.lince_track.ui.calendario.CalendarioScreen
import com.example.lince_track.ui.perfil.PerfilRealScreen
import com.example.lince_track.ui.perfil.PerfilScreen
import com.example.lince_track.ui.servicios.ServiciosScreen

/**
 * Pestañas principales del conductor con cambio por deslizamiento (swipe).
 *
 * El orden de las páginas calza con `bottomNavItems(Rol.CONDUCTOR)`:
 * 0 = Servicios, 1 = Calendario, 2 = Alertas, 3 = Perfil.
 * El estado del pager se crea en AppNavHost para compartirlo con la barra inferior.
 */
@Composable
fun PestanasConductor(
    pagerState: PagerState,
    onVerTerminados: () -> Unit,
    onServiceClick: (String) -> Unit,
    onRouteClick: (String) -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { pagina ->
        when (pagina) {
            0 -> ServiciosScreen(
                onVerTerminados = onVerTerminados,
                onServiceClick = onServiceClick,
                onRouteClick = onRouteClick
            )
            1 -> CalendarioScreen(onServiceClick = onServiceClick)
            2 -> AlertasScreen(onServiceClick = onServiceClick)
            else -> {
                // PerfilScreen queda como placeholder; las cuentas marcadas usan el perfil real.
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
