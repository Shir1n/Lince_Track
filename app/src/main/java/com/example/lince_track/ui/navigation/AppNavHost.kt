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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lince_track.ui.alertas.AlertasScreen
import com.example.lince_track.ui.calendario.CalendarioScreen
import com.example.lince_track.ui.detalle.DetalleServicioScreen
import com.example.lince_track.ui.login.LoginScreen
import com.example.lince_track.ui.perfil.PerfilScreen
import com.example.lince_track.ui.servicios.ServiciosScreen
import com.example.lince_track.ui.theme.BackgroundColor
import com.example.lince_track.ui.theme.NavyBlue
import com.example.lince_track.ui.theme.NavyBlueDark

object Routes {
    const val LOGIN = "login"
    const val SERVICIOS = "servicios"
    const val DETALLE_SERVICIO = "detalle_servicio"
    const val CALENDARIO = "calendario"
    const val ALERTAS = "alertas"
    const val PERFIL = "perfil"
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
        Routes.SERVICIOS, Routes.DETALLE_SERVICIO, Routes.PERFIL -> NavyBlueDark
        Routes.LOGIN -> BackgroundColor
        else -> MaterialTheme.colorScheme.background
    }

    Scaffold(
        containerColor = containerColor,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White
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
                                selectedIconColor = Color.White,
                                selectedTextColor = NavyBlue,
                                unselectedIconColor = NavyBlue,
                                unselectedTextColor = NavyBlue,
                                indicatorColor = NavyBlue
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
                    onLoginClick = { _, _ ->
                        navController.navigate(Routes.SERVICIOS) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.SERVICIOS) {
                ServiciosScreen(
                    onServiceClick = { navController.navigate(Routes.DETALLE_SERVICIO) }
                )
            }
            composable(
                route = Routes.DETALLE_SERVICIO,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                DetalleServicioScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CALENDARIO) { CalendarioScreen() }
            composable(Routes.ALERTAS) { AlertasScreen() }
            composable(Routes.PERFIL) {
                PerfilScreen(
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
