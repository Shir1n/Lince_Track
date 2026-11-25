package com.example.lince_track

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lince_track.data.Rol
import com.example.lince_track.ui.login.LoginScreen
import com.example.lince_track.ui.login.LoginViewModel
import com.example.lince_track.ui.theme.Lince_TrackTheme
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Formulario de inicio de sesión con datos inválidos: muestra el error y nunca llama a Firebase. */
class LoginUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    // Los textos se leen de los recursos: así el test pasa con el celular en español o en inglés.
    private fun texto(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val intentos = mutableListOf<String>()
    private var rolEntregado: Rol? = null

    private fun abrirLogin() {
        val viewModel = LoginViewModel(
            uidActual = { null },
            cargarRol = { null },
            iniciar = { correo, _ -> intentos += correo; Result.success(Rol.CONDUCTOR) }
        )
        composeRule.setContent {
            Lince_TrackTheme {
                LoginScreen(onLoginSuccess = { rolEntregado = it }, viewModel = viewModel)
            }
        }
    }

    @Test
    fun camposVaciosMuestranUnError() {
        abrirLogin()

        composeRule.onNodeWithText(texto(R.string.login_iniciar_sesion)).performClick()

        composeRule.onNodeWithText(texto(R.string.login_error_campos_vacios)).assertExists()
        assertTrue(intentos.isEmpty())
        assertNull(rolEntregado)
    }

    @Test
    fun correoInvalidoMuestraUnErrorYNoIniciaSesion() {
        abrirLogin()

        composeRule.onNodeWithText(texto(R.string.login_correo_placeholder)).performTextInput("carlos.southbound")
        composeRule.onNodeWithText(texto(R.string.login_password_placeholder)).performTextInput("clave")
        composeRule.onNodeWithText(texto(R.string.login_iniciar_sesion)).performClick()

        composeRule.onNodeWithText(texto(R.string.login_error_correo_invalido)).assertExists()
        assertTrue(intentos.isEmpty())
        assertNull(rolEntregado)
    }

    @Test
    fun datosValidosIniciaSesionConElRol() {
        abrirLogin()

        composeRule.onNodeWithText(texto(R.string.login_correo_placeholder)).performTextInput("conductor@southbound.cl")
        composeRule.onNodeWithText(texto(R.string.login_password_placeholder)).performTextInput("clave")
        composeRule.onNodeWithText(texto(R.string.login_iniciar_sesion)).performClick()
        composeRule.waitForIdle()

        assertTrue(intentos == listOf("conductor@southbound.cl"))
        assertTrue(rolEntregado == Rol.CONDUCTOR)
    }
}
