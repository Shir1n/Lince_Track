package com.example.lince_track.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
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
import com.example.lince_track.ui.ajustes.AjustesScreen
import com.example.lince_track.ui.alertas.AlertasScreen
import com.example.lince_track.ui.calendario.CalendarioScreen
import com.example.lince_track.ui.detalle.DetalleServicioScreen
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

    fun detalleServicio(id: String) = "detalle_servicio/$id"
    fun rutaMapa(id: String) = "ruta_mapa/$id"
}

enum class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Servicios(Routes.SERVICIOS, "Servicios", Icons.Outlined.Home),
    Calendario(Routes.CALENDARIO, "Calendario", Icons.Outlined.CalendarMonth),
    Alertas(Routes.ALERTAS, "Alertas", Icons.Outlined.Notifications),
    Perfil(Routes.PERFIL, "Perfil", Icons.Outlined.Person)
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = BottomNavItem.entries.any { it.route == currentRoute }
    val containerColor = when (currentRoute) {
        Routes.SERVICIOS, Routes.DETALLE_SERVICIO, Routes.RUTA_MAPA, Routes.PERFIL, Routes.ALERTAS -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.background
    }

    Scaffold(
        containerColor = containerColor,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    BottomNavItem.entries.forEach { item ->
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
                    onLoginSuccess = { _ ->
                        // TODO: enrutar según rol cuando existan pantallas para guía y administrador.
                        navController.navigate(Routes.SERVICIOS) {
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
        }
    }
}
