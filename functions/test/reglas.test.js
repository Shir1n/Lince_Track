// Pruebas de reglas.js (sin Firebase): npm test
import assert from 'node:assert/strict';
import { test } from 'node:test';
import * as reglas from '../reglas.js';

const asignacion = { servicioId: '3008', uid: 'patricio', nombre: 'Patricio Lagos', rol: 'conductor', fecha: '2026-11-22' };
const incidencia = {
  uid: 'carlos', nombre: 'Carlos Morales', servicioId: '3008', titulo: 'Tour Frutillar',
  tipo: 'falla_mecanica', detalle: 'Se pinchó un neumático', resuelta: false,
};

test('fechaCorta y recortar', () => {
  assert.equal(reglas.fechaCorta('2026-11-22'), '22-11');
  assert.equal(reglas.fechaCorta('mañana'), 'mañana');
  assert.equal(reglas.fechaCorta(undefined), '');
  assert.equal(reglas.recortar('  hola  '), 'hola');
  assert.equal(reglas.recortar('abcdef', 4), 'abc…');
  assert.equal(reglas.recortar(null), '');
});

test('una asignación nueva o a otra persona avisa al reemplazo', () => {
  assert.deepEqual(reglas.avisoAsignacion(undefined, asignacion), {
    tipo: 'asignacion',
    servicioId: '3008',
    titulo: 'Nuevo servicio asignado',
    texto: 'Coordinación te asignó el servicio 3008 para el 22-11. Ábrelo para aceptarlo.',
  });
  assert.ok(reglas.avisoAsignacion({ ...asignacion, uid: 'miguel' }, asignacion));
});

test('si la asignación no cambió de persona o se borró, no avisa', () => {
  assert.equal(reglas.avisoAsignacion(asignacion, { ...asignacion, nombre: 'Otro nombre' }), null);
  assert.equal(reglas.avisoAsignacion(asignacion, undefined), null);
  assert.equal(reglas.avisoAsignacion(undefined, { ...asignacion, servicioId: '' }), null);
});

test('una incidencia nueva avisa con el tipo, el servicio, quién y el detalle', () => {
  assert.deepEqual(reglas.avisoIncidenciaNueva(incidencia), {
    tipo: 'incidencia',
    servicioId: '3008',
    titulo: 'Incidencia: Falla mecánica',
    texto: 'Tour Frutillar · Carlos Morales: Se pinchó un neumático',
  });
  const sinDetalle = reglas.avisoIncidenciaNueva({ ...incidencia, detalle: ' ', tipo: 'desconocido' });
  assert.equal(sinDetalle.titulo, 'Incidencia: Incidencia');
  assert.equal(sinDetalle.texto, 'Tour Frutillar · Carlos Morales');
  assert.equal(reglas.avisoIncidenciaNueva(undefined), null);
  assert.ok(reglas.avisoIncidenciaNueva({ ...incidencia, detalle: 'x'.repeat(400) }).texto.length <= reglas.MAX_TEXTO);
});

test('la respuesta de Coordinación avisa a quien reportó', () => {
  const respondida = { ...incidencia, enGestion: true, respuesta: 'Va un reemplazo en camino' };
  assert.deepEqual(reglas.avisoIncidenciaAtendida(incidencia, respondida), {
    tipo: 'incidencia_respuesta',
    servicioId: '3008',
    titulo: 'Coordinación respondió',
    texto: 'Va un reemplazo en camino',
  });
  // Misma respuesta: nada nuevo.
  assert.equal(reglas.avisoIncidenciaAtendida(respondida, { ...respondida }), null);
});

test('resolver la incidencia avisa, con o sin mensaje', () => {
  const resuelta = { ...incidencia, resuelta: true, respuesta: 'Listo' };
  assert.equal(reglas.avisoIncidenciaAtendida(incidencia, resuelta).texto, 'Coordinación: Listo');
  const sinMensaje = reglas.avisoIncidenciaAtendida(incidencia, { ...incidencia, resuelta: true });
  assert.equal(sinMensaje.titulo, 'Incidencia resuelta');
  assert.equal(sinMensaje.texto, 'Coordinación cerró tu incidencia en Tour Frutillar.');
  const sinTitulo = reglas.avisoIncidenciaAtendida(incidencia, { ...incidencia, titulo: '', resuelta: true });
  assert.equal(sinTitulo.texto, 'Coordinación cerró tu incidencia en el servicio 3008.');
  assert.equal(reglas.avisoIncidenciaAtendida(undefined, resuelta), null);
});

test('solo el paso a rechazado avisa a los administradores', () => {
  const rechazo = { uid: 'carlos', servicioId: '3067', rol: 'conductor', estado: 'rechazado', motivo: 'cruce_horario' };
  assert.ok(reglas.esRechazoNuevo(undefined, rechazo));
  assert.ok(reglas.esRechazoNuevo({ ...rechazo, estado: 'aceptado' }, rechazo));
  assert.equal(reglas.esRechazoNuevo(rechazo, rechazo), false);
  assert.equal(reglas.esRechazoNuevo(undefined, { ...rechazo, estado: 'aceptado' }), false);
  assert.equal(reglas.esRechazoNuevo(undefined, undefined), false);

  assert.deepEqual(reglas.avisoRechazo(rechazo, 'Carlos Morales R.'), {
    tipo: 'rechazo',
    servicioId: '3067',
    titulo: 'Servicio rechazado',
    texto: 'Carlos Morales R. rechazó el servicio 3067 (cruce de horario). Hay que reasignarlo.',
  });
  assert.match(reglas.avisoRechazo({ ...rechazo, rol: 'guia', motivo: 'x' }, '').texto, /^Un guía rechazó .*\(sin motivo\)/);
  assert.match(reglas.avisoRechazo(rechazo, undefined).texto, /^Un conductor/);
});

test('el mensaje usa el canal y la etiqueta de la app y lleva el servicio', () => {
  const aviso = reglas.avisoAsignacion(undefined, asignacion);
  assert.deepEqual(reglas.mensaje('tok', aviso), {
    token: 'tok',
    notification: { title: aviso.titulo, body: aviso.texto },
    data: { servicioId: '3008', tipo: 'asignacion' },
    android: { priority: 'high', notification: { channelId: 'servicios', tag: 'servicio|3008' } },
  });
});

test('reconoce los tokens que ya no sirven', () => {
  assert.ok(reglas.tokenInvalido('messaging/registration-token-not-registered'));
  assert.ok(reglas.tokenInvalido('messaging/invalid-registration-token'));
  assert.equal(reglas.tokenInvalido('messaging/internal-error'), false);
});
