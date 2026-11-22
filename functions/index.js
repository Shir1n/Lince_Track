// Cloud Functions de Lince Track: notificaciones con la app cerrada.
// Cada función escucha una colección de Firestore, decide el aviso con reglas.js y lo envía con
// Firebase Cloud Messaging al teléfono guardado en tokensFcm/{uid}. Requiere el plan Blaze.
import { initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { getMessaging } from 'firebase-admin/messaging';
import { logger, setGlobalOptions } from 'firebase-functions';
import { onDocumentCreated, onDocumentUpdated, onDocumentWritten } from 'firebase-functions/firestore';
import * as reglas from './reglas.js';

initializeApp();
// Misma región que la base de datos (Santiago). Pocas instancias: el uso es bajo y así se limita el costo.
setGlobalOptions({ region: 'southamerica-west1', maxInstances: 3 });

const db = getFirestore();

async function administradores() {
  const consulta = await db.collection('usuarios').where('rol', '==', 'administrador').get();
  return consulta.docs.map((documento) => documento.id);
}

/** Envía [aviso] a cada uid que tenga teléfono registrado y borra los tokens que ya no sirven. */
async function enviar(uids, aviso) {
  if (!aviso || uids.length === 0) return;
  const documentos = await Promise.all(uids.map((uid) => db.doc(`tokensFcm/${uid}`).get()));
  const conToken = documentos.filter((documento) => documento.exists && documento.get('token'));
  if (conToken.length === 0) {
    logger.info('Sin teléfonos registrados', { tipo: aviso.tipo, servicioId: aviso.servicioId });
    return;
  }
  const resultado = await getMessaging().sendEach(conToken.map((d) => reglas.mensaje(d.get('token'), aviso)));
  await Promise.all(resultado.responses.map((respuesta, i) => {
    if (respuesta.success) return null;
    const codigo = respuesta.error?.code;
    logger.warn('No se pudo enviar el aviso', { uid: conToken[i].id, codigo });
    return reglas.tokenInvalido(codigo) ? conToken[i].ref.delete() : null;
  }));
  logger.info('Aviso enviado', {
    tipo: aviso.tipo,
    servicioId: aviso.servicioId,
    enviados: resultado.successCount,
    fallidos: resultado.failureCount,
  });
}

/** Coordinación eligió un reemplazo: avisa al conductor o guía nuevo. */
export const avisarAsignacion = onDocumentWritten('asignaciones/{servicioId}', async (evento) => {
  const despues = evento.data?.after?.data();
  const aviso = reglas.avisoAsignacion(evento.data?.before?.data(), despues);
  if (aviso) await enviar([despues.uid], aviso);
});

/** Un conductor o guía reportó una incidencia: avisa a los administradores. */
export const avisarIncidenciaNueva = onDocumentCreated('incidencias/{id}', async (evento) => {
  const aviso = reglas.avisoIncidenciaNueva(evento.data?.data());
  if (aviso) await enviar(await administradores(), aviso);
});

/** Coordinación respondió o resolvió una incidencia: avisa a quien la reportó. */
export const avisarIncidenciaAtendida = onDocumentUpdated('incidencias/{id}', async (evento) => {
  const despues = evento.data?.after?.data();
  const aviso = reglas.avisoIncidenciaAtendida(evento.data?.before?.data(), despues);
  if (aviso) await enviar([despues.uid], aviso);
});

/** Alguien rechazó un servicio: avisa a los administradores para que lo reasignen. */
export const avisarRechazo = onDocumentWritten('respuestas/{id}', async (evento) => {
  const despues = evento.data?.after?.data();
  if (!reglas.esRechazoNuevo(evento.data?.before?.data(), despues)) return;
  const persona = await db.doc(`usuarios/${despues.uid}`).get();
  await enviar(await administradores(), reglas.avisoRechazo(despues, persona.get('nombre')));
});
