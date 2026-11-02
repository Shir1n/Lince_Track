// Reglas de firestore.rules para la colección "checkins". Corren en el emulador,
// con un proyecto demo: no tocan los datos reales de Firebase.
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { collection, deleteDoc, doc, getDoc, getDocs, query, serverTimestamp, setDoc, updateDoc, where } from 'firebase/firestore';

let entorno;

const conductor = () => entorno.authenticatedContext('conductor1').firestore();
const guia = () => entorno.authenticatedContext('guia1').firestore();
const admin = () => entorno.authenticatedContext('admin1').firestore();

const ref = (db, id = 'conductor1_8941') => doc(db, 'checkins', id);

const checkIn = (extra = {}) => ({
  uid: 'conductor1',
  servicioId: '8941',
  rol: 'conductor',
  checkInEn: serverTimestamp(),
  lat: -41.3195,
  lon: -72.9854,
  distanciaM: 120,
  gpsReal: true,
  ...extra,
});

const checkOut = (extra = {}) => ({
  checkOutEn: serverTimestamp(),
  latFin: -41.1731,
  lonFin: -72.4468,
  ...extra,
});

// Deja guardado un check-in sin pasar por las reglas.
const sembrar = (extra = {}) =>
  entorno.withSecurityRulesDisabled((contexto) =>
    setDoc(ref(contexto.firestore()), { ...checkIn(), checkInEn: new Date(), ...extra })
  );

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

test('conductor registra su check-in', () => assertSucceeds(setDoc(ref(conductor()), checkIn())));

test('guía registra su check-in', () =>
  assertSucceeds(setDoc(ref(guia(), 'guia1_8942'), checkIn({ uid: 'guia1', rol: 'guia', servicioId: '8942' }))));

test('se acepta un check-in con GPS simulado', () =>
  assertSucceeds(setDoc(ref(conductor()), checkIn({ gpsReal: false }))));

test('sin sesión no se registra', () =>
  assertFails(setDoc(ref(entorno.unauthenticatedContext().firestore()), checkIn())));

test('no se puede registrar con el uid de otro', () => assertFails(setDoc(ref(guia()), checkIn())));

test('el id debe calzar con uid y servicio', () =>
  assertFails(setDoc(ref(conductor(), 'conductor1_9999'), checkIn())));

test('el administrador no registra check-in', () =>
  assertFails(setDoc(ref(admin(), 'admin1_8941'), checkIn({ uid: 'admin1', rol: 'administrador' }))));

test('la hora debe ser la del servidor', () =>
  assertFails(setDoc(ref(conductor()), checkIn({ checkInEn: new Date(0) }))));

test('coordenadas inválidas denegadas', () =>
  assertFails(setDoc(ref(conductor()), checkIn({ lat: 'cerca' }))));

test('no se aceptan campos extra', () =>
  assertFails(setDoc(ref(conductor()), checkIn({ tarifa: 1000 }))));

test('no se puede hacer check-in dos veces', async () => {
  await sembrar();
  await assertFails(setDoc(ref(conductor()), checkIn()));
});

test('no se puede crear un check-in ya cerrado', () =>
  assertFails(setDoc(ref(conductor()), { ...checkIn(), ...checkOut() })));

test('check-out con check-in previo', async () => {
  await sembrar();
  await assertSucceeds(updateDoc(ref(conductor()), checkOut()));
});

test('check-out sin check-in denegado', () =>
  assertFails(updateDoc(ref(conductor()), checkOut())));

test('doble check-out denegado', async () => {
  await sembrar({ checkOutEn: new Date(), latFin: -41.1, lonFin: -72.4 });
  await assertFails(updateDoc(ref(conductor()), checkOut()));
});

test('otra persona no hace el check-out', async () => {
  await sembrar();
  await assertFails(updateDoc(ref(guia()), checkOut()));
  await assertFails(updateDoc(ref(admin()), checkOut()));
});

test('el check-out no puede cambiar la distancia', async () => {
  await sembrar();
  await assertFails(updateDoc(ref(conductor()), checkOut({ distanciaM: 1 })));
});

test('el check-out exige la hora del servidor y las coordenadas', async () => {
  await sembrar();
  await assertFails(updateDoc(ref(conductor()), checkOut({ checkOutEn: new Date(0) })));
  await assertFails(updateDoc(ref(conductor()), { checkOutEn: serverTimestamp() }));
});

test('nadie puede borrar un check-in', async () => {
  await sembrar();
  await assertFails(deleteDoc(ref(conductor())));
  await assertFails(deleteDoc(ref(admin())));
});

test('solo el dueño y el administrador leen la ubicación', async () => {
  await sembrar();
  await assertSucceeds(getDoc(ref(conductor())));
  await assertSucceeds(getDoc(ref(admin())));
  await assertFails(getDoc(ref(guia())));
});

test('listar: cada uno lo suyo y el administrador todo', async () => {
  await sembrar();
  const propios = (db, uid) => getDocs(query(collection(db, 'checkins'), where('uid', '==', uid)));
  await assertSucceeds(propios(conductor(), 'conductor1'));
  await assertFails(propios(guia(), 'conductor1'));
  await assertFails(getDocs(collection(conductor(), 'checkins')));
  await assertSucceeds(getDocs(collection(admin(), 'checkins')));
});
