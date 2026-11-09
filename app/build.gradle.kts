import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

// Valores locales (no se suben a git): local.properties está en .gitignore.
val localProps = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

// Fuente única de versión del proyecto: version.json en la raíz del repositorio.
val archivoVersion = rootProject.file("version.json")
val versionJsonTexto = if (archivoVersion.exists()) archivoVersion.readText(Charsets.UTF_8) else ""
val versionCodeCalculado = Regex(""""versionCode"\s*:\s*(\d+)""")
    .find(versionJsonTexto)?.groupValues?.get(1)?.toIntOrNull() ?: 1
val versionNameCalculado = Regex(""""versionName"\s*:\s*"([^"]+)"""")
    .find(versionJsonTexto)?.groupValues?.get(1) ?: "1.0"
val versionJsonB64 = Base64.getEncoder().encodeToString(versionJsonTexto.toByteArray(Charsets.UTF_8))

android {
    namespace = "com.example.lince_track"
    compileSdk {
        version = release(37)
    }

    signingConfigs {
        getByName("debug") {
            val keystoreCompartido = file("debug.keystore")
            if (keystoreCompartido.exists()) {
                storeFile = keystoreCompartido
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    defaultConfig {
        applicationId = "cl.duoc.lince_track"
        minSdk = 27
        targetSdk = 37
        versionCode = versionCodeCalculado
        versionName = versionNameCalculado

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // API de Southbound. Si LINCE_API_KEY está vacía, la app usa datos de ejemplo.
        buildConfigField("String", "LINCE_API_BASE_URL", "\"https://ws.southbound.travel/lince-api/\"")
        buildConfigField("String", "LINCE_API_KEY", "\"${localProps.getProperty("LINCE_API_KEY", "").trim()}\"")
        buildConfigField("String", "LOCAL_VERSION_JSON_B64", "\"$versionJsonB64\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.osmdroid.android)
    implementation(libs.play.services.location)
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
}