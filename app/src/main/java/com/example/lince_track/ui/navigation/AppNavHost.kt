package com.example.lince_track.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
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
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.Rol
import com.example.lince_track.ui.ajustes.AjustesScreen
import com.example.lince_track.ui.alertas.AlertasScreen
import com.example.lince_track.ui.calendario.CalendarioScreen
import com.example.lince_track.ui.detalle.DetalleServicioScreen
import com.example.lince_track.ui.guia.ConexionConductorScreen
import com.example.lince_track.ui.guia.GuiaAsignacionesScreen
import com.example.lince_track.ui.guia.GuiaPerfilScreen
import com.example.lince_track.ui.login.LoginScreen
import com.example.lince_track.ui.perfil.PerfilScreen
import com.example.lince_track.ui.ruta.RutaMapaScreen
import com.example.lince_track.ui.servicios.ServiciosScreen

object Routes {
    const val LOGIN = "login"
    const val SERVICIOS = "servicios"
    const val DETALLE_SERVICIO = "detalle_servicio/{servicioId}"
    const val RUTA_MAPA = "ruta_mapa/{servicioId}"
    const val CALENDARIO = "calendario"
    const val ALERTAS = "alertas"
    const val PERFIL = "perfil"
    const val AJUSTES = "ajustes"

    // Rutas del guía
    const val GUIA_ASIGNACIONES = "guia_asignaciones"
    const val GUIA_CONEXION = "guia_conexion"
    const val GUIA_PERFIL = "guia_perfil"

    fun detalleServicio(id: String) = "detalle_servicio/$id"
    fun rutaMapa(id: String) = "ruta_mapa/$id"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private fun bottomNavItems(rol: Rol?): List<BottomNavItem> = when (rol) {
    Rol.GUIA -> listOf(
        BottomNavItem(Routes.GUIA_ASIGNACIONES, "Asignaciones", Icons.Outlined.Home),
        BottomNavItem(Routes.CALENDARIO, "Calendario", Icons.Outlined.CalendarMonth),
        BottomNavItem(Routes.GUIA_CONEXION, "Conexión", Icons.Outlined.LocationOn),
        BottomNavItem(Routes.GUIA_PERFIL, "Perfil", Icons.Outlined.Person)
    )
    // Conductor (y por ahora administrador, hasta tener pantallas propias)
    else -> listOf(
        BottomNavItem(Routes.SERVICIOS, "Servicios", Icons.Outlined.Home),
        BottomNavItem(Routes.CALENDARIO, "Calendario", Icons.Outlined.CalendarMonth),
        BottomNavItem(Routes.ALERTAS, "Alertas", Icons.Outlined.Notifications),
        BottomNavItem(Routes.PERFIL, "Perfil", Icons.Outlined.Person)
    )
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val rol by AuthRepository.rolActual.collectAsState()

    val items = bottomNavItems(rol)
    val showBottomBar = items.any { it.route == currentRoute }
    val containerColor = when (currentRoute) {
        Routes.SERVICIOS, Routes.DETALLE_SERVICIO, Routes.RUTA_MAPA, Routes.PERFIL, Routes.ALERTAS,
        Routes.GUIA_ASIGNACIONES, Routes.GUIA_CONEXION, Routes.GUIA_PERFIL -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.background
    }

    Scaffold(
        containerColor = containerColor,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (currentRoute == item.route) FontWeight.Bold else FontWeight.Medium
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
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = { rol ->
                        val destino = if (rol == Rol.GUIA) Routes.GUIA_ASIGNACIONES else Routes.SERVICIOS
                        navController.navigate(destino) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.SERVICIOS) {
                ServiciosScreen(
                    onServiceClick = { id -> navController.navigate(Routes.detalleServicio(id)) },
                    onRouteClick = { id -> navController.navigate(Routes.rutaMapa(id)) }
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
            composable(Routes.CALENDARIO) {
                CalendarioScreen(
                    onServiceClick = { id -> navController.navigate(Routes.detalleServicio(id)) }
                )
            }
            composable(Routes.ALERTAS) { AlertasScreen() }
            composable(Routes.PERFIL) {
                PerfilScreen(
                    onLogout = {
                        AuthRepository.cerrarSesion()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onOpenSettings = { navController.navigate(Routes.AJUSTES) }
                )
            }
            composable(Routes.AJUSTES) {
                AjustesScreen(onBack = { navController.popBackStack() })
            }

            // Pantallas del guía
            composable(Routes.GUIA_ASIGNACIONES) { GuiaAsignacionesScreen() }
            composable(Routes.GUIA_CONEXION) { ConexionConductorScreen() }
            composable(Routes.GUIA_PERFIL) {
                GuiaPerfilScreen(
                    onLogout = {
                        AuthRepository.cerrarSesion()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
