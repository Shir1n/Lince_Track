<div align="center">

<img src="app/src/main/res/drawable/logo_solo.png" width="240" alt="Lince Track logo">

#  Lince Track

**Coordinación de servicios turísticos entre conductores, guías y coordinadores.**

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-27-orange?style=for-the-badge)
![Estado](https://img.shields.io/badge/Estado-Prototipo-yellow?style=for-the-badge)as

</div>

---

##  Descripción

**Lince Track** centraliza la información de los servicios turísticos asignados a cada
transportista, permitiendo consultar, **confirmar o rechazar** asignaciones y hacer
seguimiento del estado de los servicios.

##  Funcionalidades

-  Inicio de sesión del transportista.
-  Dashboard con los servicios del día y saldo acumulado.
-  Detalle de servicio (ruta, horario, inclusiones y guía asignado).
-  Confirmación /  rechazo de asignaciones con motivo.
- ️ Calendario de servicios.
-  Alertas.
-  Perfil del usuario.

##  Tecnologías utilizadas

| Área | Tecnología |
| --- | --- |
| IDE | Android Studio |
| Lenguaje | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| Build | Gradle (AGP 9.3.3) |
| Navegación | Navigation Compose 2.10.2 |
| Diagramas | Mermaid |

##  Cómo ejecutar

### Requisitos
- **Android Studio** (última versión)
- **JDK 21 o superior** (el proyecto viene configurado con JDK 25)
- **Android SDK** (Android Studio lo instala; la ruta queda en `local.properties`)

### Opción A — Android Studio 
1. Abre Android Studio → **Open** → selecciona la carpeta del proyecto.
2. Espera a que termine la **sincronización de Gradle** (la primera vez descarga
   Gradle 9.5 y las dependencias; puede tardar unos minutos).
3. Conecta un dispositivo o inicia un **emulador**.
4. Pulsa **Run ▶** (Shift+F10). La app se compila e instala sola.

### Opción B — Terminal (genera el APK)
```bash
git clone https://github.com/Shir1n/Lince_Track.git
cd Lince_Track
```

```powershell
# Windows (PowerShell)
.\gradlew.bat assembleDebug
```
```bash
# macOS / Linux
./gradlew assembleDebug
```

El APK de debug queda en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Para instalarlo directamente en un dispositivo conectado:

```powershell
.\gradlew.bat installDebug
```

##  Identidad visual

El **lince** simboliza agilidad, visión y adaptabilidad; las **montañas** representan
el turismo en Chile y la conexión con la naturaleza.

| Color | HEX | Uso |
| --- | --- | --- |
| Principal | `#0F2B48` | Top bars y botones principales |
| Secundario | `#0A8F82` | Acentos y acciones (Confirmar) |
| Advertencia | `#C67A18` | Estados "Pendiente" |
| Fondo | `#F8FAFC` | Fondo general |
| Texto | `#0F172A` | Textos principales |

##  Estructura

```text
app/src/main/java/com/example/lince_track/
├── MainActivity.kt
└── ui/
    ├── navigation/   AppNavHost.kt
    ├── login/
    ├── servicios/
    ├── detalle/
    ├── calendario/
    ├── alertas/
    ├── perfil/
    └── theme/
```

##  Integrantes

| Integrante | Rol |
| --- | --- |
| Benjamin Agüero | Frontend |
| Luciano Garrido | Backend |
| Raúl Ferrini | Frontend Dev |
| Ignacio Salazar | Scrum Master |



