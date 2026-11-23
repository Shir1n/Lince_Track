// Borra los datos de una persona en Firestore, Storage y Auth cuando Coordinación aprueba su solicitud
// (Ley 21.719). Recibe Firestore, Auth y el bucket por parámetro para probarlo sin Firebase.
import { CARPETAS_STORAGE, COLECCIONES_CON_UID } from './reglas.js';

/**
 * @returns cantidades borradas por colección, más "fotos" y "cuenta" (1 si se borró la cuenta de Auth).
 */
export async function borrarDatosDe(uid, { db, auth, bucket }) {
  const borrados = {};

  for (const coleccion of COLECCIONES_CON_UID) {
    const consulta = await db.collection(coleccion).where('uid', '==', uid).get();
    await Promise.all(consulta.docs.map((documento) => documento.ref.delete()));
    borrados[coleccion] = consulta.size;
  }

  for (const coleccion of ['tokensFcm', 'usuarios']) {
    const referencia = db.doc(`${coleccion}/${uid}`);
    const documento = await referencia.get();
    if (documento.exists) await referencia.delete();
    borrados[coleccion] = documento.exists ? 1 : 0;
  }

  // Storage puede no estar activado todavía (las fotos aún no se implementan): no debe frenar el resto.
  borrados.fotos = 0;
  for (const prefijo of CARPETAS_STORAGE(uid)) {
    try {
      const [archivos] = await bucket.getFiles({ prefix: prefijo });
      await Promise.all(archivos.map((archivo) => archivo.delete()));
      borrados.fotos += archivos.length;
    } catch {
      // Sin bucket o sin permisos: no hay fotos que borrar.
    }
  }

  try {
    await auth.deleteUser(uid);
    borrados.cuenta = 1;
  } catch (error) {
    if (error?.code !== 'auth/user-not-found') throw error;
    borrados.cuenta = 0;
  }
  return borrados;
}
