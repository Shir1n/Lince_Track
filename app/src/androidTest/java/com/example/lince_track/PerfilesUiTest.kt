package com.example.lince_track

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.lince_track.data.Rol
import com.example.lince_track.ui.admin.AdminPerfilContenido
import com.example.lince_track.ui.perfil.PerfilRealContenido
import com.example.lince_track.ui.perfil.PerfilScreen
import com.example.lince_track.ui.theme.Lince_TrackTheme
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PerfilesUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun perfilCarlos() {
        composeRule.setContent {
            Lince_TrackTheme {
                PerfilScreen()
            }
        }

        composeRule.onNodeWithText("Carlos Morales R.").assertExists()
        composeRule.onNodeWithText("Vehículo Asignado").assertExists()
        composeRule.onNodeWithText("Resumen Operacional").assertExists()
    }

    @Test
    fun perfilPiloto1() {
        composeRule.setContent {
            Lince_TrackTheme {
                PerfilRealContenido(
                    nombre = "Patricio Lagos",
                    email = "patricio@southbound.cl",
                    telefono = null,
                    rol = Rol.CONDUCTOR,
                    servicios = emptyList(),
                    checkins = emptyMap(),
                    error = null,
                    onLogout = {},
                    onOpenSettings = {},
                    onServiceClick = {}
                )
            }
        }

        composeRule.onAllNodesWithText("Patricio Lagos", substring = false).assertCountEquals(2)
        composeRule.onNodeWithText("JAC S5").assertExists()
        composeRule.onNodeWithText("Año modelo 2015").assertExists()
    }

    @Test
    fun perfilGuia() {
        composeRule.setContent {
            Lince_TrackTheme {
                PerfilRealContenido(
                    nombre = "Raúl Soto",
                    email = "raul@southbound.cl",
                    telefono = null,
                    rol = Rol.GUIA,
                    servicios = emptyList(),
                    checkins = emptyMap(),
                    error = null,
                    onLogout = {},
                    onOpenSettings = {},
                    onServiceClick = {}
                )
            }
        }

        composeRule.onAllNodesWithText("Raúl Soto", substring = false).assertCountEquals(2)
        composeRule.onNodeWithText("Guía Southbound").assertExists()
        composeRule.onNodeWithText("Vehículo Asignado").assertDoesNotExist()
    }

    @Test
    fun perfilAdministrador() {
        composeRule.setContent {
            Lince_TrackTheme {
                AdminPerfilContenido(
                    nombre = "Nacho",
                    email = "nacho@southbound.cl",
                    telefono = null,
                    rol = Rol.ADMINISTRADOR,
                    servicios = emptyList(),
                    checkins = emptyMap(),
                    incidencias = emptyList(),
                    onLogout = {},
                    onOpenSettings = {},
                    hoy = LocalDate.of(2026, 11, 16)
                )
            }
        }

        composeRule.onAllNodesWithText("Nacho", substring = false).assertCountEquals(2)
        composeRule.onNodeWithText("ADMINISTRADOR").assertExists()
        composeRule.onNodeWithText("Operación de Hoy").assertExists()
        composeRule.onNodeWithText("Incidencias").assertExists()
    }

    @Test
    fun cierreSesion() {
        var cerroSesion = false
        composeRule.setContent {
            Lince_TrackTheme {
                PerfilScreen(onLogout = { cerroSesion = true })
            }
        }

        composeRule.onNodeWithText("Cerrar Sesión").performScrollTo().performClick()
        composeRule.runOnIdle { assertTrue(cerroSesion) }
    }
}
