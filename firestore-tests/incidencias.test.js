// Reglas de firestore.rules para la colección "incidencias". Corren en el emulador,
// con un proyecto demo: no tocan los datos reales de Firebase.
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
} from 'firebase/firestore';

let entorno;

const conductor = () => entorno.authenticatedContext('conductor1').firestore();
const guia = () => entorno.authenticatedContext('guia1').firestore();
const admin = () => entorno.authenticatedContext('admin1').firestore();

const incidencia = (extra = {}) => ({
  uid: 'conductor1',
  rol: 'conductor',
  nombre: 'Carlos Prueba',
  servicioId: '8941',
  titulo: 'Excursión',
  tipo: 'atraso',
  detalle: '',
  fecha: '2026-11-02',
  creadaEn: serverTimestamp(),
  resuelta: false,
  ...extra,
});

const resolucion = (extra = {}) => ({
  resuelta: true,
  resueltaEn: serverTimestamp(),
  resueltaPor: 'admin1',
  ...extra,
});

const ref = (db, id = 'i1') => doc(db, 'incidencias', id);

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
    await setDoc(doc(db, 'usuarios/conductor1'), { rol: 'conductor', nombre: 'Carlos Prueba' });
    await setDoc(doc(db, 'usuarios/guia1'), { rol: 'guia', nombre: 'Alfredini Pastelini' });
    await setDoc(doc(db, 'usuarios/admin1'), { rol: 'administrador' });
  });
});

const sembrar = (extra = {}) =>
  entorno.withSecurityRulesDisabled((contexto) =>
    setDoc(ref(contexto.firestore()), incidencia({ creadaEn: new Date(), ...extra })),
  );

test('conductor reporta una incidencia', () => assertSucceeds(setDoc(ref(conductor()), incidencia())));

test('guía reporta una incidencia', () =>
  assertSucceeds(setDoc(ref(guia()), incidencia({ uid: 'guia1', rol: 'guia', nombre: 'Alfredini Pastelini' }))));

test('no se puede reportar con el uid de otro', () => assertFails(setDoc(ref(guia()), incidencia())));

test('el nombre debe ser el del perfil', () =>
  assertFails(setDoc(ref(conductor()), incidencia({ nombre: 'Otra Persona' }))));

test('el rol debe ser el del perfil', () =>
  assertFails(setDoc(ref(conductor()), incidencia({ rol: 'guia' }))));

test('el administrador no reporta', () =>
  assertFails(setDoc(ref(admin()), incidencia({ uid: 'admin1', rol: 'administrador', nombre: '' }))));

test('tipo desconocido denegado', () =>
  assertFails(setDoc(ref(conductor()), incidencia({ tipo: 'me_da_lata' }))));

test('"otro" exige detalle', async () => {
  await assertFails(setDoc(ref(conductor()), incidencia({ tipo: 'otro', detalle: '   ' })));
  await assertSucceeds(setDoc(ref(conductor()), incidencia({ tipo: 'otro', detalle: 'Se cortó el camino' })));
});

test('detalle de más de 500 caracteres denegado', () =>
  assertFails(setDoc(ref(conductor()), incidencia({ detalle: 'x'.repeat(501) }))));

test('no nace resuelta', () => assertFails(setDoc(ref(conductor()), incidencia({ resuelta: true }))));

test('la hora debe ser la del servidor', () =>
  assertFails(setDoc(ref(conductor()), incidencia({ creadaEn: new Date(0) }))));

test('no se aceptan campos extra', () =>
  assertFails(setDoc(ref(conductor()), incidencia({ tarifa: 1000 }))));

test('el administrador marca una incidencia como resuelta', async () => {
  await sembrar();
  await assertSucceeds(updateDoc(ref(admin()), resolucion()));
});

test('quien reportó no puede resolverla ni cambiarla', async () => {
  await sembrar();
  await assertFails(updateDoc(ref(conductor()), resolucion({ resueltaPor: 'conductor1' })));
  await assertFails(updateDoc(ref(conductor()), { detalle: 'Otro texto' }));
});

test('al resolver no se puede cambiar el contenido', async () => {
  await sembrar();
  await assertFails(updateDoc(ref(admin()), resolucion({ detalle: 'Otro texto' })));
});

test('al resolver se registra quién lo hizo', async () => {
  await sembrar();
  await assertFails(updateDoc(ref(admin()), resolucion({ resueltaPor: 'conductor1' })));
});

test('una incidencia resuelta no se vuelve a resolver', async () => {
  await sembrar({ resuelta: true });
  await assertFails(updateDoc(ref(admin()), resolucion()));
});

const respuesta = (extra = {}) => ({
  enGestion: true,
  respuesta: 'Va un reemplazo en camino',
  atendidaEn: serverTimestamp(),
  atendidaPor: 'admin1',
  ...extra,
});

test('el administrador responde y puede actualizar la respuesta', async () => {
  await sembrar();
  await assertSucceeds(updateDoc(ref(admin()), respuesta()));
  await assertSucceeds(updateDoc(ref(admin()), respuesta({ respuesta: 'Llega en 20 minutos' })));
});

test('quien reportó no puede responder', async () => {
  await sembrar();
  await assertFails(updateDoc(ref(conductor()), respuesta({ atendidaPor: 'conductor1' })));
});

test('la respuesta no puede ir vacía ni ser muy larga', async () => {
  await sembrar();
  await assertFails(updateDoc(ref(admin()), respuesta({ respuesta: '   ' })));
  await assertFails(updateDoc(ref(admin()), respuesta({ respuesta: 'x'.repeat(501) })));
});

test('al responder no se puede cambiar el contenido ni cerrarla', async () => {
  await sembrar();
  await assertFails(updateDoc(ref(admin()), respuesta({ detalle: 'Otro texto' })));
  await assertFails(updateDoc(ref(admin()), respuesta({ resuelta: true })));
});

test('una incidencia resuelta no se puede responder', async () => {
  await sembrar({ resuelta: true });
  await assertFails(updateDoc(ref(admin()), respuesta()));
});

test('se puede resolver con un mensaje', async () => {
  await sembrar();
  await assertSucceeds(updateDoc(ref(admin()), resolucion({ respuesta: 'Se cambió el vehículo' })));
});

test('el mensaje al resolver no puede ser muy largo', async () => {
  await sembrar();
  await assertFails(updateDoc(ref(admin()), resolucion({ respuesta: 'x'.repeat(501) })));
});

test('nadie puede borrar una incidencia', async () => {
  await sembrar();
  await assertFails(deleteDoc(ref(conductor())));
  await assertFails(deleteDoc(ref(admin())));
});

test('cada uno lee las suyas y el administrador todas', async () => {
  await sembrar();
  await assertSucceeds(getDoc(ref(conductor())));
  await assertSucceeds(getDoc(ref(admin())));
  await assertFails(getDoc(ref(guia())));
});

test('las consultas de la app están permitidas solo a quien corresponde', async () => {
  const propias = (db, uid) => getDocs(query(collection(db, 'incidencias'), where('uid', '==', uid)));
  const desde = (db) => getDocs(query(collection(db, 'incidencias'), where('fecha', '>=', '2026-10-26')));
  await assertSucceeds(propias(conductor(), 'conductor1'));
  await assertFails(propias(guia(), 'conductor1'));
  await assertSucceeds(desde(admin()));
  await assertFails(desde(conductor()));
});

test('la incidencia se guarda con ubicación o sin ella', async () => {
  await assertSucceeds(setDoc(ref(conductor(), 'i1'), incidencia({ lat: -41.3, lon: -72.9 })));
  await assertSucceeds(setDoc(ref(conductor(), 'i2'), incidencia()));
});

test('la ubicación de la incidencia lleva las dos coordenadas y son números', async () => {
  await assertFails(setDoc(ref(conductor(), 'i1'), incidencia({ lat: -41.3 })));
  await assertFails(setDoc(ref(conductor(), 'i2'), incidencia({ lon: -72.9 })));
  await assertFails(setDoc(ref(conductor(), 'i3'), incidencia({ lat: '-41.3', lon: -72.9 })));
  await assertFails(setDoc(ref(conductor(), 'i4'), incidencia({ lat: -41.3, lon: '-72.9' })));
});
