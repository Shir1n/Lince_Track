// Copia rol, transportistaId y guiaId de usuarios/{uid} (Firestore) a los custom claims de cada cuenta de Firebase Auth.
// El backend lee esos claims del token para saber el rol y qué servicios de la API son de esa persona.
//
// Uso (desde backend/scripts):
//   npm install
//   $env:GOOGLE_APPLICATION_CREDENTIALS = "C:\ruta\fuera\del\repo\cuenta-de-servicio.json"   # PowerShell
//   npm run sincronizar            # muestra lo que haría
//   npm run sincronizar -- --aplicar
//
// La cuenta de servicio se descarga en Firebase > Configuración del proyecto > Cuentas de servicio.
// NUNCA guardarla dentro del repositorio. Tras aplicar, cada persona debe cerrar sesión y volver a entrar
// (o esperar hasta 1 hora) para que su token traiga los claims nuevos.

import { initializeApp, applicationDefault } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";

const aplicar = process.argv.includes("--aplicar");

initializeApp({ credential: applicationDefault(), projectId: "lince-track-66c55" });
const auth = getAuth();
const db = getFirestore();

const usuarios = await db.collection("usuarios").get();
for (const doc of usuarios.docs) {
  const u = doc.data();
  const claims = { rol: u.rol ?? null };
  if (typeof u.transportistaId === "number") claims.transportistaId = u.transportistaId;
  if (typeof u.guiaId === "number") claims.guiaId = u.guiaId;

  console.log(`${doc.id} (${u.nombre ?? "sin nombre"}):`, JSON.stringify(claims));
  if (aplicar) {
    try {
      await auth.setCustomUserClaims(doc.id, claims);
    } catch (e) {
      console.error(`  no se pudo: ${e.message}`);
    }
  }
}
console.log(aplicar ? "Claims aplicados." : "Solo vista previa: agrega --aplicar para guardarlos.");
