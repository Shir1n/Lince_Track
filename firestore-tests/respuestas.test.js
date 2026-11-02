// Reglas de firestore.rules para la colección "respuestas". Corren en el emulador,
// con un proyecto demo: no tocan los datos reales de Firebase.
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, getDocs, collection, query, serverTimestamp, setDoc, where } from 'firebase/firestore';

let entorno;

const conductor = () => entorno.authenticatedContext('conductor1').firestore();
const guia = () => entorno.authenticatedContext('guia1').firestore();
const admin = () => entorno.authenticatedContext('admin1').firestore();

const aceptada = (uid = 'conductor1', rol = 'conductor', servicioId = '8890') => ({
  uid,
  servicioId,
  rol,
  estado: 'aceptado',
  respondidoEn: serverTimestamp(),
});

const rechazada = (extra = {}) => ({
  uid: 'conductor1',
  servicioId: '8890',
  rol: 'conductor',
  estado: 'rechazado',
  motivo: 'falla_mecanica',
  observaciones: '',
  respondidoEn: serverTimestamp(),
  ...extra,
});

const ref = (db, id = 'conductor1_8890') => doc(db, 'respuestas', id);

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
    await setDoc(doc(db, 'usuarios/conductor1'), { rol: 'conductor' });
    await setDoc(doc(db, 'usuarios/guia1'), { rol: 'guia' });
    await setDoc(doc(db, 'usuarios/admin1'), { rol: 'administrador' });
  });
});

// Deja guardada una respuesta sin pasar por las reglas.
const sembrar = (datos, id = 'conductor1_8890') =>
  entorno.withSecurityRulesDisabled((contexto) => setDoc(ref(contexto.firestore(), id), datos));

test('conductor acepta un servicio', () => assertSucceeds(setDoc(ref(conductor()), aceptada())));

test('guía acepta un servicio', () =>
  assertSucceeds(setDoc(ref(guia(), 'guia1_8901'), aceptada('guia1', 'guia', '8901'))));

test('conductor rechaza con motivo', () => assertSucceeds(setDoc(ref(conductor()), rechazada())));

test('rechazo con "otro" y observaciones', () =>
  assertSucceeds(setDoc(ref(conductor()), rechazada({ motivo: 'otro', observaciones: 'Se atrasó el ferry' }))));

test('sin sesión no se puede responder', () =>
  assertFails(setDoc(ref(entorno.unauthenticatedContext().firestore()), aceptada())));

test('no se puede escribir con el uid de otro', () =>
  assertFails(setDoc(ref(guia()), aceptada())));

test('no se puede usar un rol distinto al propio', () =>
  assertFails(setDoc(ref(conductor()), aceptada('conductor1', 'guia'))));

test('el id del documento debe calzar con uid y servicio', () =>
  assertFails(setDoc(ref(conductor(), 'conductor1_9999'), aceptada())));

test('el administrador no responde servicios', () =>
  assertFails(setDoc(ref(admin(), 'admin1_8890'), aceptada('admin1', 'administrador'))));

test('rechazo sin motivo denegado', async () => {
  const { motivo, ...sinMotivo } = rechazada();
  await assertFails(setDoc(ref(conductor()), sinMotivo));
});

test('motivo desconocido denegado', () =>
  assertFails(setDoc(ref(conductor()), rechazada({ motivo: 'me_da_lata' }))));

test('"otro" sin observaciones denegado', () =>
  assertFails(setDoc(ref(conductor()), rechazada({ motivo: 'otro', observaciones: '   ' }))));

test('observaciones de más de 500 caracteres denegadas', () =>
  assertFails(setDoc(ref(conductor()), rechazada({ observaciones: 'x'.repeat(501) }))));

test('una aceptación no lleva motivo', () =>
  assertFails(setDoc(ref(conductor()), { ...aceptada(), motivo: 'otro', observaciones: 'x' })));

test('no se aceptan campos extra', () =>
  assertFails(setDoc(ref(conductor()), { ...aceptada(), tarifa: 1000 })));

test('la hora debe ser la del servidor', () =>
  assertFails(setDoc(ref(conductor()), { ...aceptada(), respondidoEn: new Date(0) })));

test('un servicio aceptado se puede rechazar después', async () => {
  await sembrar({ ...aceptada(), respondidoEn: new Date() });
  await assertSucceeds(setDoc(ref(conductor()), rechazada()));
});

test('un rechazo es definitivo', async () => {
  await sembrar({ ...rechazada(), respondidoEn: new Date() });
  await assertFails(setDoc(ref(conductor()), aceptada()));
  await assertFails(setDoc(ref(conductor()), rechazada({ motivo: 'cruce_horario' })));
});

test('no se puede volver a aceptar lo aceptado', async () => {
  await sembrar({ ...aceptada(), respondidoEn: new Date() });
  await assertFails(setDoc(ref(conductor()), aceptada()));
});

test('nadie puede borrar una respuesta', async () => {
  await sembrar({ ...aceptada(), respondidoEn: new Date() });
  await assertFails(deleteDoc(ref(conductor())));
  await assertFails(deleteDoc(ref(admin())));
});

test('cada uno lee sus respuestas y el administrador todas', async () => {
  await sembrar({ ...aceptada(), respondidoEn: new Date() });
  await assertSucceeds(getDoc(ref(conductor())));
  await assertSucceeds(getDoc(ref(admin())));
  await assertFails(getDoc(ref(guia())));
});

test('la consulta de la app por uid está permitida solo para el propio uid', async () => {
  const consulta = (db, uid) => getDocs(query(collection(db, 'respuestas'), where('uid', '==', uid)));
  await assertSucceeds(consulta(conductor(), 'conductor1'));
  await assertFails(consulta(guia(), 'conductor1'));
  await assertFails(getDocs(collection(conductor(), 'respuestas')));
});

test('usuarios y colecciones no declaradas siguen protegidos', async () => {
  await assertFails(setDoc(doc(conductor(), 'usuarios/conductor1'), { rol: 'administrador' }));
  await assertFails(getDoc(doc(conductor(), 'usuarios/guia1')));
  await assertFails(setDoc(doc(conductor(), 'otra/x'), { a: 1 }));
});
