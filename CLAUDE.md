# Lince Track — contexto para Claude

App Android para coordinar servicios turísticos de Southbound (Puerto Varas) entre
conductores (transportistas), guías y administración. Proyecto académico de equipo.
Responde y escribe la interfaz en **español de Chile**.

## Equipo
| Persona | Rol | Foco actual |
|---|---|---|
| Shir1n (dueño del repo) | Coordina | Firebase completo (reglas, Auth, datos) y vincular usuario ↔ API |
| Benjamin Agüero | Frontend | Detalle, calendario, colores, alertas |
| Raúl Ferrini | Frontend | Pantalla de mapa (`ui/ruta/`) |
| Luciano Garrido | Backend → frontend | Panel del administrador (`ui/admin/`, nuevo). **No tiene acceso a Firebase** |
| Ignacio Salazar | Scrum Master | Trello, README, comunicación con Southbound |

Estos focos son sugerencias: cada quien trabaja en todo, así que no hace falta frenar ni coordinar antes de tocar el
área de otra persona.

## Evaluación del curso (DSY1105, revisado el 13-11-2026)
El material del docente está en `contexto/` (ignorada en Git: trae la API key y un video de 250 MB; **no subirla**):
pautas `EA4_...Encargo` y `EA5_...Defensa`, caso `Formato_Caso_DSY1105`, guía `ACCESO_ESTUDIANTES.md`, colección de
Postman y la transcripción de la reunión con Southbound.
- **EA4 (6 %, en parejas):** entrega en AVA en la semana 15 (código Android y backend, APK release firmada, README,
  evidencia de pruebas y enlace a GitHub público). Cambios en el repositorio después del plazo = nota 1,0.
- **EA5 (24 %, individual, 15 min):** defensa en la semana 16, sin IA, con la APK en un celular real. El 40 % es una
  **modificación en vivo** que recorra Compose → ViewModel → endpoint → base de datos (ej.: radio de 150 a 100 m,
  tolerancia de puntualidad, motivo obligatorio de incidencia, campo "precisión del GPS").
- **Lo que piden y falta hoy:**
  - Backend propio **Spring Boot** + Swagger/OpenAPI + JWT/roles + BD (check-ins, geocercas, disponibilidad,
    incidencias, confirmaciones). Hoy eso vive en Firebase: la pauta acepta "alternativa autorizada", así que hay que
    preguntar al docente si Firebase sirve.
  - Check-in **bloqueado** fuera de la geocerca (radio inicial 150 m, configurable por coordinación) o si es más tarde
    que 15 min antes del pickup; mostrar distancia, radio y precisión del GPS, y generar incidencia si falla.
    Validación doble: la app y el backend. Catálogo ficticio de hoteles con coordenadas.
  - Disponibilidad: bloquear días con 7 días de anticipación y nunca sobre un servicio asignado.
  - Room + WorkManager: caché, cola de check-ins sin conexión, sincronización con reintentos e idempotencia.
  - Español e inglés con selector de idioma persistente.
  - JUnit + MockK con cobertura > 90 % en ViewModel/Repository y al menos un Compose UI Test.
  - APK release firmada con `signingConfigs` seguros; `.jks`, contraseñas y API key fuera del repositorio.
  - Usar también `/guias`, `/transportistas`, `/clientes`, `/catalogos` y `/resumen`; mostrar `requestId` en errores.
  - Coordinación: última posición puntual del transportista durante un servicio activo (sin rastreo 24/7).
- **Pedidos de Southbound en la reunión (11-09-2026) que aún no están:** datos de la agencia (vehículo, polera, letrero),
  qué incluye la excursión, reporte mensual con servicios y dinero acumulado, y que los servicios cancelados igual se
  paguen al transportista. Un guía puede hacer un tour de medio día y un transfer el mismo día, pero no otro servicio
  si tiene un tour de día completo.

## Estado actual (13-11-2026)
- **Objetivo cercano:** un video para vender la app a los dueños de Southbound, con el flujo lo más completo posible.
- **Clave de la API disponible** (vence el 31-12-2026). Los datos de la API llegan hasta el 15-12-2026. Con la clave
  en `local.properties`, master compila, pasan los 150 tests y los DTO calzan con el JSON real; falta probarla en
  celulares.
- **Rama en curso:** `fix/detalles-video` (revisión visual con los tres roles). Ya están en `master` el resumen (PR 30),
  la ubicación en la incidencia (PR 31), el panel con fecha (PR 32), el perfil (PR 34), las pestañas deslizables de los
  tres roles (PR 33) y el verificador de versión (PR 36).
- **Reglas:** revisar que la versión de `firestore.rules` de `master` (`telefono` en `estadosServicio`, `lat` y `lon` en
  `incidencias`) esté publicada antes de probar: si falta, aceptar, rechazar o reportar da error.
- **Cuentas de prueba** en `usuarios/{uid}`:
  - Carlos Morales: `conductor`, `transportistaId: 12`, sin `perfilNuevo` (se muestra el perfil completo de ejemplo).
  - Patricio Lagos: `conductor`, `transportistaId: 16`, `perfilNuevo: true`. Es el reemplazo en el video.
  - Guía: `guiaId: 7`, administrador sin ID. `telefono` es texto con código de país (`+569...`).
  - Los correos y la contraseña están en `USUARIOS_TEST.md`, y `LoginScreen` tiene botones que rellenan esas cuentas.
  - **Con la clave, esos IDs no sirven:** transportistas 12 y 16 y guía 7 son de Santiago (SCL) y la app carga solo PUX,
    así que verían la lista vacía. IDs de PUX con más servicios (hoy-7 a hoy+30, al 13-11): transportistas 40, 33, 43,
    37; guías 31, 23, 29, 35.
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
  3. Decidir la foto de `PerfilScreen` (`res/drawable/perfil1`): parece de una persona real.
- **Resumen para los dueños** (`ui/admin/AdminResumenScreen`): sin clave de la API, "Últimos 7 días" suma el historial
  inventado de `ResumenDemostracion`, siempre con un aviso visible; "Hoy" y las cifras con clave son solo datos reales.
  Los márgenes (15 minutos de puntualidad, 500 m de distancia) son provisionales: falta confirmarlos con Southbound.
- **Fuera por ahora:** fotos y ubicación en vivo (mejor con Blaze) y chat (ya está WhatsApp).
- **No mostrar en el video:** botones del líder del grupo (sin teléfono), notificaciones con la app cerrada (no llegan) y
  botones de cuentas de prueba en el login.
- **Limitaciones conocidas:**
  - Historial solo alcanza a los últimos 7 días cargados.
  - Quien es reemplazado deja su check-in sin cerrar, ya no puede abrir el servicio, y si se le devuelve aparece aceptado
    con ese check-in.
  - `estadosServicio` lo leen todas las cuentas con rol, incluido el teléfono: revisarlo antes de usar datos reales.
- **Nombres de la API:** el panel muestra el de la fuente mientras nadie responda; las cuentas muestran su `nombre`.

## Stack
- Kotlin 2.2.10, Jetpack Compose + Material 3, Navigation Compose 2.10.2
- AGP 9.3.3, Gradle 9.5, compileSdk/targetSdk 37, minSdk 27, Java 11
- Firebase BoM 34.19.0: Auth + Firestore (+ Analytics). Plan **Spark**.
- Retrofit 3.0.0 + kotlinx.serialization 1.9.0 (API de Southbound)
- osmdroid (mapa OSM), Play Services Location, Coil, DataStore (tema)

## Comandos (Windows, PowerShell)
```powershell
.\gradlew.bat assembleDebug          # compilar
.\gradlew.bat testDebugUnitTest      # tests unitarios (incluye LinceApiTest)
.\gradlew.bat installDebug           # instalar en dispositivo/emulador
```
- Se prueba en celulares reales por USB: no crear emuladores de Android. `adb` está en `Android\Sdk\platform-tools`, y si
  Java no está en el PATH se puede usar el `jbr` de Android Studio (`C:\Program Files\Android\Android Studio\jbr` o
  `D:\Android\AndroidStudio\jbr`, según el computador).
- Desde el PR 36 todos firman con `app/debug.keystore` (versionada, solo para debug: el repositorio es público). Un
  celular con una versión firmada antes con otra clave rechaza instalar (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) y hay que
  desinstalar una vez. Se pierde la sesión y lo no enviado: confirmarlo antes con quien usa ese celular.
- La versión sale de `version.json` (raíz). La app compara con el de `master` en GitHub al abrir; si el APK de
  `releases/latest` no existe, el aviso de versión desactualizada no ofrece descargar.
- `gh` puede no estar instalado.

## Identificadores
- `applicationId = "cl.duoc.lince_track"` (registrado en el proyecto Firebase `lince-track-66c55`).
- `namespace` y paquetes del código siguen en `com.example.lince_track` **a propósito**
  (es interno; no renombrar sin coordinar con todo el equipo).
- `app/google-services.json` está versionado y trae ambas apps (la vieja `com.example` se puede borrar en Firebase).

## Estructura (`app/src/main/java/com/example/lince_track/`)
- `data/` — `AuthRepository` (login + `PerfilUsuario` desde Firestore), `Rol`, `Servicio` (modelo de UI),
  `ServicioRepository` (fuente única de servicios para las pantallas), `RespuestaRepository` y
  `RespuestaServicio` (aceptar/rechazar en Firestore), `CheckinRepository`, `AsignacionRepository`,
  `IncidenciaRepository`. Reglas sin Android ni Firebase, con tests: `ReglasCompanero` (la otra persona del tour),
  `ReglasAsignacion` (qué ve cada uno y reemplazos), `ReglasIncidencia`, `ReglasCheckin`, `CambiosServicios` (qué notificar).
  - `data/remote/` — `LinceApi` (Retrofit), `LinceApiClient` (header `X-API-Key`, errores → `LinceApiException`),
    `dto/LinceDtos.kt` (JSON de la API + `LinceJson`).
  - `data/servicios/` — `ServiciosDataSource` (+ `ApiServiciosDataSource`), `FakeServiciosDataSource`,
    `ServicioMapper` (DTO → `Servicio`), `FiltroServicios`.
  - `data/clima/` — `ClimaService` (Open-Meteo).
- `ui/` — `navigation/AppNavHost` (rutas) y `PestanasPrincipales` (pestañas deslizables de los tres roles;
  `PestanasConductor` solo delega en ella), `login`, `servicios` (conductor), `guia` (guía),
  `detalle`, `calendario` (+ `agenda/AgendaViewModel`), `ruta` (mapa, check-in, clima), `alertas`,
  `historial` (servicios con check-out; solo alcanza a los últimos 7 días cargados), `perfil`, `ajustes`,
  `components` (incluye `Incidencias`), `theme`.
- `util/` — `LinceLogger` (consola de logs en Ajustes), `WhatsApp.kt` (`abrirWhatsApp`), `NotificadorLocal`.

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
  punto y si el GPS era real (`CheckinRepository`). No se bloquea por distancia ni por hora: se registra
  para que Coordinación lo vea. Solo lo leen el dueño y el administrador.
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
- Notificaciones locales (`NotificadorLocal`, `CambiosServicios`): solo con la app viva. Con la app cerrada requieren
  Blaze y un servidor. Los primeros 10 s tras cargar no avisan: Firestore entrega primero la caché del teléfono y
  después el servidor, y esa diferencia repetía avisos antiguos cada vez que se abría la app.
- Tests de reglas en `firestore-tests/` con el emulador y un proyecto demo (`npm install`, `npm test`;
  necesita Java en el PATH). Tras cambiar `firestore.rules`, hay que publicarlas en Firebase.
- `storage.rules` está en el repo pero **sin uso**: Storage exige plan Blaze. Se activará Blaze
  (crédito de prueba de 90 días) recién cuando se implementen fotos; elegir región con cuota gratuita
  y crear presupuesto de $5 ese mismo día.
- Plan detallado de datos: `Lince_Track_plan_base_de_datos.txt` (v2). Desactualizado en parte:
  con la API ya no se crean `servicios` en Firestore, y `asignaciones` ahora guarda solo los reemplazos. Firestore solo
  guarda lo que la app escribe.

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
1. Ya llegó la clave: cambiar en Firebase los IDs de `usuarios/{uid}` (`transportistaId: 12` y `16`, `guiaId: 7`, de
   Santiago) por IDs de PUX (ver "Cuentas de prueba") y rehacer el recorrido del video con servicios reales.
2. El panel del administrador (`ui/admin/AdminPanelScreen`) muestra el día elegido (hoy-7 a hoy+30) y, siempre, las
   incidencias abiertas y los rechazos de los próximos días; avisa y reasigna. Lo ya aceptado solo se reasigna con una
   incidencia abierta, y los días pasados no se reasignan. Con datos de ejemplo, los días pasados se ven casi vacíos: el
   historial de demostración solo está en el Resumen.
3. Benjamin: `DarkErrorContainer` gris. El botón de la otra persona en el detalle abre su WhatsApp (teléfono compartido
   al aceptar). Los del líder del grupo quedan visibles a propósito aunque no haya teléfono: la API no trae el dato. `ConexionConductorScreen` sigue en construcción y fuera de
   la barra: el guía ve Alertas en su lugar. Las alertas se calculan al abrir la pestaña (`GeneradorAlertas`).
4. Mapa: la ruta es una línea recta desde el Hotel Cumbres y el destino se deduce del texto (faltan
   coordenadas en la API). Falta validar distancia y hora del check-in. Las incidencias aún no llevan fotos:
   requieren Storage, y por lo tanto Blaze.
5. Luciano: sumar datos de `GET /api/v1/resumen` al panel del administrador (los totales de hoy se calculan
   desde los servicios cargados, porque los datos de ejemplo no traen ese endpoint).
6. Ignacio: preguntas a Southbound, clave del docente, Trello, README (estructura y roles).
7. Número de soporte `+56987654321` es provisional (`SoporteDialog.kt`):
   reemplazar por el real o deshabilitar.

## Problemas conocidos (revisión del 02-11-2026)
- **Credenciales de prueba públicas:** el repositorio es público, y `USUARIOS_TEST.md` y los botones de `LoginScreen` traen
  correos y contraseña de cuentas reales de Firebase, incluida la de administrador. Se mantienen a propósito para trabajar
  más rápido. Antes de usar datos reales o publicar la app: quitar ambos y cambiar las contraseñas (quedan en el historial).
- `PerfilScreen` tiene datos fijos ("Carlos Morales", fechas de 2026) y su foto no se guarda: se mantiene a propósito
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
3. Para el docente: ¿se acepta Firebase como "alternativa autorizada" al backend Spring Boot?

Ya respondidas: la API no tendrá endpoints para escribir (lo nuevo va al backend propio), un tour se relaciona por la
misma reserva y hora (EX + GS), y la app puede llamar a la API directo con la clave en configuración local.
