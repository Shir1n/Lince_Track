package com.example.lince_track

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.lince_track.ui.navigation.AppNavHost
import com.example.lince_track.ui.theme.AppLanguage
import com.example.lince_track.ui.theme.Lince_TrackTheme
import com.example.lince_track.ui.theme.ThemeMode
import com.example.lince_track.ui.theme.appLanguageFlow
import com.example.lince_track.ui.theme.localizedContext
import com.example.lince_track.ui.theme.themeModeFlow
import com.example.lince_track.util.LinceLogger
import com.example.lince_track.util.NotificadorLocal
import com.google.firebase.FirebaseApp
import org.osmdroid.config.Configuration
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        val systemLocale = resources.configuration.locales[0] ?: Locale.getDefault()

        val prefs = getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        prefs.edit().putString("osmdroid.userAgentValue", "LinceTrackChile/1.0 (cl.duoc.lince_track)").apply()
        val osmContext = object : ContextWrapper(applicationContext) {
            override fun getPackageName(): String = "cl.duoc.lince_track"
        }
        Configuration.getInstance().load(osmContext, prefs)
        Configuration.getInstance().userAgentValue = "LinceTrackChile/1.0 (cl.duoc.lince_track)"
        LinceLogger.i(
            tag = "APP",
            message = "MainActivity iniciada y osmdroid configurado",
            details = "userAgent='${Configuration.getInstance().userAgentValue}', normalizedUA='${Configuration.getInstance().normalizedUserAgent}'"
        )

        NotificadorLocal.iniciar(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        enableEdgeToEdge()
        setContent {
            val themeMode by this@MainActivity.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val appLanguage by this@MainActivity.appLanguageFlow.collectAsState(initial = AppLanguage.SYSTEM)
            val targetLocale = remember(appLanguage, systemLocale) {
                appLanguage.resolveLocale(systemLocale)
            }
            val localizedCtx = remember(appLanguage, systemLocale) {
                this@MainActivity.localizedContext(appLanguage, systemLocale)
            }

            LaunchedEffect(targetLocale) {
                Locale.setDefault(targetLocale)
            }

            CompositionLocalProvider(
                LocalContext provides localizedCtx,
                LocalConfiguration provides localizedCtx.resources.configuration,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                Lince_TrackTheme(themeMode = themeMode) {
                    AppNavHost()
                }
            }
        }
    }
}
