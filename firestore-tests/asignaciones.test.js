// Reglas de firestore.rules para la colección "asignaciones". Corren en el emulador,
// con un proyecto demo: no tocan los datos reales de Firebase.
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, serverTimestamp, setDoc, writeBatch } from 'firebase/firestore';

let entorno;

const conductor = () => entorno.authenticatedContext('conductor1').firestore();
const admin = () => entorno.authenticatedContext('admin1').firestore();

const asignacion = (extra = {}) => ({
  servicioId: '8941',
  uid: 'conductor2',
  nombre: 'Miguel Prueba',
  rol: 'conductor',
  fecha: '2026-11-02',
  asignadoPor: 'admin1',
  asignadoEn: serverTimestamp(),
  ...extra,
});

const ref = (db, id = '8941') => doc(db, 'asignaciones', id);

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
    await setDoc(doc(db, 'usuarios/guia1'), { rol: 'guia', nombre: 'Alfredini Pastelini' });
    await setDoc(doc(db, 'usuarios/admin1'), { rol: 'administrador' });
  });
});

const sembrar = () =>
  entorno.withSecurityRulesDisabled((contexto) =>
    setDoc(ref(contexto.firestore()), asignacion({ asignadoEn: new Date() })),
  );

test('el administrador reasigna un servicio', () =>
  assertSucceeds(setDoc(ref(admin()), asignacion())));

test('el administrador puede cambiar el reemplazo', async () => {
  await sembrar();
  await assertSucceeds(setDoc(ref(admin()), asignacion({ uid: 'conductor1', nombre: 'Pedro Prueba' })));
});

test('reasignar y borrar el estado anterior van en un solo lote', async () => {
  await entorno.withSecurityRulesDisabled((contexto) =>
    setDoc(doc(contexto.firestore(), 'estadosServicio/8941'), { uid: 'conductor1', estado: 'rechazado' }),
  );
  const db = admin();
  const lote = writeBatch(db);
  lote.set(ref(db), asignacion());
  lote.delete(doc(db, 'estadosServicio/8941'));
  await assertSucceeds(lote.commit());
});

test('un conductor no puede reasignar', () =>
  assertFails(setDoc(ref(conductor()), asignacion({ asignadoPor: 'conductor1' }))));

test('sin sesión no se puede reasignar', () =>
  assertFails(setDoc(ref(entorno.unauthenticatedContext().firestore()), asignacion())));

test('la cuenta del reemplazo debe existir', () =>
  assertFails(setDoc(ref(admin()), asignacion({ uid: 'noexiste' }))));

test('el rol debe ser el de la cuenta', () =>
  assertFails(setDoc(ref(admin()), asignacion({ rol: 'guia' }))));

test('el nombre debe ser el de la cuenta', () =>
  assertFails(setDoc(ref(admin()), asignacion({ nombre: 'Otra Persona' }))));

test('no se asigna a un administrador', () =>
  assertFails(setDoc(ref(admin()), asignacion({ uid: 'admin1', rol: 'administrador', nombre: '' }))));

test('el id del documento debe calzar con el servicio', () =>
  assertFails(setDoc(ref(admin(), '9999'), asignacion())));

test('la hora debe ser la del servidor', () =>
  assertFails(setDoc(ref(admin()), asignacion({ asignadoEn: new Date(0) }))));

test('no se aceptan campos extra', () =>
  assertFails(setDoc(ref(admin()), asignacion({ tarifa: 1000 }))));

test('nadie puede borrar una asignación', async () => {
  await sembrar();
  await assertFails(deleteDoc(ref(admin())));
});

test('los usuarios con rol leen las asignaciones', async () => {
  await sembrar();
  await assertSucceeds(getDoc(ref(conductor())));
  await assertSucceeds(getDoc(ref(admin())));
  await assertFails(getDoc(ref(entorno.authenticatedContext('sinperfil').firestore())));
});
