package com.example.lince_track

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.lince_track.ui.login.LoginScreen
import com.example.lince_track.ui.theme.Lince_TrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lince_TrackTheme {
                LoginScreen(
                    onLoginClick = { email, password ->
                        // TODO: Lógica de autenticación
                    },
                    onForgotPasswordClick = {
                        // TODO: Navegar a recuperación de contraseña
                    }
                )
            }
        }
    }
}