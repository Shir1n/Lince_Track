# Defensa: guía del backend

Pasos para mostrar el microservicio en la defensa (EA5) y practicar la modificación en vivo. Ensayado el 16-11-2026
con tokens reales, la API de Southbound y Firestore.

## Antes de entrar (5 minutos)

En PowerShell, desde `backend/`:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:LINCE_API_KEY = "<clave del curso>"
$env:GOOGLE_APPLICATION_CREDENTIALS = "<ruta al JSON de la cuenta de servicio, fuera del repo>"
.\gradlew.bat bootRun
```

- Debe aparecer en el log: `Reemplazos activos` y `Started LinceBackendApplication`.
- Abrir http://localhost:8080/swagger-ui.html.
- En otra terminal, sacar los tokens (valen 1 hora; sacarlos justo antes):

```powershell
cd backend\scripts
$env:GOOGLE_APPLICATION_CREDENTIALS = "<la misma ruta>"
node token-de-prueba.mjs "Admin"  > $env:TEMP\token-admin.txt
node token-de-prueba.mjs "Carlos" > $env:TEMP\token-carlos.txt
```

No mostrar los tokens en pantalla ni subirlos: mientras duran, sirven para actuar como esa cuenta. Tampoco mostrar la
clave de la API ni el JSON de la cuenta de servicio.

## Recorrido en Swagger (orden sugerido)

1. **Authorize** → pegar el token del administrador (sin "Bearer").
2. `GET /api/parametros`: las reglas vigentes (15 min, 50 m, 150 m, 7 días) y de dónde salen (tabla `parametros`).
3. `GET /api/geocercas`: los 4 lugares de PUX con su radio.
4. **Cambio de regla sin código:** `PUT /api/geocercas/1` con `"radioMetros": 100`.
5. **Authorize** con el token de Carlos. `POST /api/hitos` en el servicio **3008** (tour del 20-11 a las 08:30, de
   Carlos), a unos 120 m del hotel:

   ```json
   {"servicioId": 3008, "tipo": "CHECK_IN", "lat": -41.31868, "lon": -72.9822,
    "precisionMetros": 8, "claveIdempotencia": "ensayo-1"}
   ```

   Resultado esperado: `FUERA_DE_ZONA`, `distanciaMetros` ≈ 120, `radioMetros` 100.
6. Con el token del administrador, volver el radio a 150. Con el de Carlos, repetir con otra clave (`ensayo-2`):
   ahora `VERIFICADO`. Un tercer intento da 409 `HITO_DUPLICADO`.
7. Mandar otra vez `ensayo-2`: responde **200** con el mismo hito (idempotencia, lo que usa WorkManager al reconectar).
8. Con el token de Carlos, `PUT /api/geocercas/1` → **403**: el rol sale del token de Firebase.
9. Con el administrador: `GET /api/hitos/resumen` (puntualidad por persona) y
   `GET /api/hitos/ultima-posicion?servicioId=3008`.

## Qué explicar (pauta IE 3.1.2)

| Pregunta típica | Dónde está |
|---|---|
| Entidades y base de datos | `geocerca/Geocerca`, `hito/Hito`, `bloqueo/Bloqueo`, `parametro/Parametros` (JPA). H2 en `backend/data/`, PostgreSQL con Docker |
| Endpoints | `*Controller.java`; documentados en Swagger con `@Operation` |
| Seguridad (JWT y roles) | `config/SeguridadConfig`: valida el token de Firebase con las claves públicas de Google; el claim `rol` se vuelve `ROLE_...` y los endpoints usan `@PreAuthorize` |
| Validación doble | La app calcula la distancia; `HitoService` la recalcula (Haversine en `hito/Distancia`), con la hora del servidor, y revisa en la API de Southbound que el servicio sea de esa persona |
| API del cliente solo lectura | `lince/LinceRestClient` solo hace `GET` con `X-API-Key`; lo que se registra va a nuestra base |
| Reemplazos | `asignacion/ReglasDueno`: si el administrador reasignó el servicio en Firestore, manda el reemplazo |
| Errores | `comun/ManejadorErrores`: mismo formato que la API de Southbound, con `requestId` |
| Pruebas | `.\gradlew.bat test`; reporte en `build/reports/jacoco/test/html/index.html` (95 % de líneas). Ver la tabla en `README.md` |

## Práctica de modificación en vivo

La pauta da como ejemplos: radio de 150 a 100, tolerancia de puntualidad, motivo obligatorio de incidencia, validar
CX/XX y un campo "precisión del GPS". En el backend:

| Cambio | Sin tocar código | Tocando código (recorrido) |
|---|---|---|
| Radio 150 → 100 | `PUT /api/geocercas/{id}` | `CargaInicialGeocercas` o `radioPorDefectoMetros` en `application.yml` |
| Tolerancia de puntualidad | `PUT /api/parametros` (`anticipacionMinutos`) | `ReglasHito.limiteLlegada` + su test en `ReglasHitoTest` |
| Precisión del GPS | `PUT /api/parametros` (`precisionMaximaMetros`) | Ya es campo de `Hito` y `HitoSolicitud`; la regla está en `ReglasHito.evaluar` |
| Validar CX/XX | — | `ServicioLince.estaCancelado` y `ReglasDueno.estaCerrado` + `ReglasDuenoTest` |
| Campo nuevo en un hito (por ejemplo, observación) | — | `HitoSolicitud` (con `@Size`) → `Hito` (columna) → `HitoService.registrar` → test en `ApiIntegracionTest` → Retrofit en la app |

Practicar al menos uno con código completo, de punta a punta, y correr `.\gradlew.bat test` al final.

## Punto débil conocido

No hay límite de **cuán temprano** se puede hacer el check-in: en el ensayo, un check-in hecho 4 días antes quedó
`VERIFICADO` (anticipación de 5.558 minutos). Si el profe lo pregunta, la respuesta es agregar un parámetro (por
ejemplo, "se abre 2 horas antes del pickup") en `Parametros` y una condición en `ReglasHito`; es un buen ejercicio de
modificación en vivo.
