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

## Estado actual (02-11-2026)
- **Objetivo cercano:** un video para vender la app a los dueños de Southbound, con el flujo lo más completo posible.
- **Rama en curso:** `feature/gestion-incidencias`, con la gestión de incidencias y el botón de WhatsApp. Compila y pasan
  los tests, pero **falta probarla en celulares** y hacer su PR.
- **Reglas:** la versión de `firestore.rules` de esa rama (`telefono` en `estadosServicio`) podría no estar publicada.
  Revisarlo antes de probar: si falta, aceptar o rechazar da error.
- **Cuentas de prueba** en `usuarios/{uid}`:
  - Carlos Morales: `conductor`, `transportistaId: 12`, sin `perfilNuevo` (se muestra el perfil completo de ejemplo).
  - Patricio Lagos: `conductor`, `transportistaId: 16`, `perfilNuevo: true`. Es el reemplazo en el video.
  - Guía: `guiaId: 7`, administrador sin ID. `telefono` es texto con código de país (`+569...`).
- **Recorrido del video:** guía y conductor aceptan "Saltos del Petrohué" (8941/8942) → el conductor hace check-in y
  reporta "Falla mecánica" → el administrador responde y envía reemplazo → Patricio acepta y hace check-in y check-out →
  el administrador la marca resuelta. Tour de mañana para otra prueba: "Lago Todos los Santos" (8960/8961).
- **Limpiar pruebas:** lo ya aceptado no se puede volver a aceptar. Para repetir hay que borrar en Firebase el documento
  en `estadosServicio` y los de `respuestas`; también `asignaciones` si se reasignó. Lo aceptado antes de agregar
  `telefono` se guardó sin teléfono.
- **Lo que sigue para el video, en orden:**
  1. Probar y subir esta rama.
  2. Guardar la ubicación en la incidencia.
  3. Resumen para los dueños: puntualidad, terminados, rechazos por motivo y distancia del check-in.
  4. Panel con selector de fecha y terminados.
  5. Perfil real más completo (revisar lo que ya agregó `feature/perfil-piloto1`).
  6. Revisión visual con los tres roles y número de soporte real.
- **Fuera por ahora:** fotos y ubicación en vivo (mejor con Blaze) y chat (ya está WhatsApp).
- **No mostrar en el video:** botones del líder del grupo (sin teléfono) y notificaciones con la app cerrada (no llegan).
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
  Java no está en el PATH se puede usar `C:\Program Files\Android\Android Studio\jbr`.
- Cada computador firma con su propia clave: si Android rechaza instalar (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`), hay que
  desinstalar primero. Se pierde la sesión y lo no enviado, y conviene confirmarlo antes con quien usa ese celular.
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
- `ui/` — `navigation/AppNavHost` (rutas por rol), `login`, `servicios` (conductor), `guia` (guía),
  `detalle`, `calendario` (+ `agenda/AgendaViewModel`), `ruta` (mapa, check-in, clima), `alertas`,
  `historial` (servicios con check-out; solo alcanza a los últimos 7 días cargados), `perfil`, `ajustes`,
  `components`, `theme`.
- `util/` — `LinceLogger` (consola de logs en Ajustes), `WhatsApp.kt` (`abrirWhatsApp`), `NotificadorLocal`.

## Fuente de datos: API de Southbound (solo lectura)
- Docs: https://ws.southbound.travel/lince-api/api-docs/ · Base: `https://ws.southbound.travel/lince-api/`
- Header `X-API-Key` con la clave del curso (vence al terminar el semestre).
  Va en `local.properties` como `LINCE_API_KEY=...` → `BuildConfig.LINCE_API_KEY`.
  **Nunca** escribir la clave en el código, commits, issues ni chats.
- Si la clave está vacía, `ServicioRepository` usa `FakeServiciosDataSource` (misma forma que la API).
- Endpoints: `/health` (sin clave), `/api/v1/servicios` (filtros fecha, oficina, tipo, estado, statusPL,
  guiaId, transportistaId; paginado, `pageSize` máx. 100), `/servicios/{id}`, `/guias`, `/transportistas`,
  `/clientes`, `/catalogos`, `/resumen`. Parámetros desconocidos → 400. Límite → 429 con `Retry-After`.
- Reglas de negocio: `TRANSFER` y `EXCURSION` llevan **solo transportista**; `GUIDED_SERVICE` **solo guía**.
  Un tour se relaciona por `reserva.referencia` (por confirmar con Southbound): `ReglasCompanero` busca el otro
  servicio con la misma referencia, el mismo día y horario cruzado. Mientras esa persona no acepta se muestra
  "Sin asignar"; al aceptar, el nombre de su cuenta (`usuarios/{uid}.nombre`), no el de la API.
- `statusPL` → estado de la app (`ServicioMapper.estadoDesde`): CF→ACEPTADO, DL/CX o servicio XX→RECHAZADO,
  RQ/null→PENDIENTE.
- La API **no trae**: hora de término (se estima por tipo: 60/180/240 min), tarifa (queda 0),
  coordenadas (pickup/dropoff son texto), lista de pasajeros ni teléfono del líder.
- Carga actual: oficina PUX, de hoy-7 a hoy+30, filtrada según `usuarios/{uid}`: conductor por
  `transportistaId`, guía por `guiaId` (ambos numéricos), administrador sin filtro. Sin ID no se carga nada.

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
  quien lo publicó puede cambiarlo.
- `asignaciones/{servicioId}`: reemplazo elegido por el administrador (`ReglasAsignacion`), porque la API es de solo
  lectura. Manda sobre la persona que trae la API, y al crearla se borra el estado publicado. Solo escribe el
  administrador; las cuentas que aparecen como opción necesitan rol, nombre e ID de la API en `usuarios/{uid}`.
- `incidencias/{id}`: problemas reportados por conductores y guías en servicios aceptados y sin check-out
  (`IncidenciaRepository`). Las lee quien reportó y el administrador. Solo el administrador responde (queda "En
  gestión"), las resuelve y, con una abierta, puede enviar reemplazo aunque el servicio esté aceptado o en curso. Los
  botones de llamar y WhatsApp usan `usuarios/{uid}.telefono` (opcional).
- Notificaciones locales (`NotificadorLocal`, `CambiosServicios`): solo con la app viva. Con la app cerrada requieren
  Blaze y un servidor.
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
- Avisar antes de tocar archivos compartidos: `AppNavHost.kt`, `LinceApi.kt`, `LinceDtos.kt`,
  `ServicioRepository.kt`, `AuthRepository.kt`, `firestore.rules`.
- No subir `local.properties` (está en `.gitignore`).
- Los archivos Kotlin usan saltos de línea CRLF.
- Textos de la UI en español; los códigos de la API se guardan como String (tolerar valores nuevos).

## Pendientes
1. Reemplazar los IDs de prueba de `usuarios/{uid}` (`transportistaId: 12`, `guiaId: 7`) por los reales
   cuando llegue la clave de la API: con la clave, un ID de prueba mostraría servicios de otra persona.
2. El panel del administrador (`ui/admin/AdminPanelScreen`) muestra hoy y los rechazos de los próximos días, avisa y
   reasigna. Lo ya aceptado solo se reasigna con una incidencia abierta.
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

## Problemas conocidos (revisión del 28-10-2026)
- `PerfilScreen` tiene datos fijos ("Carlos Morales", "v2.4.1") y su foto no se guarda: se mantiene a propósito
  como placeholder. Conductores con `usuarios/{uid}.perfilNuevo: true` (booleano) y guías usan `PerfilRealScreen`,
  que solo muestra datos reales. Cuando llegue la clave de la API, decidir si reemplaza al placeholder.
- Navegación: tras el login se saca `LOGIN` y la barra inferior hace `popUpTo(findStartDestination())`
  (que es LOGIN) → probablemente se acumulan pestañas en la pila.
- Insets: algunas pantallas suman `statusBarsPadding()` sobre el padding del `Scaffold` y otras no.
- Textos fijos fuera de `strings.xml`; recursos sin usar (`logo_solo1.png`, `logo.jpg`, `van.png`).
- R8 desactivado en release; Coil declarado fuera del catálogo de versiones.
- Calendario fuera del rango cargado (hoy-7..hoy+30) aparece vacío.

## Preguntas abiertas a Southbound
1. ¿Los datos cambian en el tiempo (statusPL RQ→CF) y cubren las fechas actuales?
2. ¿Habrá endpoints para escribir (aceptar/rechazar)?
3. ¿Cómo se relaciona un servicio guiado con su transporte?
4. ¿Pueden agregar hora de término, tarifa o coordenadas?
5. ¿Se puede llamar a la API directo desde la app o prefieren un intermediario?
