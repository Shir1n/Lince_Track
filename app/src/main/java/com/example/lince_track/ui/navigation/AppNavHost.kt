package com.example.lince_track.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.Rol
import com.example.lince_track.data.version.ControlVersionRepository
import com.example.lince_track.data.version.EstadoAvisoVersion
import com.example.lince_track.ui.admin.AdminPanelScreen
import com.example.lince_track.ui.admin.AdminPerfilScreen
import com.example.lince_track.ui.admin.AdminResumenScreen
import com.example.lince_track.ui.ajustes.AjustesScreen
import com.example.lince_track.ui.alertas.AlertasScreen
import com.example.lince_track.ui.calendario.CalendarioScreen
import com.example.lince_track.ui.components.VersionActualizadaDialog
import com.example.lince_track.ui.components.VersionDesactualizadaDialog
import com.example.lince_track.ui.detalle.DetalleServicioScreen
import com.example.lince_track.ui.guia.ConexionConductorScreen
import com.example.lince_track.ui.guia.GuiaAsignacionesScreen
import com.example.lince_track.ui.historial.ServiciosTerminadosScreen
import com.example.lince_track.ui.login.LoginScreen
import com.example.lince_track.ui.perfil.PerfilRealScreen
import com.example.lince_track.ui.ruta.RutaMapaScreen
import com.example.lince_track.ui.theme.BarraSuperior

private fun getBottomNavIndex(route: String?): Int {
    return when (route) {
        Routes.SERVICIOS, Routes.GUIA_ASIGNACIONES, Routes.ADMIN_PANEL -> 0
        Routes.ADMIN_RESUMEN -> 1
        Routes.CALENDARIO -> 2
        Routes.ALERTAS, Routes.GUIA_CONEXION -> 3
        Routes.PERFIL, Routes.GUIA_PERFIL, Routes.ADMIN_PERFIL -> 4
        else -> -1
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.bottomNavEnterTransition(): EnterTransition? {
    val initialIndex = getBottomNavIndex(initialState.destination.route)
    val targetIndex = getBottomNavIndex(targetState.destination.route)

    return if (initialIndex != -1 && targetIndex != -1 && initialIndex != targetIndex) {
        val direction = if (targetIndex > initialIndex) 1 else -1
        slideInHorizontally(
            initialOffsetX = { fullWidth -> direction * fullWidth },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = 200))
    } else {
        null
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.bottomNavExitTransition(): ExitTransition? {
    val initialIndex = getBottomNavIndex(initialState.destination.route)
    val targetIndex = getBottomNavIndex(targetState.destination.route)

    return if (initialIndex != -1 && targetIndex != -1 && initialIndex != targetIndex) {
        val direction = if (targetIndex > initialIndex) -1 else 1
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> direction * fullWidth },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = 200))
    } else {
        null
    }
}

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val MAIN_CONDUCTOR = MAIN
    const val SERVICIOS = "servicios"
    const val DETALLE_SERVICIO = "detalle_servicio/{servicioId}"
    const val RUTA_MAPA = "ruta_mapa/{servicioId}"
    const val CALENDARIO = "calendario"
    const val ALERTAS = "alertas"
    const val TERMINADOS = "terminados"
    const val PERFIL = "perfil"
    const val AJUSTES = "ajustes"

    // Rutas del guía
    const val GUIA_ASIGNACIONES = "guia_asignaciones"
    const val GUIA_CONEXION = "guia_conexion"
    const val GUIA_PERFIL = "guia_perfil"

    // Rutas del administrador
    const val ADMIN_PANEL = "admin_panel"
    const val ADMIN_RESUMEN = "admin_resumen"
    const val ADMIN_PERFIL = "admin_perfil"

    fun detalleServicio(id: String) = "detalle_servicio/$id"
    fun rutaMapa(id: String) = "ruta_mapa/$id"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

internal fun bottomNavItems(rol: Rol?): List<BottomNavItem> = when (rol) {
    Rol.GUIA -> listOf(
        BottomNavItem(Routes.GUIA_ASIGNACIONES, "Asignaciones", Icons.Outlined.Home),
        BottomNavItem(Routes.CALENDARIO, "Calendario", Icons.Outlined.CalendarMonth),
        BottomNavItem(Routes.ALERTAS, "Alertas", Icons.Outlined.Notifications),
        BottomNavItem(Routes.GUIA_PERFIL, "Perfil", Icons.Outlined.Person)
    )
    Rol.ADMINISTRADOR -> listOf(
        BottomNavItem(Routes.ADMIN_PANEL, "Panel", Icons.Outlined.Dashboard),
        BottomNavItem(Routes.ADMIN_RESUMEN, "Resumen", Icons.Outlined.BarChart),
        BottomNavItem(Routes.CALENDARIO, "Calendario", Icons.Outlined.CalendarMonth),
        BottomNavItem(Routes.ADMIN_PERFIL, "Perfil", Icons.Outlined.Person)
    )
    // Conductor
    else -> listOf(
        BottomNavItem(Routes.SERVICIOS, "Servicios", Icons.Outlined.Home),
        BottomNavItem(Routes.CALENDARIO, "Calendario", Icons.Outlined.CalendarMonth),
        BottomNavItem(Routes.ALERTAS, "Alertas", Icons.Outlined.Notifications),
        BottomNavItem(Routes.PERFIL, "Perfil", Icons.Outlined.Person)
    )
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val rol by AuthRepository.rolActual.collectAsState()

    val items = bottomNavItems(rol)
    val pagerState = androidx.compose.runtime.key(rol) {
        rememberPagerState(pageCount = { items.size })
    }
    val scope = rememberCoroutineScope()
    var estadoAvisoVersion by remember { mutableStateOf<EstadoAvisoVersion>(EstadoAvisoVersion.Ninguno) }

    LaunchedEffect(Unit) {
        estadoAvisoVersion = ControlVersionRepository.evaluarAlIniciar(context)
    }

    // Si Android mató el proceso, la navegación se restaura sin pasar por el login:
    // hay que recuperar aquí el perfil de la sesión.
    LaunchedEffect(currentRoute) {
        val usuario = AuthRepository.usuarioActual
        if (currentRoute != null && currentRoute != Routes.LOGIN &&
            usuario != null && AuthRepository.rolActual.value == null
        ) {
            AuthRepository.cargarRolActual(usuario.uid)
        }
        // Al volver al login (cierre de sesión) el pager vuelve a la primera pestaña.
        if (currentRoute == Routes.LOGIN) {
            pagerState.scrollToPage(0)
        }
    }

    when (val aviso = estadoAvisoVersion) {
        is EstadoAvisoVersion.RecienActualizada -> {
            VersionActualizadaDialog(
                version = aviso.version,
                onDismiss = {
                    estadoAvisoVersion = EstadoAvisoVersion.Ninguno
                    scope.launch {
                        ControlVersionRepository.marcarVersionVista(context, aviso.version.versionCode)
                    }
                }
            )
        }
        is EstadoAvisoVersion.Desactualizada -> {
            VersionDesactualizadaDialog(
                local = aviso.local,
                remota = aviso.remota,
                onDismiss = { estadoAvisoVersion = EstadoAvisoVersion.Ninguno }
            )
        }
        EstadoAvisoVersion.Ninguno -> Unit
    }

    val showBottomBar = currentRoute == Routes.MAIN || currentRoute == Routes.MAIN_CONDUCTOR
    val containerColor = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = containerColor,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    items.forEachIndexed { index, item ->
                        val seleccionado = pagerState.currentPage == index
                        NavigationBarItem(
                            selected = seleccionado,
                            onClick = {
                                if (pagerState.currentPage != index) {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                }
                            },
                            icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                                selectedTextColor = MaterialTheme.colorScheme.secondary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // El contenido parte bajo la barra de estado: esta franja le da su color de fondo.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(BarraSuperior.estado)
        )
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = { _ ->
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(route = Routes.MAIN) {
                PestanasPrincipales(
                    rol = rol,
                    pagerState = pagerState,
                    onVerTerminados = { navController.navigate(Routes.TERMINADOS) },
                    onServiceClick = { id -> navController.navigate(Routes.detalleServicio(id)) },
                    onRouteClick = { id -> navController.navigate(Routes.rutaMapa(id)) },
                    onLogout = {
                        AuthRepository.cerrarSesion()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onOpenSettings = { navController.navigate(Routes.AJUSTES) }
                )
            }
            composable(
                route = Routes.DETALLE_SERVICIO,
                arguments = listOf(navArgument("servicioId") { type = NavType.StringType }),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) { backStackEntry ->
                val servicioId = backStackEntry.arguments?.getString("servicioId").orEmpty()
                DetalleServicioScreen(
                    servicioId = servicioId,
                    onBack = { navController.popBackStack() },
                    onOpenRoute = { id -> navController.navigate(Routes.rutaMapa(id)) }
                )
            }
            composable(
                route = Routes.RUTA_MAPA,
                arguments = listOf(navArgument("servicioId") { type = NavType.StringType }),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) { backStackEntry ->
                val servicioId = backStackEntry.arguments?.getString("servicioId").orEmpty()
                RutaMapaScreen(
                    servicioId = servicioId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.CALENDARIO,
                enterTransition = { bottomNavEnterTransition() },
                exitTransition = { bottomNavExitTransition() },
                popEnterTransition = { bottomNavEnterTransition() },
                popExitTransition = { bottomNavExitTransition() }
            ) {
                CalendarioScreen(
                    onServiceClick = { id -> navController.navigate(Routes.detalleServicio(id)) }
                )
            }
            composable(
                route = Routes.ALERTAS,
                enterTransition = { bottomNavEnterTransition() },
                exitTransition = { bottomNavExitTransition() },
                popEnterTransition = { bottomNavEnterTransition() },
                popExitTransition = { bottomNavExitTransition() }
            ) {
                AlertasScreen(
                    onServiceClick = { id -> navController.navigate(Routes.detalleServicio(id)) }
                )
            }
            composable(Routes.AJUSTES) {
                AjustesScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.TERMINADOS) {
                ServiciosTerminadosScreen(
                    onBack = { navController.popBackStack() },
                    onServiceClick = { id -> navController.navigate(Routes.detalleServicio(id)) }
                )
            }

            // Pantallas del guía
            composable(
                route = Routes.GUIA_ASIGNACIONES,
                enterTransition = { bottomNavEnterTransition() },
                exitTransition = { bottomNavExitTransition() },
                popEnterTransition = { bottomNavEnterTransition() },
                popExitTransition = { bottomNavExitTransition() }
            ) {
                GuiaAsignacionesScreen(
                    onVerTerminados = { navController.navigate(Routes.TERMINADOS) },
                    onServiceClick = { id -> navController.navigate(Routes.detalleServicio(id)) },
                    onRouteClick = { id -> navController.navigate(Routes.rutaMapa(id)) }
                )
            }
            composable(
                route = Routes.GUIA_CONEXION,
                enterTransition = { bottomNavEnterTransition() },
                exitTransition = { bottomNavExitTransition() },
                popEnterTransition = { bottomNavEnterTransition() },
                popExitTransition = { bottomNavExitTransition() }
            ) { ConexionConductorScreen() }
            composable(
                route = Routes.GUIA_PERFIL,
                enterTransition = { bottomNavEnterTransition() },
                exitTransition = { bottomNavExitTransition() },
                popEnterTransition = { bottomNavEnterTransition() },
                popExitTransition = { bottomNavExitTransition() }
            ) {
                PerfilRealScreen(
                    onServiceClick = { id -> navController.navigate(Routes.detalleServicio(id)) },
                    onLogout = {
                        AuthRepository.cerrarSesion()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onOpenSettings = { navController.navigate(Routes.AJUSTES) }
                )
            }

            // Pantallas del administrador
            composable(
                route = Routes.ADMIN_PANEL,
                enterTransition = { bottomNavEnterTransition() },
                exitTransition = { bottomNavExitTransition() },
                popEnterTransition = { bottomNavEnterTransition() },
                popExitTransition = { bottomNavExitTransition() }
            ) {
                AdminPanelScreen(
                    onServiceClick = { id -> navController.navigate(Routes.detalleServicio(id)) }
                )
            }
            composable(
                route = Routes.ADMIN_RESUMEN,
                enterTransition = { bottomNavEnterTransition() },
                exitTransition = { bottomNavExitTransition() },
                popEnterTransition = { bottomNavEnterTransition() },
                popExitTransition = { bottomNavExitTransition() }
            ) { AdminResumenScreen() }
            composable(
                route = Routes.ADMIN_PERFIL,
                enterTransition = { bottomNavEnterTransition() },
                exitTransition = { bottomNavExitTransition() },
                popEnterTransition = { bottomNavEnterTransition() },
                popExitTransition = { bottomNavExitTransition() }
            ) {
                AdminPerfilScreen(
                    onLogout = {
                        AuthRepository.cerrarSesion()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onOpenSettings = { navController.navigate(Routes.AJUSTES) }
                )
            }
        }
    }
}
