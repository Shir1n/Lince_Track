// Reglas de firestore.rules para la colección "tokensFcm". Corren en el emulador,
// con un proyecto demo: no tocan los datos reales de Firebase.
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, serverTimestamp, setDoc } from 'firebase/firestore';

let entorno;

const conductor = () => entorno.authenticatedContext('conductor1').firestore();
const admin = () => entorno.authenticatedContext('admin1').firestore();
const sinRol = () => entorno.authenticatedContext('intruso').firestore();
const anonimo = () => entorno.unauthenticatedContext().firestore();

const token = (extra = {}) => ({ token: 'fcm-abc', actualizadoEn: serverTimestamp(), ...extra });
const ref = (db, uid = 'conductor1') => doc(db, 'tokensFcm', uid);

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
    await setDoc(doc(db, 'usuarios/admin1'), { rol: 'administrador' });
    await setDoc(doc(db, 'tokensFcm/admin1'), { token: 'fcm-admin', actualizadoEn: new Date() });
  });
});

test('cada persona guarda y reemplaza el token de su teléfono', async () => {
  await assertSucceeds(setDoc(ref(conductor()), token()));
  await assertSucceeds(setDoc(ref(conductor()), token({ token: 'fcm-nuevo' })));
});

test('nadie guarda el token de otra cuenta', () =>
  assertFails(setDoc(ref(conductor(), 'admin1'), token())));

test('sin rol o sin sesión no se guarda', async () => {
  await assertFails(setDoc(ref(sinRol(), 'intruso'), token()));
  await assertFails(setDoc(ref(anonimo()), token()));
});

test('el token debe ser texto, con la hora del servidor y sin otros campos', async () => {
  await assertFails(setDoc(ref(conductor()), token({ token: '' })));
  await assertFails(setDoc(ref(conductor()), token({ token: 123 })));
  await assertFails(setDoc(ref(conductor()), token({ actualizadoEn: new Date(2026, 0, 1) })));
  await assertFails(setDoc(ref(conductor()), token({ rol: 'administrador' })));
});

test('nadie lee tokens desde la app, ni el administrador', async () => {
  await assertFails(getDoc(ref(conductor(), 'admin1')));
  await assertFails(getDoc(ref(admin(), 'admin1')));
});

test('solo el dueño borra su token', async () => {
  await assertFails(deleteDoc(ref(conductor(), 'admin1')));
  await assertSucceeds(deleteDoc(ref(admin(), 'admin1')));
});
