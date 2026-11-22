package com.example.lince_track

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.lince_track.ui.components.AvisoPrivacidadAlIniciar
import com.example.lince_track.ui.navigation.AppNavHost
import com.example.lince_track.ui.theme.Lince_TrackTheme
import com.example.lince_track.ui.theme.ThemeMode
import com.example.lince_track.ui.theme.themeModeFlow
import com.example.lince_track.util.LinceLogger
import com.example.lince_track.data.TokenFcmRepository
import com.example.lince_track.data.local.BaseLocal
import com.example.lince_track.util.SincronizarHitosWorker
import kotlinx.coroutines.launch
import com.example.lince_track.util.AperturaNotificacion
import com.example.lince_track.util.NotificadorLocal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.google.firebase.FirebaseApp
import org.osmdroid.config.Configuration

// AppCompatActivity (y no ComponentActivity) para que el idioma elegido en la app se aplique también en Android 12 o
// anterior. Necesita un tema de AppCompat (res/values/themes.xml).
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

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
        TokenFcmRepository.iniciar(CoroutineScope(SupervisorJob() + Dispatchers.IO))
        // Base local (Room): si quedó algo marcado sin conexión, se vuelve a programar el envío.
        val baseLocal = BaseLocal.iniciar(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            if (baseLocal.hitosPendientes().isNotEmpty()) SincronizarHitosWorker.programar(applicationContext)
        }
        // Si se abrió tocando una notificación, queda pendiente abrir ese servicio.
        AperturaNotificacion.desde(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        enableEdgeToEdge()
        setContent {
            val themeMode by this@MainActivity.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)

            // El idioma lo aplica Android a toda la app (ver ui/theme/LanguagePreference.kt): aquí no hace falta nada.
            Lince_TrackTheme(themeMode = themeMode) {
                AppNavHost()
                // Encima de todo: la primera vez que se abre la app (o si cambia el aviso).
                AvisoPrivacidadAlIniciar()
            }
        }
    }

    /** Con la app ya abierta (singleTask), tocar una notificación llega aquí. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AperturaNotificacion.desde(intent)
    }
}
