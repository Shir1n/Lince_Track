# Lince Track: backend

Microservicio Spring Boot que guarda lo que la API de Southbound (solo lectura) no puede guardar:

| Recurso | Para qué | Quién escribe |
|---|---|---|
| `/api/geocercas` | Hoteles y puntos de pickup/dropoff con coordenadas (ficticias) y radio (150 m por defecto) | Administrador |
| `/api/hitos` | Check-in, check-out y posición puntual, validados en el servidor | Conductor y guía |
| `/api/hitos/resumen` | Resumen por recurso: puntualidad y resultados de check-in por persona | Solo lectura, administrador |
| `/api/bloqueos` | Días no disponibles: con 7 días de anticipación y nunca sobre un servicio | Conductor y guía |
| `/api/parametros` | Reglas vigentes: anticipación del check-in, precisión máxima del GPS, radio por defecto y días para bloquear | Administrador (todos leen) |

El login, las respuestas (aceptar/rechazar), los reemplazos y las incidencias siguen en Firebase.

## Arquitectura

```
App (Compose → ViewModel → Repository → Retrofit)
   │  Authorization: Bearer <token de Firebase>
   ▼
Controller → Service → ReglasHito / ReglasBloqueo (sin Spring, con tests)
   │                 └→ LinceClient (consulta la API de Southbound con X-API-Key)
   ▼
Repository (JPA) → H2 (local) o PostgreSQL (Docker)
```

- **Seguridad:** no hay login propio. Spring Security valida el token de Firebase Auth (firma con las claves públicas de
  Google, emisor y audiencia `lince-track-66c55`). El rol sale del claim `rol` y los IDs de la API de los claims
  `transportistaId` y `guiaId`, que copia `scripts/sincronizar-claims.mjs` desde `usuarios/{uid}`.
- **Validación doble del check-in:** la app calcula la distancia y el backend la vuelve a calcular (Haversine), con su
  propia hora, contra la geocerca cuyo nombre coincide con el `pickup` (check-in) o `dropoff` (check-out) del servicio.
  También revisa en la API de Southbound que el servicio sea de esa persona y que no esté cancelado (XX, DL, CX).
- **Resultado del hito:** `VERIFICADO`, `FUERA_DE_ZONA`, `TARDE` (después de pickup − 15 min), `GPS_IMPRECISO`
  (precisión sobre 50 m) o `SIN_GEOCERCA`. El hito se guarda siempre como evidencia; si no es `VERIFICADO`, la app
  genera la incidencia.
- **Idempotencia:** cada hito lleva una `claveIdempotencia` (UUID de la app). Si el teléfono reenvía lo mismo al
  recuperar conexión (WorkManager), el backend responde 200 con el hito ya guardado en vez de duplicarlo.
- **Errores** con el mismo formato que la API de Southbound: `{ "error": { code, message, details, requestId } }`.

### Cambiar las reglas

Coordinación las cambia con `PUT /api/parametros` (solo administrador) y valen desde la siguiente solicitud, sin
reiniciar. Se guardan en la tabla `parametros` (una sola fila); `application.yml` (`lince.reglas`) solo trae los
valores con que se crea esa fila la primera vez.

| Parámetro | Al partir | Rango permitido | Lo usa |
|---|---|---|---|
| `anticipacionMinutos` | 15 | 0 a 240 | Check-in: después de pickup − N minutos queda `TARDE` |
| `precisionMaximaMetros` | 50 | 5 a 500 | Check-in y check-out: sobre esto queda `GPS_IMPRECISO` |
| `radioPorDefectoMetros` | 150 | 10 a 5000 | Geocercas nuevas sin radio (las existentes conservan el suyo) |
| `diasAnticipacionBloqueo` | 7 | 0 a 60 | Bloqueos: primer día que se puede bloquear |

El radio de **cada hotel** se cambia con `PUT /api/geocercas/{id}` (ese es el que usa el check-in).

## Resumen por recurso

`GET /api/hitos/resumen?desde=2026-11-01&hasta=2026-11-30` (administrador; sin fechas, los últimos 30 días; máximo 366)
devuelve el `total` y una fila por persona en `recursos`, la menos puntual primero:

| Campo | Qué es |
|---|---|
| `uid`, `rol` | Cuenta de Firebase y rol (la app pone el nombre desde `usuarios/{uid}`) |
| `servicios` | Servicios con al menos un check-in en el rango |
| `aTiempo`, `tarde`, `fueraDeZona`, `gpsImpreciso`, `sinGeocerca` | Resultado del **último** check-in de cada servicio (si reintentó, vale el último) |
| `intentosCheckIn` | Todos los check-in, contando reintentos |
| `conCheckOut` | Servicios que además tienen check-out |
| `puntualidad` | Porcentaje a tiempo (`aTiempo / servicios`) |
| `minutosAnticipacionPromedio` | Cuántos minutos antes del pickup llegó en promedio (negativo = después) |

El rango se aplica a la hora en que se **registró** el hito (hora de Chile). Cada hito guarda el rol y la hora de pickup
del servicio desde esta versión; los hitos anteriores no aportan a los minutos de anticipación.

## Correr en el computador (sin Docker)

Necesita Java 21 o más nuevo (sirve el `jbr` de Android Studio).

```powershell
cd backend
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:LINCE_API_KEY = "<clave del curso>"      # no escribirla en archivos del repo
.\gradlew.bat bootRun
```

- Swagger UI: http://localhost:8080/swagger-ui.html (botón **Authorize** → pegar el token de Firebase, sin "Bearer").
- La base queda en `backend/data/` (H2 en archivo, ignorada en Git). Al partir vacía se cargan las 4 geocercas de PUX.
- Desde el celular: misma red Wi-Fi y la IP del computador (`ipconfig`), por ejemplo `http://192.168.1.20:8080/`.
  Puede hacer falta permitir Java en el firewall de Windows.

## Con Docker (PostgreSQL)

```powershell
cd backend
copy .env.example .env    # y completar LINCE_API_KEY
docker compose up --build
```

## Tests

```powershell
.\gradlew.bat test        # reporte de cobertura en build/reports/jacoco/test/html/index.html
```

Cobertura al 16-11-2026: 95 % de líneas y 83 % de ramas (87 tests). Qué prueba cada grupo:

| Tests | Cómo |
|---|---|
| `ReglasHitoTest`, `ReglasBloqueoTest`, `ReglasDuenoTest`, `ReglasResumenTest`, `DistanciaTest` | Las reglas solas, sin Spring ni base de datos |
| `ApiIntegracionTest` | La aplicación completa con H2 en memoria, la API de Southbound y Firestore simulados (Mockito), la hora fija y tokens simulados con los mismos claims que Firebase |
| `LinceRestClientTest` | El cliente HTTP contra una API simulada (`MockRestServiceServer`): header, paginación, 404, 401, 429 y sin clave |
| `FirestoreAsignacionesTest` | La lectura de reemplazos con Firestore simulado, incluido un fallo de red |
| `ServicioLinceJsonTest` | Que el JSON real de la API se lea bien |
| `UsuarioYSeguridadTest` | Los claims del token: rol, IDs como número o texto |
| `FirestoreAsignacionesRealTest` | Opcional: lee el Firestore real (ver "Reemplazos") |

## Claims de Firebase (una vez, y cada vez que cambie un rol o un ID)

```powershell
cd backend\scripts
npm install
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\fuera\del\repo\cuenta-de-servicio.json"
npm run sincronizar                 # vista previa
npm run sincronizar -- --aplicar
```

Después cada persona debe cerrar sesión y volver a entrar para que su token traiga los claims.

## Token de prueba para Swagger o curl

```powershell
cd backend\scripts
node token-de-prueba.mjs "Carlos" > $env:TEMP\token-carlos.txt   # o "Admin", "Patricio", o el uid
```

Usa la misma cuenta de servicio (`GOOGLE_APPLICATION_CREDENTIALS`) y no necesita contraseñas. El token vale 1 hora y
trae los claims; no compartirlo ni subirlo. La guía para la defensa está en `DEFENSA.md`.

## Reemplazos (asignaciones en Firestore)

Cuando el administrador envía a otra persona, la app guarda `asignaciones/{servicioId}` en Firestore. El backend lo
lee con Firebase Admin y aplica la misma regla que la app (`ReglasDueno`):

- Con reemplazo, el servicio es **solo** de la cuenta reemplazante (por `uid`); quien lo tenía recibe 403.
- Con reemplazo, el `DL` o `CX` de la API (de la persona anterior) no cierra el servicio; solo lo cierra `XX`.
- Al bloquear un día: no cuenta un servicio propio que se reasignó a otra persona, y sí cuenta uno recibido.

Para activarlo, `GOOGLE_APPLICATION_CREDENTIALS` debe apuntar a la cuenta de servicio (fuera del repo):

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS = "D:\ruta\fuera\del\repo\cuenta-de-servicio.json"
.\gradlew.bat bootRun     # el log dice "Reemplazos activos"
```

Sin esa variable el backend arranca igual, avisa en el log y funciona como si nadie hubiera sido reemplazado (así
puede trabajar quien no tiene acceso a Firebase). Para probar la conexión real (solo lectura):

```powershell
$env:LINCE_PRUEBA_FIRESTORE = "1"
.\gradlew.bat test --tests "*FirestoreAsignacionesRealTest"
```

## Limitaciones conocidas

- Las geocercas solo cubren los 4 lugares de PUX que usa la API hoy.
- Leer un reemplazo es una consulta a Firestore por cada hito o bloqueo (unas pocas por día y persona).
