package com.example.lince_track.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lince_track.ui.alertas.AlertasScreen
import com.example.lince_track.ui.calendario.CalendarioScreen
import com.example.lince_track.ui.login.LoginScreen
import com.example.lince_track.ui.perfil.PerfilScreen
import com.example.lince_track.ui.servicios.ServiciosScreen

object Routes {
    const val LOGIN = "login"
    const val SERVICIOS = "servicios"
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

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavItem.entries.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
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
            composable(Routes.SERVICIOS) { ServiciosScreen() }
            composable(Routes.CALENDARIO) { CalendarioScreen() }
            composable(Routes.ALERTAS) { AlertasScreen() }
            composable(Routes.PERFIL) { PerfilScreen() }
        }
    }
}
