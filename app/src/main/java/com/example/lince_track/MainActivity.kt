package com.example.lince_track

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.lince_track.ui.navigation.AppNavHost
import com.example.lince_track.ui.theme.Lince_TrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lince_TrackTheme {
                AppNavHost()
            }
        }
    }
}
