// Pruebas de la eliminación de datos (Ley 21.719), sin Firebase: Firestore, Auth y Storage simulados.
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { borrarDatosDe } from '../privacidad.js';
import * as reglas from '../reglas.js';

/** Firestore en memoria: { 'coleccion/id': { ...campos } }. */
function firestoreSimulado(datos) {
  const documento = (ruta) => ({
    get: async () => ({ exists: ruta in datos, ref: documento(ruta) }),
    delete: async () => {
      delete datos[ruta];
    },
  });
  return {
    doc: documento,
    collection: (coleccion) => ({
      where: (campo, _op, valor) => ({
        get: async () => {
          const docs = Object.entries(datos)
            .filter(([ruta, d]) => ruta.startsWith(`${coleccion}/`) && d[campo] === valor)
            .map(([ruta]) => ({ ref: documento(ruta) }));
          return { docs, size: docs.length };
        },
      }),
    }),
  };
}

const solicitud = { uid: 'u1', nombre: 'Patricio Lagos', rol: 'conductor', motivo: 'Me cambio de trabajo', estado: 'pendiente' };

test('al aprobar se borra todo lo de la persona y nada de las demás', async () => {
  const datos = {
    'checkins/u1_3166': { uid: 'u1' },
    'incidencias/a': { uid: 'u1' },
    'respuestas/u1_3166': { uid: 'u1' },
    'estadosServicio/3166': { uid: 'u1' },
    'asignaciones/3272': { uid: 'u1' },
    'checkins/u2_3166': { uid: 'u2' },
    'tokensFcm/u1': { token: 't' },
    'usuarios/u1': { nombre: 'Patricio' },
    'usuarios/u2': { nombre: 'Carlos' },
  };
  const archivosBorrados = [];
  const bucket = {
    getFiles: async ({ prefix }) => [prefix.startsWith('perfil/') ? [{ delete: async () => archivosBorrados.push(prefix) }] : []],
  };
  const cuentasBorradas = [];
  const auth = { deleteUser: async (uid) => cuentasBorradas.push(uid) };

  const borrados = await borrarDatosDe('u1', { db: firestoreSimulado(datos), auth, bucket });

  assert.deepEqual(borrados, {
    checkins: 1, incidencias: 1, respuestas: 1, estadosServicio: 1, asignaciones: 1,
    tokensFcm: 1, usuarios: 1, fotos: 1, cuenta: 1,
  });
  assert.deepEqual(Object.keys(datos).sort(), ['checkins/u2_3166', 'usuarios/u2']);
  assert.deepEqual(archivosBorrados, ['perfil/u1/']);
  assert.deepEqual(cuentasBorradas, ['u1']);
});

test('sin Storage activado ni cuenta en Auth, igual termina', async () => {
  const bucket = { getFiles: async () => { throw new Error('bucket inexistente'); } };
  const auth = { deleteUser: async () => { throw Object.assign(new Error('no'), { code: 'auth/user-not-found' }); } };

  const borrados = await borrarDatosDe('u9', { db: firestoreSimulado({}), auth, bucket });

  assert.equal(borrados.fotos, 0);
  assert.equal(borrados.cuenta, 0);
  assert.equal(borrados.usuarios, 0);
});

test('otro error de Auth no se esconde', async () => {
  const bucket = { getFiles: async () => [[]] };
  const auth = { deleteUser: async () => { throw Object.assign(new Error('caído'), { code: 'auth/internal-error' }); } };
  await assert.rejects(borrarDatosDe('u1', { db: firestoreSimulado({}), auth, bucket }), /caído/);
});

test('una solicitud nueva avisa a Coordinación, sin servicio y con su propia etiqueta', () => {
  const aviso = reglas.avisoSolicitudPrivacidad(undefined, solicitud);
  assert.deepEqual(aviso, {
    tipo: 'privacidad_solicitud',
    servicioId: '',
    etiqueta: 'privacidad|u1',
    titulo: 'Solicitud de eliminación de datos',
    texto: 'Patricio Lagos pidió eliminar sus datos: Me cambio de trabajo',
  });
  assert.deepEqual(reglas.mensaje('tok', aviso).data, { tipo: 'privacidad_solicitud' });
  assert.equal(reglas.mensaje('tok', aviso).android.notification.tag, 'privacidad|u1');
  // Volver a pedir tras un rechazo también avisa; un cambio sin pasar a pendiente, no.
  assert.ok(reglas.avisoSolicitudPrivacidad({ ...solicitud, estado: 'rechazada' }, solicitud));
  assert.equal(reglas.avisoSolicitudPrivacidad(solicitud, solicitud), null);
  assert.equal(reglas.avisoSolicitudPrivacidad(solicitud, undefined), null);
  assert.equal(
    reglas.avisoSolicitudPrivacidad(undefined, { ...solicitud, nombre: '', motivo: '', rol: 'guia' }).texto,
    'Un guía pidió eliminar sus datos personales.',
  );
});

test('solo el rechazo avisa a la persona; la aprobación dispara el borrado', () => {
  const rechazada = { ...solicitud, estado: 'rechazada', respuesta: 'Tienes servicios esta semana' };
  assert.equal(reglas.avisoRechazoPrivacidad(solicitud, rechazada).texto, 'Coordinación: Tienes servicios esta semana');
  assert.equal(reglas.avisoRechazoPrivacidad(rechazada, rechazada), null);

  const aprobada = { ...solicitud, estado: 'aprobada' };
  assert.ok(reglas.esAprobacionPrivacidad(solicitud, aprobada));
  assert.equal(reglas.esAprobacionPrivacidad(aprobada, { ...aprobada, estado: 'completada' }), false);
  assert.equal(reglas.avisoRechazoPrivacidad(solicitud, aprobada), null);
});

test('el registro que queda no guarda nombre, motivo ni respuesta', () => {
  const registro = reglas.registroCompletado(
    { ...solicitud, estado: 'aprobada', respuesta: 'ok', creadaEn: 'c', atendidaEn: 'a' }, { checkins: 2 }, 'ahora',
  );
  assert.deepEqual(registro, {
    uid: 'u1', estado: 'completada', creadaEn: 'c', atendidaEn: 'a', completadaEn: 'ahora', borrados: { checkins: 2 },
  });
  assert.equal(reglas.registroCompletado({ uid: 'u1' }, {}, 'x').creadaEn, null);
});
