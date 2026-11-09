package com.example.lince_track.ui.navigation

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import com.example.lince_track.data.Rol

/**
 * Pestañas principales del conductor con cambio por deslizamiento (swipe).
 * Mantenido por compatibilidad; delega en [PestanasPrincipales].
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
    PestanasPrincipales(
        rol = Rol.CONDUCTOR,
        pagerState = pagerState,
        onVerTerminados = onVerTerminados,
        onServiceClick = onServiceClick,
        onRouteClick = onRouteClick,
        onLogout = onLogout,
        onOpenSettings = onOpenSettings
    )
}
