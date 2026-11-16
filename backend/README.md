# Lince Track: backend

Microservicio Spring Boot que guarda lo que la API de Southbound (solo lectura) no puede guardar:

| Recurso | Para qué | Quién escribe |
|---|---|---|
| `/api/geocercas` | Hoteles y puntos de pickup/dropoff con coordenadas (ficticias) y radio (150 m por defecto) | Administrador |
| `/api/hitos` | Check-in, check-out y posición puntual, validados en el servidor | Conductor y guía |
| `/api/bloqueos` | Días no disponibles: con 7 días de anticipación y nunca sobre un servicio | Conductor y guía |

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

Las reglas se cambian en `src/main/resources/application.yml` (`lince.reglas`): radio por defecto, anticipación,
precisión máxima y días de anticipación para bloquear. El radio de cada hotel se cambia con `PUT /api/geocercas/{id}`.

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

`ApiIntegracionTest` levanta la aplicación con H2 en memoria, la API de Southbound simulada (Mockito), la hora fija y
tokens simulados con los mismos claims que Firebase.

## Claims de Firebase (una vez, y cada vez que cambie un rol o un ID)

```powershell
cd backend\scripts
npm install
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\fuera\del\repo\cuenta-de-servicio.json"
npm run sincronizar                 # vista previa
npm run sincronizar -- --aplicar
```

Después cada persona debe cerrar sesión y volver a entrar para que su token traiga los claims.

## Limitaciones conocidas

- Los reemplazos viven en Firestore (`asignaciones`): el backend solo conoce a la persona que trae la API, así que
  quien entra como reemplazo recibe 403 al marcar. Hay que resolverlo (por ejemplo, que el backend lea `asignaciones`
  con la cuenta de servicio) antes de usar los hitos en el flujo del video.
- Las geocercas solo cubren los 4 lugares de PUX que usa la API hoy.
