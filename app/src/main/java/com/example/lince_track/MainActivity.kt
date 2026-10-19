package com.example.lince_track

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.lince_track.ui.navigation.AppNavHost
import com.example.lince_track.ui.theme.Lince_TrackTheme
import com.example.lince_track.ui.theme.ThemeMode
import com.example.lince_track.ui.theme.themeModeFlow

import android.content.Context
import java.io.File
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "LinceTrackChile/1.0 (Android; contacto@lincetrack.cl; DuocUC)"
        try {
            Configuration.getInstance().osmdroidTileCache?.deleteRecursively()
            File(cacheDir, "osmdroid").deleteRecursively()
        } catch (_: Exception) {}
        enableEdgeToEdge()
        setContent {
            val themeMode by this@MainActivity.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            Lince_TrackTheme(themeMode = themeMode) {
                AppNavHost()
            }
        }
    }
}
