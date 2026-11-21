// Entrega un token real de Firebase (ID token, vale 1 hora) de una cuenta de prueba, para usarlo en Swagger o curl.
// No usa contraseñas: con la cuenta de servicio crea un "custom token" y lo cambia por un ID token en Firebase Auth.
// El token trae los claims (rol, transportistaId, guiaId) que copió sincronizar-claims.mjs.
//
// Uso (desde backend/scripts, con GOOGLE_APPLICATION_CREDENTIALS definida):
//   node token-de-prueba.mjs "Carlos"          # busca por nombre en usuarios/{uid} (o pasa el uid completo)
//   node token-de-prueba.mjs "Admin" > token.txt
//
// Solo el token sale por la salida estándar; los avisos van a la de errores. No lo compartas ni lo subas:
// mientras dure, cualquiera puede actuar como esa cuenta.

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { initializeApp, cert } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";

const buscado = process.argv[2];
if (!buscado) {
  console.error('Indica el nombre o uid de la cuenta, por ejemplo: node token-de-prueba.mjs "Carlos"');
  process.exit(1);
}

// La clave web de Firebase es pública (va dentro del APK); se lee del google-services.json de la app.
const servicios = JSON.parse(readFileSync(fileURLToPath(new URL("../../app/google-services.json", import.meta.url)), "utf8"));
const claveWeb = servicios.client[0].api_key[0].current_key;

const rutaCuenta = process.env.GOOGLE_APPLICATION_CREDENTIALS;
if (!rutaCuenta) {
  console.error("Falta GOOGLE_APPLICATION_CREDENTIALS con la ruta de la cuenta de servicio.");
  process.exit(1);
}
// cert() firma el custom token en el computador con la llave de la cuenta de servicio. Con applicationDefault()
// Firebase intenta firmarlo en la nube y pide activar la API "IAM Service Account Credentials".
initializeApp({ credential: cert(rutaCuenta), projectId: "lince-track-66c55" });

// Ante cualquier error, solo el mensaje: el detalle completo puede traer tokens de acceso de Google.
process.on("unhandledRejection", (e) => {
  console.error("Error:", e?.message ?? e);
  process.exit(1);
});

const usuarios = await getFirestore().collection("usuarios").get();
const candidatos = usuarios.docs.filter(
  (d) => d.id === buscado || (d.data().nombre ?? "").toLowerCase().includes(buscado.toLowerCase())
);
if (candidatos.length !== 1) {
  console.error(candidatos.length === 0
    ? `No hay ninguna cuenta que coincida con "${buscado}".`
    : `Hay ${candidatos.length} cuentas que coinciden con "${buscado}": ${candidatos.map((d) => d.data().nombre).join(", ")}.`);
  process.exit(1);
}

const uid = candidatos[0].id;
const customToken = await getAuth().createCustomToken(uid);
const respuesta = await fetch(
  `https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=${claveWeb}`,
  {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token: customToken, returnSecureToken: true }),
  }
);
const datos = await respuesta.json();
if (!respuesta.ok) {
  console.error("Firebase rechazó el canje:", datos.error?.message ?? respuesta.status);
  process.exit(1);
}

const claims = (await getAuth().getUser(uid)).customClaims ?? {};
console.error(`Token de ${candidatos[0].data().nombre} (${uid}), claims ${JSON.stringify(claims)}, vence en 1 hora.`);
process.stdout.write(datos.idToken);
