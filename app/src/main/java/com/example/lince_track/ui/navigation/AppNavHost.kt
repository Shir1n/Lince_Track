package com.example.lince_track.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
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
import com.example.lince_track.ui.ajustes.AjustesScreen
import com.example.lince_track.ui.alertas.AlertasScreen
import com.example.lince_track.ui.calendario.CalendarioScreen
import com.example.lince_track.ui.detalle.DetalleServicioScreen
import com.example.lince_track.ui.login.LoginScreen
import com.example.lince_track.ui.perfil.PerfilScreen
import com.example.lince_track.ui.ruta.RutaMapaScreen
import com.example.lince_track.ui.servicios.ServiciosScreen

private fun getBottomNavIndex(route: String?): Int {
    return when (route) {
        Routes.SERVICIOS -> 0
        Routes.CALENDARIO -> 1
        Routes.ALERTAS -> 2
        Routes.PERFIL -> 3
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
    val containerColor = MaterialTheme.colorScheme.background

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
                    onLoginClick = { _, _ ->
                        navController.navigate(Routes.SERVICIOS) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Routes.SERVICIOS,
                enterTransition = { bottomNavEnterTransition() },
                exitTransition = { bottomNavExitTransition() },
                popEnterTransition = { bottomNavEnterTransition() },
                popExitTransition = { bottomNavExitTransition() }
            ) {
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
            ) { AlertasScreen() }
            composable(
                route = Routes.PERFIL,
                enterTransition = { bottomNavEnterTransition() },
                exitTransition = { bottomNavExitTransition() },
                popEnterTransition = { bottomNavEnterTransition() },
                popExitTransition = { bottomNavExitTransition() }
            ) {
                PerfilScreen(
                    onLogout = {
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
