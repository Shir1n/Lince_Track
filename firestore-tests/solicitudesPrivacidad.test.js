// Reglas de firestore.rules para "solicitudesPrivacidad" (Ley 21.719). Corren en el emulador,
// con un proyecto demo: no tocan los datos reales de Firebase.
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, serverTimestamp, setDoc, updateDoc } from 'firebase/firestore';

let entorno;

const conductor = () => entorno.authenticatedContext('conductor1').firestore();
const otro = () => entorno.authenticatedContext('conductor2').firestore();
const admin = () => entorno.authenticatedContext('admin1').firestore();

const solicitud = (extra = {}) => ({
  uid: 'conductor1',
  nombre: 'Pedro Prueba',
  rol: 'conductor',
  motivo: 'Ya no trabajo con Southbound',
  estado: 'pendiente',
  creadaEn: serverTimestamp(),
  ...extra,
});

const ref = (db, uid = 'conductor1') => doc(db, 'solicitudesPrivacidad', uid);

const respuesta = (estado, texto, extra = {}) => ({
  estado,
  respuesta: texto,
  atendidaEn: serverTimestamp(),
  atendidaPor: 'admin1',
  ...extra,
});

before(async () => {
  entorno = await initializeTestEnvironment({
    projectId: 'demo-lince-track',
    firestore: {
      rules: readFileSync('../firestore.rules', 'utf8'),
      host: '127.0.0.1',
      port: 8080,
    },
  });
});

after(() => entorno.cleanup());

beforeEach(async () => {
  await entorno.clearFirestore();
  await entorno.withSecurityRulesDisabled(async (contexto) => {
    const db = contexto.firestore();
    await setDoc(doc(db, 'usuarios/conductor1'), { rol: 'conductor', nombre: 'Pedro Prueba' });
    await setDoc(doc(db, 'usuarios/conductor2'), { rol: 'conductor', nombre: 'Miguel Prueba' });
    await setDoc(doc(db, 'usuarios/admin1'), { rol: 'administrador', nombre: 'Admin 1' });
  });
});

const sembrar = (estado = 'pendiente') =>
  entorno.withSecurityRulesDisabled((contexto) =>
    setDoc(ref(contexto.firestore()), solicitud({ estado, creadaEn: new Date() })),
  );

test('cada persona pide la eliminación de sus propios datos', async () => {
  await assertSucceeds(setDoc(ref(conductor()), solicitud()));
  await assertSucceeds(getDoc(ref(conductor())));
});

test('nadie pide por otra persona ni con otro nombre o rol', async () => {
  await assertFails(setDoc(ref(otro()), solicitud()));
  await assertFails(setDoc(ref(conductor()), solicitud({ nombre: 'Otro Nombre' })));
  await assertFails(setDoc(ref(conductor()), solicitud({ rol: 'guia' })));
});

test('la solicitud parte pendiente, con la hora del servidor y sin otros campos', async () => {
  await assertFails(setDoc(ref(conductor()), solicitud({ estado: 'aprobada' })));
  await assertFails(setDoc(ref(conductor()), solicitud({ creadaEn: new Date(2026, 0, 1) })));
  await assertFails(setDoc(ref(conductor()), solicitud({ motivo: 'x'.repeat(501) })));
  await assertFails(setDoc(ref(conductor()), solicitud({ respuesta: 'aprobada por mí' })));
});

test('el administrador no pide eliminación para sí desde la app', async () => {
  await assertFails(setDoc(ref(admin(), 'admin1'), solicitud({ uid: 'admin1', nombre: 'Admin 1', rol: 'administrador' })));
});

test('otra persona no ve la solicitud; el administrador sí', async () => {
  await sembrar();
  await assertFails(getDoc(ref(otro())));
  await assertSucceeds(getDoc(ref(admin())));
});

test('se puede cancelar mientras está pendiente, no después', async () => {
  await sembrar();
  await assertFails(deleteDoc(ref(otro())));
  await assertSucceeds(deleteDoc(ref(conductor())));
  await sembrar('aprobada');
  await assertFails(deleteDoc(ref(conductor())));
});

test('solo el administrador aprueba o rechaza, y rechazar exige motivo', async () => {
  await sembrar();
  await assertFails(updateDoc(ref(conductor()), respuesta('aprobada', '', { atendidaPor: 'conductor1' })));
  await assertFails(updateDoc(ref(admin()), respuesta('rechazada', '  ')));
  await assertFails(updateDoc(ref(admin()), respuesta('completada', 'ok')));
  await assertFails(updateDoc(ref(admin()), respuesta('aprobada', '', { atendidaPor: 'otro' })));
  await assertFails(updateDoc(ref(admin()), { ...respuesta('aprobada', ''), motivo: 'cambiado' }));
  await assertSucceeds(updateDoc(ref(admin()), respuesta('aprobada', '')));
});

test('una solicitud ya atendida no se vuelve a responder', async () => {
  await sembrar('rechazada');
  await assertFails(updateDoc(ref(admin()), respuesta('aprobada', '')));
});

test('tras un rechazo, la persona puede volver a pedirla', async () => {
  await sembrar('rechazada');
  await assertSucceeds(setDoc(ref(conductor()), solicitud({ motivo: 'Lo pido de nuevo' })));
});

test('una solicitud pendiente o aprobada no se reemplaza desde la app', async () => {
  await sembrar('pendiente');
  await assertFails(setDoc(ref(conductor()), solicitud({ motivo: 'otro motivo' })));
  await sembrar('aprobada');
  await assertFails(setDoc(ref(conductor()), solicitud()));
});
