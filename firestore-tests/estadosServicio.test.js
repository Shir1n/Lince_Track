// Reglas de firestore.rules para la colección "estadosServicio". Corren en el emulador,
// con un proyecto demo: no tocan los datos reales de Firebase.
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { collection, deleteDoc, doc, getDoc, getDocs, query, serverTimestamp, setDoc, where } from 'firebase/firestore';

let entorno;

const conductor = () => entorno.authenticatedContext('conductor1').firestore();
const guia = () => entorno.authenticatedContext('guia1').firestore();
const admin = () => entorno.authenticatedContext('admin1').firestore();
const ajeno = () => entorno.authenticatedContext('sinperfil').firestore();

const estado = (extra = {}) => ({
  uid: 'conductor1',
  servicioId: '8941',
  rol: 'conductor',
  nombre: 'Pedro Rojas',
  estado: 'aceptado',
  fecha: '2026-11-02',
  actualizadoEn: serverTimestamp(),
  ...extra,
});

const ref = (db, id = '8941') => doc(db, 'estadosServicio', id);

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
    await setDoc(doc(db, 'usuarios/conductor1'), { rol: 'conductor', nombre: 'Pedro Rojas' });
    await setDoc(doc(db, 'usuarios/guia1'), { rol: 'guia', nombre: 'Alfredini Pastelini' });
    await setDoc(doc(db, 'usuarios/conductor2'), { rol: 'conductor' });
    await setDoc(doc(db, 'usuarios/admin1'), { rol: 'administrador' });
  });
});

// Deja guardado un estado sin pasar por las reglas.
const sembrar = (datos) =>
  entorno.withSecurityRulesDisabled((contexto) => setDoc(ref(contexto.firestore()), datos));

test('conductor publica que aceptó', () => assertSucceeds(setDoc(ref(conductor()), estado())));

test('guía publica que aceptó', () =>
  assertSucceeds(
    setDoc(ref(guia(), '8942'), estado({ uid: 'guia1', rol: 'guia', nombre: 'Alfredini Pastelini', servicioId: '8942' })),
  ));

test('no se puede publicar con un nombre distinto al del perfil', () =>
  assertFails(setDoc(ref(conductor()), estado({ nombre: 'Otra Persona' }))));

test('el nombre es obligatorio', async () => {
  const { nombre, ...sinNombre } = estado();
  await assertFails(setDoc(ref(conductor()), sinNombre));
});

test('una cuenta sin nombre publica el nombre vacío', () =>
  assertSucceeds(
    setDoc(
      ref(entorno.authenticatedContext('conductor2').firestore()),
      estado({ uid: 'conductor2', nombre: '' }),
    ),
  ));

test('conductor publica que rechazó', () =>
  assertSucceeds(setDoc(ref(conductor()), estado({ estado: 'rechazado' }))));

test('no se puede publicar con el uid de otro', () => assertFails(setDoc(ref(guia()), estado())));

test('no se puede usar un rol distinto al propio', () =>
  assertFails(setDoc(ref(conductor()), estado({ rol: 'guia' }))));

test('el id del documento debe calzar con el servicio', () =>
  assertFails(setDoc(ref(conductor(), '9999'), estado())));

test('el administrador no publica estados', () =>
  assertFails(setDoc(ref(admin()), estado({ uid: 'admin1', rol: 'administrador' }))));

test('el motivo del rechazo no se comparte', () =>
  assertFails(setDoc(ref(conductor()), estado({ estado: 'rechazado', motivo: 'otro' }))));

test('estado desconocido denegado', () =>
  assertFails(setDoc(ref(conductor()), estado({ estado: 'quizas' }))));

test('la fecha es obligatoria', async () => {
  const { fecha, ...sinFecha } = estado();
  await assertFails(setDoc(ref(conductor()), sinFecha));
});

test('la hora debe ser la del servidor', () =>
  assertFails(setDoc(ref(conductor()), estado({ actualizadoEn: new Date(0) }))));

test('quien aceptó puede rechazar después', async () => {
  await sembrar(estado({ actualizadoEn: new Date() }));
  await assertSucceeds(setDoc(ref(conductor()), estado({ estado: 'rechazado' })));
});

test('otra persona no puede cambiar el estado publicado', async () => {
  await sembrar(estado({ actualizadoEn: new Date() }));
  await assertFails(
    setDoc(ref(guia()), estado({ uid: 'guia1', rol: 'guia', nombre: 'Alfredini Pastelini', estado: 'rechazado' })),
  );
});

test('un rechazo es definitivo', async () => {
  await sembrar(estado({ estado: 'rechazado', actualizadoEn: new Date() }));
  await assertFails(setDoc(ref(conductor()), estado()));
});

test('solo el administrador puede borrar un estado', async () => {
  await sembrar(estado({ actualizadoEn: new Date() }));
  await assertFails(deleteDoc(ref(conductor())));
  await assertSucceeds(deleteDoc(ref(admin())));
});

test('si se reasignó, solo responde el reemplazo', async () => {
  await entorno.withSecurityRulesDisabled((contexto) =>
    setDoc(doc(contexto.firestore(), 'asignaciones/8941'), { uid: 'conductor2' }),
  );
  await assertFails(setDoc(ref(conductor()), estado()));
  await assertSucceeds(
    setDoc(
      ref(entorno.authenticatedContext('conductor2').firestore()),
      estado({ uid: 'conductor2', nombre: '' }),
    ),
  );
});

test('conductores, guías y administrador leen los estados', async () => {
  await sembrar(estado({ actualizadoEn: new Date() }));
  await assertSucceeds(getDoc(ref(conductor())));
  await assertSucceeds(getDoc(ref(guia())));
  await assertSucceeds(getDoc(ref(admin())));
});

test('la consulta de la app por fecha está permitida', () =>
  assertSucceeds(getDocs(query(collection(guia(), 'estadosServicio'), where('fecha', '>=', '2026-10-26')))));

test('sin perfil o sin sesión no se leen', async () => {
  await sembrar(estado({ actualizadoEn: new Date() }));
  await assertFails(getDoc(ref(ajeno())));
  await assertFails(getDoc(ref(entorno.unauthenticatedContext().firestore())));
});
