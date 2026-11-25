# Lince Track — contexto para Claude

App Android para coordinar servicios turísticos de Southbound (Puerto Varas) entre
conductores (transportistas), guías y administración. Proyecto académico de equipo.
Responde y escribe la interfaz en **español de Chile**.

## Equipo
| Persona | Rol | Foco actual |
|---|---|---|
| Ignacio Salazar (Shir1n, dueño del repo) | Scrum Master y coordinación técnica | Firebase, backend, nube, Trello, README y comunicación con Southbound |
| Benjamin Agüero | Frontend | Detalle, calendario, colores, alertas |
| Raúl Ferrini | Frontend | Pantalla de mapa (`ui/ruta/`) |
| Luciano Garrido | Backend → frontend | Panel del administrador (`ui/admin/`). **No tiene acceso a Firebase** |

Son **4 integrantes**: Shir1n e Ignacio Salazar son la misma persona.

Estos focos son sugerencias: cada quien trabaja en todo, así que no hace falta frenar ni coordinar antes de tocar el
área de otra persona.

## Evaluación del curso (DSY1105, revisado el 13-11-2026)
El material del docente está en `contexto/` (ignorada en Git: trae la API key y un video de 250 MB; **no subirla**):
pautas `EA4_...Encargo` y `EA5_...Defensa`, caso `Formato_Caso_DSY1105`, guía `ACCESO_ESTUDIANTES.md`, colección de
Postman y la transcripción de la reunión con Southbound. Desde el 21-11-2026 también trae (en el computador de Shir1n)
la cuenta de servicio de Firebase (`lince-track-66c55-firebase-adminsdk-*.json`), la keystore de release
(`lince-release.jks`) y un `local.properties` con sus contraseñas. **Nunca** mostrar ni subir esos archivos.
- **EA4 (6 %, en parejas):** entrega en AVA en la semana 15 (código Android y backend, APK release firmada, README,
  evidencia de pruebas y enlace a GitHub público). Cambios en el repositorio después del plazo = nota 1,0.
- **EA5 (24 %, individual, 15 min):** defensa en la semana 16, sin IA, con la APK en un celular real. El 40 % es una
  **modificación en vivo** que recorra Compose → ViewModel → endpoint → base de datos (ej.: radio de 150 a 100 m,
  tolerancia de puntualidad, motivo obligatorio de incidencia, campo "precisión del GPS").
- **Lo que piden y falta hoy:**
  - ~~Backend propio **Spring Boot** + Swagger/OpenAPI + JWT/roles + BD~~ en `backend/` y ya usado por la app
    (bloqueos, geocercas y check-in), **publicado en Cloud Run con Cloud SQL** desde el 22-11-2026 (ver "Backend propio").
  - ~~Check-in **bloqueado** fuera de la geocerca o tarde~~ **Hecho (PR 53, 21-11-2026):** ver "Check-in con geocerca".
  - ~~Disponibilidad: bloquear días con 7 días de anticipación~~ **Hecho (PR 51).**
  - ~~Room + WorkManager~~ **Hecho (PR 58, 22-11-2026):** cola de check-ins sin conexión, caché de geocercas y reglas,
    sincronización con reintentos e idempotencia (ver "Check-in sin conexión").
  - ~~Español e inglés con selector de idioma persistente~~ **Hecho (PR 52 y 54).**
  - ~~JUnit + MockK > 90 % y al menos un Compose UI Test~~ **Hecho:** 343 tests de la app (93,4 % de líneas en
    ViewModel/Repository) y 10 Compose UI Tests que pasan en el celular de la defensa (24-11-2026).
  - ~~APK release firmada~~ **Publicada en GitHub Releases** (v1.5 el 24-11-2026; ver "Comandos"). Falta instalarla en el
    celular de la defensa (Infinix X6837, el mismo que se usa por USB).
  - ~~Usar `/guias`, `/transportistas`, `/clientes`, `/catalogos` y `/resumen`~~ **Hecho** (PR 60, 62 y 68); los errores
    muestran el `requestId`.
  - ~~Última posición del transportista durante un servicio activo~~ **Hecho (PR 68):** ver "Última posición".
- **Pedidos de Southbound en la reunión (11-09-2026) que aún no están:** datos de la agencia (vehículo, polera, letrero),
  qué incluye la excursión, reporte mensual con servicios y dinero acumulado, y que los servicios cancelados igual se
  paguen al transportista. Un guía puede hacer un tour de medio día y un transfer el mismo día, pero no otro servicio
  si tiene un tour de día completo.

## Estado actual (24-11-2026)
- **Objetivo cercano:** la presentación del viernes 27-11 (08:30-08:50, grupo Patagonia-Dev) es para "vender" la app.
  Diferenciadores: check-in con geocerca validado por el backend en la nube, modo sin conexión, notificaciones con la
  app cerrada, última posición del conductor y derechos de privacidad. Para mostrar las notificaciones hacen falta dos
  dispositivos (conductor y administrador). Guía de la demo y scripts (`mover-geocerca.mjs`, `limpiar-pruebas.mjs`) en
  `contexto/demo/` (fuera de Git).
- **Todo está en `master`** hasta el PR 68 (última posición y agencias). Versión 1.6 en `release/1.6`.
- **Datos limpios el 24-11-2026:** Firestore sin respuestas, check-ins, estados, reemplazos, incidencias ni solicitudes
  (se conservan `usuarios` y `tokensFcm`), backend sin hitos ni bloqueos, Storage sin fotos y geocercas en su lugar.
- **Clave de la API disponible** (vence el 31-12-2026). Los datos de la API llegan hasta el 15-12-2026. Pasan los 343
  tests de la app (93,4 %), 10 Compose UI Tests, 128 de reglas, 15 de las funciones y 89 del backend (96 %).
- **Reglas:** las publicadas son las de `master` (incluye `tokensFcm` y `solicitudesPrivacidad`; Storage, `perfil/`).
  Si alguien cambia `firestore.rules`, publicarla antes de probar: si falta, aceptar, rechazar o reportar da error.
- **Cuentas de prueba** en `usuarios/{uid}`:
  - Carlos Morales R.: `conductor`, `transportistaId: 37`, sin `perfilNuevo` (se muestra el perfil completo de ejemplo).
  - Patricio Lagos: `conductor`, `transportistaId: 43`, `perfilNuevo: true`. Es el reemplazo en el video.
  - Alfredo Pizarro: `guia`, `guiaId: 35`. "Admin 1": administrador sin ID. `telefono` es texto con código de país
    (`+569...`).
  - IDs de PUX desde el 16-11-2026, con custom claims aplicados (`rol` y el ID). Carlos y Alfredo comparten el tour real
    3008/3009 del 20-11 a las 08:30. Tras cambiar un rol o un ID en `usuarios/{uid}`, volver a correr
    `backend/scripts/sincronizar-claims.mjs --aplicar` y cerrar sesión en la app.
  - Los correos y la contraseña están en `USUARIOS_TEST.md`, y `LoginScreen` tiene botones que rellenan esas cuentas.
- **Recorrido del video:** guía y conductor aceptan "Saltos del Petrohué" (8941/8942) → el conductor hace check-in y
  reporta "Falla mecánica" → el administrador responde y envía reemplazo → Patricio acepta y hace check-in y check-out →
  el administrador la marca resuelta. Tour de mañana para otra prueba: "Lago Todos los Santos" (8960/8961). Otro tour de
  hoy, con Miguel Soto y Roberto Díaz, sirve para mostrar un reemplazo: "Frutillar y Teatro del Lago" (8970/8971).
  **Esos IDs solo existen en los datos de ejemplo.** En la API real, un tour con ambos asignados y pendiente (RQ) es
  "Tour Frutillar y Lago Llanquihue" del 20-11 a las 08:30: 3008 (EX, transportista 37) y 3009 (GS, guía 35). Otro:
  3067/3068 del 22-11 (transportista 37, guía 33, el guía ya en CF).
- **Limpiar pruebas:** lo ya aceptado no se puede volver a aceptar. Para repetir hay que borrar en Firebase el documento
  en `estadosServicio` y los de `respuestas`; también `asignaciones` si se reasignó. Lo aceptado antes de agregar
  `telefono` se guardó sin teléfono.
- **Lo que sigue para el video, en orden:**
  1. Número de soporte real o deshabilitar los botones (`SoporteDialog.kt`).
  2. Antes de grabar: abrir la app y cerrar una vez "¡Estás actualizado!", que habla del equipo y aparece una sola vez
     por versión. Las respuestas de 8941/8942 ya se borraron (03-11-2026) y el administrador se llama "Admin 1"
     (mejor un nombre real o "Coordinación Southbound"). Tras cada ensayo, limpiar de nuevo lo aceptado y los check-ins
     hechos fuera de hora: la puntualidad del perfil y del resumen es real.
  3. ~~Decidir la foto de `PerfilScreen`~~: se quitó; cada cuenta sube la suya (ver "Foto de perfil").
- **Resumen para los dueños** (`ui/admin/AdminResumenScreen`): sin clave de la API, "Últimos 7 días" suma el historial
  inventado de `ResumenDemostracion`, siempre con un aviso visible; "Hoy" y las cifras con clave son solo datos reales.
  Los márgenes (15 minutos de puntualidad, 500 m de distancia) son provisionales: falta confirmarlos con Southbound.
- **Fuera por ahora:** fotos (ya se pueden, con Blaze y Storage), ubicación en vivo y chat (ya está WhatsApp).
- **No mostrar en el video:** botones del líder del grupo (sin teléfono) y botones de cuentas de prueba en el login.
- **Limitaciones conocidas:**
  - Historial solo alcanza a los últimos 7 días cargados.
  - Quien es reemplazado deja su check-in sin cerrar, ya no puede abrir el servicio, y si se le devuelve aparece aceptado
    con ese check-in.
  - `estadosServicio` lo leen todas las cuentas con rol, incluido el teléfono: revisarlo antes de usar datos reales.
- **Nombres de la API:** el panel muestra el de la fuente mientras nadie responda; las cuentas muestran su `nombre`.

## Stack
- Kotlin 2.2.10, Jetpack Compose + Material 3, Navigation Compose 2.10.2
- AGP 9.3.3, Gradle 9.5, compileSdk/targetSdk 37, minSdk 27, Java 11
- Firebase BoM 34.19.0: Auth + Firestore + Messaging (sin Analytics). Plan **Blaze** desde el 21-11-2026.
- Cloud Functions de 2.ª generación en Node 22 (`functions/`, firebase-functions 7 y firebase-admin 14), región
  `southamerica-west1` (Santiago, la misma de Firestore).
- Retrofit 3.0.0 + kotlinx.serialization 1.9.0 (API de Southbound)
- osmdroid (mapa OSM), Play Services Location, Coil, DataStore (tema)

## Comandos (Windows, PowerShell)
```powershell
.\gradlew.bat assembleDebug          # compilar
.\gradlew.bat testDebugUnitTest      # tests unitarios (incluye LinceApiTest)
.\gradlew.bat jacocoPautaReport      # cobertura de ViewModel + Repository (la pauta pide > 90 %)
.\gradlew.bat installDebug           # instalar en dispositivo/emulador
```
- **Tests de repositorios y ViewModel (desde el 20-11-2026, 94,7 % de líneas):**
  - Los repositorios siguen siendo `object`, pero Firebase y la fuente de servicios se obtienen con `Reemplazable`
    (`firestoreReemplazable`, `authReemplazable`, `fuenteReemplazable`), que los tests cambian por simulaciones de
    MockK. La app usa siempre la real. Un repositorio nuevo debe seguir el mismo patrón (o recibir todo por constructor).
  - Los ViewModel reciben sus dependencias por el constructor con la app real como valor por defecto (Compose los crea
    con `viewModel()`). En sus tests se pasan lambdas, no mocks de MockK: MockK falla al simular funciones `suspend`.
  - Ayudas en `app/src/test/.../data/FirebaseSimulado.kt` (tareas, documentos, escuchas) y `Datos.kt` (DTO de prueba).
  - Al terminar un test, dejar `AuthRepository` con `FirebaseSimulado.authSinSesion()`, nunca con `null`: en la JVM no
    hay Firebase real y `ServicioRepository` (que sigue vivo entre tests) dejaría de escuchar.
  - `ControlVersionRepository` se prueba con un DataStore en un archivo temporal y un servidor HTTP local.
- Se prueba en celulares reales por USB: no crear emuladores de Android. `adb` está en `Android\Sdk\platform-tools`, y si
  Java no está en el PATH se puede usar el `jbr` de Android Studio (`C:\Program Files\Android\Android Studio\jbr` o
  `D:\Android\AndroidStudio\jbr`, según el computador).
- Desde el PR 36 todos firman con `app/debug.keystore` (versionada, solo para debug: el repositorio es público). Un
  celular con una versión firmada antes con otra clave rechaza instalar (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) y hay que
  desinstalar una vez. Se pierde la sesión y lo no enviado: confirmarlo antes con quien usa ese celular.
- **APK release:** `.\gradlew.bat assembleRelease` firma con la keystore `lince-release.jks` de Shir1n (fuera del
  repo: `D:\IA\Fire` en un computador y `contexto/` en el otro); la ruta y las contraseñas están en su `local.properties` (`LINCE_RELEASE_*`). Sin esos datos sale
  sin firmar. Instrucciones en el README. Un celular con la versión debug instalada no acepta la release encima
  (firma distinta): hay que desinstalar primero. `androidx.fragment` va fijo en 1.8.9 porque Play Services trae la
  1.1.0 y el lint de release lo rechaza. En `res/` las imágenes deben tener la extensión de su formato real (un JPEG
  llamado `.png` rompe la compilación de release).
- La versión sale de `version.json` (raíz). La app compara con el de `master` en GitHub al abrir; si el APK de
  `releases/latest` no existe, el aviso de versión desactualizada no ofrece descargar.
- `gh` puede no estar instalado.
- **Firebase CLI:** viene en `firestore-tests/node_modules` (no hace falta instalarlo aparte). Tras `npx firebase login`
  (desde `firestore-tests/`, con la cuenta dueña del proyecto) se publica desde la raíz con
  `.\firestore-tests\node_modules\.bin\firebase deploy --only firestore:rules` o `--only functions`. La cuenta de
  servicio no sirve para publicar (Google responde 403). Las imágenes de las funciones se borran tras 1 día.
- **Tests de las funciones:** `cd functions; npm install; npm test` (`node --test`, sin Firebase).
- **Backend:** la app usa por defecto el de Cloud Run (`BACKEND_NUBE` en `app/build.gradle.kts`). Para probar uno local:
  `LINCE_BACKEND_URL=http://localhost:8080/` en `local.properties` y `adb reverse tcp:8080 tcp:8080` (el reenvío se
  pierde si el cable se desconecta). El emulador de los tests de reglas también usa el puerto 8080: detener el backend
  local mientras corren.
- **Google Cloud CLI** (`gcloud`, instalado el 22-11-2026 en el computador de Shir1n, en
  `%LOCALAPPDATA%\Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd`; sesión iniciada con la cuenta dueña del proyecto).
  Publicar el backend, desde `backend/`:
  `gcloud run deploy lince-backend --source . --region southamerica-west1` (conserva variables y secretos). Logs:
  `gcloud run services logs read lince-backend --region southamerica-west1`.

## Identificadores
- `applicationId = "cl.duoc.lince_track"` (registrado en el proyecto Firebase `lince-track-66c55`).
- `namespace` y paquetes del código siguen en `com.example.lince_track` **a propósito**
  (es interno; no renombrar sin coordinar con todo el equipo).
- `app/google-services.json` está versionado y trae ambas apps (la vieja `com.example` se puede borrar en Firebase).

## Estructura (`app/src/main/java/com/example/lince_track/`)
- `data/` — `AuthRepository` (login + `PerfilUsuario` desde Firestore), `Rol`, `Servicio` (modelo de UI),
  `ServicioRepository` (fuente única de servicios para las pantallas), `RespuestaRepository` y
  `RespuestaServicio` (aceptar/rechazar en Firestore), `CheckinRepository`, `AsignacionRepository`,
  `IncidenciaRepository`, `BloqueoRepository` y `HitoRepository` (backend propio), `TokenFcmRepository` (token de
  notificaciones). Reglas sin Android ni Firebase, con tests: `ReglasCompanero` (la otra persona del tour),
  `ReglasAsignacion` (qué ve cada uno y reemplazos), `ReglasIncidencia`, `ReglasCheckin`, `ReglasGeocerca` (check-in con
  geocerca), `CambiosServicios` (qué notificar).
  - `data/remote/` — `LinceApi` (Retrofit), `LinceApiClient` (header `X-API-Key`, errores → `LinceApiException`),
    `dto/LinceDtos.kt` (JSON de la API + `LinceJson`). `BloqueoApi` y `BloqueoApiClient` son el cliente del backend
    propio (bloqueos, parámetros, geocercas e hitos), con el token de Firebase; `dto/HitoDtos.kt`.
  - `data/servicios/` — `ServiciosDataSource` (+ `ApiServiciosDataSource`), `FakeServiciosDataSource`,
    `ServicioMapper` (DTO → `Servicio`), `FiltroServicios`.
  - `data/clima/` — `ClimaService` (Open-Meteo).
- `ui/` — `navigation/AppNavHost` (rutas) y `PestanasPrincipales` (pestañas deslizables de los tres roles;
  `PestanasConductor` solo delega en ella), `login`, `servicios` (conductor), `guia` (guía),
  `detalle`, `calendario` (+ `agenda/AgendaViewModel`), `ruta` (mapa, check-in, clima), `alertas`,
  `historial` (servicios con check-out; solo alcanza a los últimos 7 días cargados), `perfil`, `ajustes`,
  `components` (incluye `Incidencias`), `theme`.
- `util/` — `LinceLogger` (consola de logs en Ajustes), `WhatsApp.kt` (`abrirWhatsApp`), `NotificadorLocal`,
  `LinceMessagingService` (Firebase Cloud Messaging) y `AperturaNotificacion` (abre el servicio de una notificación).
- `functions/` (raíz del repo) — Cloud Functions: `index.js` (escucha Firestore y envía) y `reglas.js` (qué avisar y a
  quién, con tests en `functions/test/`).

## Fuente de datos: API de Southbound (solo lectura)
- Docs: https://ws.southbound.travel/lince-api/api-docs/ · Base: `https://ws.southbound.travel/lince-api/`
- Header `X-API-Key` con la clave del curso, compartida por todo el curso y vigente hasta el 31-12-2026. Está en
  `contexto/` (docx y PDF de las pautas). Va en `local.properties` como `LINCE_API_KEY=...` →
  `BuildConfig.LINCE_API_KEY`. **Nunca** escribir la clave en el código, commits, issues, capturas ni chats.
- Si la clave está vacía, `ServicioRepository` usa `FakeServiciosDataSource` (misma forma que la API).
- Datos ficticios de tres oficinas (SCL, PUX, PUQ), del 01-09-2026 al 15-12-2026. PUX tiene unos 500 servicios entre
  hoy-7 y hoy+30.
- Endpoints: `/health` (sin clave), `/api/v1/servicios` (filtros `fechaDesde`, `fechaHasta`, `oficina`, `tipo`,
  `estadoServicio`, `estadoReserva`, `statusPL` (incluye `SIN_ASIGNAR`), `clienteId`, `guiaId`, `transportistaId`, `q`,
  `orden`; paginado, `pageSize` máx. 100), `/servicios/{id}`, `/guias` y `/guias/{id}`, `/transportistas` y
  `/transportistas/{id}`, `/clientes` y `/clientes/{id}`, `/catalogos`, `/resumen`. Parámetros desconocidos → 400.
  Errores: `{ error: { code, message, details, requestId } }` con 400, 401 (`UNAUTHORIZED`, `KEY_EXPIRED`), 404, 429
  (`Retry-After`) y 503. El límite de solicitudes es por minuto y compartido por red.
- Reglas de negocio: `TRANSFER` y `EXCURSION` llevan **solo transportista**; `GUIDED_SERVICE` **solo guía**.
  Un tour con traslado propio son **dos líneas de la misma reserva a la misma hora** (confirmado en la guía del curso):
  un `EX` "`<tour>`, sin guía" con el transportista y un `GS` "Servicio guiado…" con el guía. `ReglasCompanero` busca
  el otro servicio con la misma referencia, el mismo día y horario cruzado. Mientras esa persona no acepta se muestra
  "Sin asignar"; al aceptar, el nombre de su cuenta (`usuarios/{uid}.nombre`), no el de la API. En `DL` y `CX` el staff
  informado es el último solicitado; si la reserva está `XX`, todos sus servicios también.
- `statusPL` → estado de la app (`ServicioMapper.estadoDesde`): CF→ACEPTADO, DL/CX o servicio XX→RECHAZADO,
  RQ/null→PENDIENTE.
- La API **no trae**: hora de término (se estima por tipo: 60/180/240 min), tarifa (queda 0),
  coordenadas (pickup/dropoff son texto), lista de pasajeros ni teléfono del líder.
- Carga actual: oficina PUX, de hoy-7 a hoy+30, filtrada según `usuarios/{uid}`: conductor por
  `transportistaId`, guía por `guiaId` (ambos numéricos), administrador sin filtro. Sin ID no se carga nada. Conductor y
  guía también cargan toda la oficina (`serviciosOficina`), para encontrar a la otra persona del tour y lo que se les
  reasignó.

## Firebase
- Auth email/contraseña; cuentas creadas solo por el admin. Rol en `usuarios/{uid}.rol`
  (`administrador` | `conductor` | `guia`).
- `firestore.rules` (publicadas): cada usuario lee solo su `usuarios/{uid}`, admin lee todos,
  nadie escribe roles, **todo lo demás denegado**. Nunca volver a usar comodines `{document=**}`.
- `respuestas/{uid}_{servicioId}`: aceptar/rechazar de conductores y guías (`RespuestaRepository`).
  Aceptado → rechazado se permite; un rechazo es definitivo. Si la API marca el servicio como
  rechazado o cancelado (DL, CX, XX), eso manda sobre la respuesta. Las reglas no pueden comprobar que
  el servicio sea de esa persona (vive en la API); lo revisa la app.
- `checkins/{uid}_{servicioId}`: check-in y check-out con hora del servidor, coordenadas, distancia al
  punto y si el GPS era real (`CheckinRepository`). Se escribe solo después de que el backend verificó el
  check-in (ver "Check-in con geocerca"). Solo lo leen el dueño y el administrador.
- `estadosServicio/{servicioId}`: lo que el guía y el conductor de un mismo tour ven del otro (estado, nombre y teléfono, sin motivo
  ni observaciones). Se escribe junto con `respuestas` en un solo lote y lo leen todos los usuarios con rol. Solo
  quien lo publicó puede cambiarlo; el administrador lo borra al reasignar.
- `asignaciones/{servicioId}`: reemplazo elegido por el administrador (`ReglasAsignacion`), porque la API es de solo
  lectura. Manda sobre la persona que trae la API, y al crearla se borra el estado publicado. Solo escribe el
  administrador; las cuentas que aparecen como opción necesitan rol, nombre e ID de la API en `usuarios/{uid}`.
- `incidencias/{id}`: problemas reportados por conductores y guías en servicios aceptados y sin check-out
  (`IncidenciaRepository`). Guardan `lat` y `lon` si se pudo obtener la posición real en 5 segundos: sin permiso o sin
  señal se envían igual, y nunca con la posición simulada. Las lee quien reportó y el administrador. Solo el administrador responde (queda "En
  gestión"), las resuelve y, con una abierta, puede enviar reemplazo aunque el servicio esté aceptado o en curso. Los
  botones de llamar y WhatsApp usan `usuarios/{uid}.telefono` (opcional).
- **Notificaciones con la app cerrada** (desde el 21-11-2026): al iniciar sesión, `TokenFcmRepository` guarda el token
  del teléfono en `tokensFcm/{uid}` (`{ token, actualizadoEn }`, un teléfono por cuenta: el último). Al cerrar sesión se
  anula el token en Firebase (ya no hay permiso para borrar el documento) y la función borra el documento al ver que no
  sirve. Las reglas dejan escribir solo al dueño y nadie lo lee desde la app. Cuatro Cloud Functions en `functions/`:
  `avisarAsignacion` (reemplazo → la persona nueva), `avisarIncidenciaNueva` (→ administradores),
  `avisarIncidenciaAtendida` (respuesta o resolución → quien reportó) y `avisarRechazo` (→ administradores). Usan el
  canal "servicios" y la etiqueta `servicio|<id>`, la misma de `NotificadorLocal`, así que si llegan las dos una
  reemplaza a la otra. Al tocar el aviso la app abre el detalle de ese servicio (`MainActivity` es `singleTask`).
  Logs: `firebase functions:log`. Probado el 21-11-2026 con el celular bloqueado (reemplazo de prueba en el 3139,
  ya borrado).
- Notificaciones locales (`NotificadorLocal`, `CambiosServicios`): con la app viva. Los primeros 10 s tras cargar no avisan: Firestore entrega primero la caché del teléfono y
  después el servidor, y esa diferencia repetía avisos antiguos cada vez que se abría la app.
- Tests de reglas en `firestore-tests/` con el emulador y un proyecto demo (`npm install`, `npm test`;
  necesita Java en el PATH). Tras cambiar `firestore.rules`, hay que publicarlas en Firebase.
- **Foto de perfil (desde el 23-11-2026):** Firebase Storage, bucket `lince-track-66c55.firebasestorage.app`
  (southamerica-west1), archivo `perfil/{uid}/foto.jpg` (`FotoPerfilRepository`). `AvatarPerfil` se usa en los tres
  perfiles: cámara (foto completa vía `FileProvider`, enderezada por EXIF) o galería → `EditorFotoPerfil` (círculo,
  arrastrar y pellizcar, se adapta a horizontal) → se guarda solo el cuadrado del círculo, a 512 px en JPEG. Sin foto,
  ícono de persona (`perfil1.png` se borró). `storage.rules` publicadas (firebase.json las incluye): la ve cualquier
  cuenta con rol y solo el dueño la sube o borra; leen el rol desde Firestore, para eso la cuenta de Storage tiene el
  rol `firebaserules.firestoreServiceAgent`. `VERSION_AVISO` = 4 (menciona la foto y la posición cada 5 minutos). El proyecto está en
  Blaze: revisar que exista una alerta de presupuesto de $5 en Google Cloud.
- Plan detallado de datos: `Lince_Track_plan_base_de_datos.txt` (v2). Desactualizado en parte:
  con la API ya no se crean `servicios` en Firestore, y `asignaciones` ahora guarda solo los reemplazos. Firestore solo
  guarda lo que la app escribe.

## Backend propio (`backend/`, Spring Boot)
- El docente aceptó Firebase, pero igual se necesitan microservicios. `backend/` es un proyecto Gradle aparte
  (Spring Boot 4.1, Java 21) con geocercas, hitos (check-in, check-out y posición validados en el servidor), bloqueos
  de disponibilidad y parámetros que coordinación cambia sin reiniciar (`/api/parametros`: anticipación, precisión
  máxima, radio por defecto y días para bloquear). `GET /api/hitos/resumen` da la puntualidad por persona para el
  panel de coordinación. Detalle y comandos en `backend/README.md`.
- No tiene login propio: valida el token de Firebase Auth. Rol e IDs de la API vienen de custom claims que copia
  `backend/scripts/sincronizar-claims.mjs` desde `usuarios/{uid}` (necesita una cuenta de servicio guardada fuera del
  repo).
- Local usa H2 en `backend/data/`; Docker usa PostgreSQL. La clave de la API va en `LINCE_API_KEY` (variable de entorno o
  `backend/.env`, ignorado).
- Lee los reemplazos (`asignaciones/{servicioId}`) con Firebase Admin si `GOOGLE_APPLICATION_CREDENTIALS` apunta a la
  cuenta de servicio (la de Shir1n está fuera del repo: `D:\IA\Fire` o `contexto/`, según el computador); sin ella
  arranca igual y no los ve.
- La app lo usa para bloqueos (PR 51), el check-in con geocerca (PR 53) y la cola sin conexión (PR 58).
- **En la nube (desde el 22-11-2026):** Cloud Run `lince-backend` en `southamerica-west1`
  (https://lince-backend-589632568586.southamerica-west1.run.app, Swagger en `/swagger-ui.html`), máximo 2 instancias,
  perfil `nube`. Base: Cloud SQL `lince-db` (PostgreSQL 17, `db-f1-micro`, disco HDD de 10 GB, sin respaldos), base y
  usuario `lince`; se conecta con el conector de Cloud SQL (`CLOUD_SQL_INSTANCIA`). La clave de la API y la contraseña
  de la base están en Secret Manager (`lince-api-key`, `lince-db-password`). Lee Firestore con la identidad de Cloud
  Run (`LINCE_FIREBASE_CREDENCIALES=nube`), sin archivo de clave. Costo aproximado: Cloud SQL unos US$9-10 al mes;
  se puede borrar después de la entrega. El `Dockerfile` quita los CRLF de `gradlew` (si no, Linux no lo ejecuta).
- Al partir con la base vacía, el backend crea las tablas y las 4 geocercas; los parámetros parten con los valores
  de `application.yml`. Para mover una geocerca en la nube se usa `PUT /api/geocercas/{id}` con token de administrador.
- Para probar sin token de administrador, la base H2 local se puede editar con el `h2-*.jar` de la caché de Gradle
  (`org.h2.tools.Shell`, usuario `sa` sin contraseña; `AUTO_SERVER=TRUE` permite hacerlo con el backend corriendo).

### Check-in con geocerca (PR 53)
- `RutaMapaScreen` carga del backend las geocercas (`GET /api/geocercas`) y las reglas (`/api/parametros`). El pickup
  es la geocerca cuyo nombre coincide con `servicio.origen` (los 4 lugares de PUX calzan con la API real); el destino,
  la de `servicio.destino`, y si no hay, se deduce del texto como antes.
- `ReglasGeocerca.evaluar` repite en la app las reglas del backend, en el mismo orden: sin geocerca → sin GPS real
  (posición simulada) → GPS impreciso (sobre 50 m) → fuera de zona (sobre el radio, 150 m) → tarde (después del pickup
  − 15 min). Si no cumple, el botón dice "CHECK-IN BLOQUEADO", se muestra el motivo y "Avisar a Coordinación" crea la
  incidencia (Atraso si llegó tarde; si no, Otro).
- Al marcar, `HitoRepository.registrar` envía el hito con una `claveIdempotencia` (UUID que solo cambia cuando el
  backend respondió). Si responde `VERIFICADO` se guarda en Firestore como antes; si no, se muestra el motivo y la
  incidencia se crea sola. `HITO_DUPLICADO` cuenta como verificado. El check-out se registra, pero no se bloquea.
- La pantalla muestra distancia, radio, precisión del GPS y hora límite, nunca coordenadas (quedan en capturas y videos).
- Modificación en vivo típica: el radio de cada hotel (`PUT /api/geocercas/{id}`) o `radioPorDefectoMetros` y
  `anticipacionMinutos` (`PUT /api/parametros`).

### Check-in sin conexión (PR 58, Room + WorkManager)
- Room (`data/local/LinceDatabase.kt`, con KSP): `hitos_pendientes` (cola) y la caché de `geocercas` y `parametros`.
  `HitoRepository` guarda la caché al leer del backend y, si no hay conexión (`esSinConexion`: sin red, 5xx o 429), usa
  la copia guardada. Un 4xx no se esconde con la caché.
- Si la app permite el check-in y el backend no responde, se guarda en la cola con su clave y el panel muestra
  "Pendiente de sincronizar". El check-out también va a la cola si su check-in sigue pendiente.
- `SincronizarHitosWorker` (WorkManager, solo con red, reintentos exponenciales desde 30 s) usa `SincronizadorHitos`:
  envía en orden y con la misma clave; VERIFICADO → Firestore; rechazado → incidencia y descarta su check-out; 4xx →
  lo saca de la cola. Al terminar avisa con una notificación. `MainActivity` lo reprograma si quedó algo en la cola.
- Probado el 22-11-2026 cortando el `adb reverse`: el check-in del 3166 (Patricio) se sincronizó solo en unos 20 s.
  Ese servicio quedó aceptado y con check-in de prueba. Con el backend en Cloud Run, la demo se hace con modo avión.

### Última posición (PR 68)
- `PosicionRepository` envía un hito `POSICION` cada 5 minutos (`ReglasPosicion`) solo con el servicio en curso
  (check-in confirmado y sin check-out), el mapa abierto y GPS real. Sin señal se omite (no va a la cola). No hay rastreo
  24/7.
- Coordinación la ve en el detalle del servicio (`UltimaPosicionCard`, con "Ver en el mapa"):
  `GET /api/hitos/ultima-posicion` (solo administrador, 404 `SIN_POSICION` si nadie informó).
- Agencias: `Servicio.agencia` sale de `dto.cliente`; se ve en el detalle y en Panel → Equipo → Agencias (`/clientes`).

## Privacidad (Ley 21.719, vigente desde el 01-12-2026)
- Aviso "Tus datos en Lince Track" (`ui/components/Privacidad.kt`): se muestra al abrir la app hasta pulsar
  "Entendido" y se relee en Ajustes. Si cambia lo que dice (qué datos, para qué, quién los ve), subir
  `PrivacidadRepository.VERSION_AVISO` para que se vuelva a mostrar.
- La ubicación se pide con `rememberPermisoUbicacion`, que explica el motivo antes del permiso de Android.
- El mapa lee el GPS cada 2-3 s solo mientras está abierto (velocímetro y distancia) y eso **no se envía**; solo se
  guardan las coordenadas de check-in, check-out e incidencias. **Nunca** poner coordenadas en `LinceLogger` (la
  consola se puede compartir). A Open-Meteo se envía la zona con 2 decimales, no la posición exacta.
- **Derechos (desde el 22-11-2026):** en Ajustes → Privacidad, conductores y guías descargan sus datos (JSON con lo
  suyo de Firestore y del backend, `GET /api/privacidad/mis-datos`) y piden la eliminación (`solicitudesPrivacidad/{uid}`,
  cancelable mientras esté pendiente). Coordinación la ve en el **Panel**, sobre las incidencias, y la aprueba o rechaza
  (con motivo). Al aprobar, la app borra hitos y bloqueos (`DELETE /api/privacidad/{uid}`, solo administrador) y la
  Cloud Function `atenderSolicitudPrivacidad` borra lo de Firestore, Storage y la cuenta de Auth, y deja un registro
  "completada" solo con fechas y cantidades. Probado el 22-11-2026 con una cuenta desechable. `VERSION_AVISO` = 2.
- Firebase Auth permite registrarse con la clave web pública: el 21-11-2026 apareció una cuenta sin rol (borrada).
  Conviene desactivar "Habilitar creación (registro)" en Authentication → Configuración.
- Sin Firebase Analytics (se quitó el 20-11-2026: no se usaba). Pendiente antes de datos reales: contraseñas de
  prueba fuera del repo, teléfono visible solo para el compañero del tour, plazos de borrado, `allowBackup`.

## Convenciones del equipo
- Una rama por tarea (`feature/...`, `fix/...`) y PR chico a `master`. Nada directo a `master`.
- **Claude llega hasta el push:** los PR y el merge los hace Shir1n. No crear PR ni preparar enlaces para abrirlos.
- **Commits:** mensajes lo más simples posible, sin línea `Co-Authored-By`. No hacer commit hasta que Shir1n termine las
  pruebas en celulares, salvo que lo pida.
- Tras cambiar `firestore.rules`, avisar a Shir1n que las publique **antes** de instalar: sin eso la app falla al escribir
  y dejan de salir las notificaciones de esa cuenta.
- Al tocar archivos compartidos, mencionarlo al equipo sin esperar respuesta: `AppNavHost.kt`, `LinceApi.kt`,
  `LinceDtos.kt`, `ServicioRepository.kt`, `AuthRepository.kt`, `firestore.rules`.
- No subir `local.properties` (está en `.gitignore`).
- Los archivos Kotlin usan saltos de línea CRLF.
- Textos de la UI en español; los códigos de la API se guardan como String (tolerar valores nuevos).

## Pendientes
1. Rehacer el recorrido del video con servicios reales (IDs de PUX ya aplicados el 16-11-2026).
2. El panel del administrador (`ui/admin/AdminPanelScreen`) muestra el día elegido (hoy-7 a hoy+30) y, siempre, las
   incidencias abiertas y los rechazos de los próximos días; avisa y reasigna. Lo ya aceptado solo se reasigna con una
   incidencia abierta, y los días pasados no se reasignan. Con datos de ejemplo, los días pasados se ven casi vacíos: el
   historial de demostración solo está en el Resumen.
3. Benjamin: `DarkErrorContainer` gris. El botón de la otra persona en el detalle abre su WhatsApp (teléfono compartido
   al aceptar). Los del líder del grupo quedan visibles a propósito aunque no haya teléfono: la API no trae el dato. `ConexionConductorScreen` sigue en construcción y fuera de
   la barra: el guía ve Alertas en su lugar. Las alertas se calculan al abrir la pestaña (`GeneradorAlertas`).
4. Mapa: la ruta es una línea recta entre las geocercas de pickup y destino (sin trazado por calles). El servicio 3067
   de Carlos quedó con check-in y check-out de prueba (21-11-2026): para reusarlo, borrar `checkins/{uid}_3067` en
   Firebase y sus hitos en la base del backend. Las incidencias aún no llevan fotos (requieren Storage).
5. Luciano: sumar datos de `GET /api/v1/resumen` al panel del administrador (los totales de hoy se calculan
   desde los servicios cargados, porque los datos de ejemplo no traen ese endpoint).
6. Ignacio: preguntas a Southbound, clave del docente, Trello, README (estructura y roles).
7. Número de soporte `+56987654321` es provisional (`SoporteDialog.kt`):
   reemplazar por el real o deshabilitar.

## Problemas conocidos (revisión del 02-11-2026)
- **Credenciales de prueba públicas:** el repositorio es público, y `USUARIOS_TEST.md` y los botones de `LoginScreen` traen
  correos y contraseña de cuentas reales de Firebase, incluida la de administrador. Se mantienen a propósito para trabajar
  más rápido. Antes de usar datos reales o publicar la app: quitar ambos y cambiar las contraseñas (quedan en el historial).
- `PerfilScreen` tiene datos fijos ("Carlos Morales", fechas de 2026; la foto ya es la real de la cuenta): se mantiene a propósito
  como placeholder. Conductores con `usuarios/{uid}.perfilNuevo: true` (booleano) y guías usan `PerfilRealScreen`,
  que muestra próximo servicio, puntualidad, terminados y por realizar reales. Son fijos a propósito, para que se vea
  completo en el video: "Verificado", la certificación "Nivel Oro" (solo conductores), el interruptor de notificaciones
  y, para "Piloto1" y "Patricio Lagos", el "JAC S5", 2015, 5 pasajeros, 86.400 km, revisión al día y calificación 4,9
  (los guías, 4,8).
  "Viajes Mes" cuenta también lo futuro y lo rechazado. Quitar o reemplazar todo esto antes de usar datos reales.
  Cuando llegue la clave de la API, decidir si reemplaza al placeholder.
- `AppNavHost` conserva las rutas antiguas de las pestañas de guía y administrador, que ya no se usan desde el PR 33
  (ahora se abren en `PestanasPrincipales`): se pueden limpiar.
- Textos fijos fuera de `strings.xml`; recursos sin usar (`logo_solo1.png`, `logo.jpg`, `van.png`).
- R8 desactivado en release; Coil declarado fuera del catálogo de versiones.
- Calendario fuera del rango cargado (hoy-7..hoy+30) aparece vacío.

## Preguntas abiertas a Southbound
1. ¿Los datos cambian en el tiempo (statusPL RQ→CF)? Cubren las fechas actuales: llegan hasta el 15-12-2026.
2. ¿Pueden agregar hora de término, tarifa o coordenadas? (La pauta dice que mantengamos un catálogo ficticio de
   hoteles con coordenadas.)

Ya respondidas: la API no tendrá endpoints para escribir (lo nuevo va al backend propio), un tour se relaciona por la
misma reserva y hora (EX + GS), la app puede llamar a la API directo con la clave en configuración local, y el docente
acepta Firebase siempre que haya microservicios propios.
