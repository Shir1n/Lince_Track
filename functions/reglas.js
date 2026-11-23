// Qué notificación enviar y a quién, según lo que cambió en Firestore. Sin Firebase: se prueba sola
// (test/reglas.test.js). index.js lee los datos, llama a estas funciones y envía con Firebase Cloud Messaging.

export const TIPOS_INCIDENCIA = {
  atraso: 'Atraso',
  falla_mecanica: 'Falla mecánica',
  pasajeros: 'Problema con pasajeros',
  camino: 'Camino o clima',
  otro: 'Otro',
};

export const MOTIVOS_RECHAZO = {
  falla_mecanica: 'falla mecánica',
  fuerza_mayor: 'fuerza mayor',
  cruce_horario: 'cruce de horario',
  otro: 'otro motivo',
};

/** Largo máximo del texto: en la notificación se leen una o dos líneas. */
export const MAX_TEXTO = 180;

export function recortar(texto, max = MAX_TEXTO) {
  const limpio = (texto ?? '').trim();
  return limpio.length > max ? `${limpio.slice(0, max - 1).trimEnd()}…` : limpio;
}

/** "2026-11-22" → "22-11" (así se muestran las fechas en Chile). */
export function fechaCorta(iso) {
  const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso ?? '');
  return partes ? `${partes[3]}-${partes[2]}` : (iso ?? '');
}

/** asignaciones/{servicioId}: avisa a la persona de reemplazo, solo si cambió. */
export function avisoAsignacion(antes, despues) {
  if (!despues?.uid || !despues.servicioId) return null;
  if (antes?.uid === despues.uid) return null;
  return {
    tipo: 'asignacion',
    servicioId: String(despues.servicioId),
    titulo: 'Nuevo servicio asignado',
    texto: `Coordinación te asignó el servicio ${despues.servicioId} para el ${fechaCorta(despues.fecha)}. Ábrelo para aceptarlo.`,
  };
}

/** incidencias/{id} nueva: avisa a los administradores. */
export function avisoIncidenciaNueva(incidencia) {
  if (!incidencia?.servicioId) return null;
  const tipo = TIPOS_INCIDENCIA[incidencia.tipo] ?? 'Incidencia';
  const quien = [incidencia.titulo, incidencia.nombre].filter(Boolean).join(' · ');
  const detalle = (incidencia.detalle ?? '').trim();
  return {
    tipo: 'incidencia',
    servicioId: String(incidencia.servicioId),
    titulo: `Incidencia: ${tipo}`,
    texto: recortar(detalle ? `${quien}: ${detalle}` : quien),
  };
}

/** incidencias/{id} actualizada: avisa a quien reportó si Coordinación respondió o la resolvió. */
export function avisoIncidenciaAtendida(antes, despues) {
  if (!antes || !despues?.uid || !despues.servicioId) return null;
  const respuesta = (despues.respuesta ?? '').trim();
  const servicioId = String(despues.servicioId);
  if (!antes.resuelta && despues.resuelta) {
    return {
      tipo: 'incidencia_resuelta',
      servicioId,
      titulo: 'Incidencia resuelta',
      texto: recortar(respuesta
        ? `Coordinación: ${respuesta}`
        : `Coordinación cerró tu incidencia en ${despues.titulo || `el servicio ${servicioId}`}.`),
    };
  }
  if (respuesta && respuesta !== (antes.respuesta ?? '').trim()) {
    return {
      tipo: 'incidencia_respuesta',
      servicioId,
      titulo: 'Coordinación respondió',
      texto: recortar(respuesta),
    };
  }
  return null;
}

/** respuestas/{uid}_{servicioId}: solo interesa el paso a rechazado (un rechazo es definitivo). */
export function esRechazoNuevo(antes, despues) {
  return despues?.estado === 'rechazado' && antes?.estado !== 'rechazado'
    && Boolean(despues.uid) && Boolean(despues.servicioId);
}

/** Aviso a los administradores de un rechazo; [nombre] sale de usuarios/{uid}. */
export function avisoRechazo(respuesta, nombre) {
  const quien = (nombre ?? '').trim() || (respuesta.rol === 'guia' ? 'Un guía' : 'Un conductor');
  const motivo = MOTIVOS_RECHAZO[respuesta.motivo] ?? 'sin motivo';
  return {
    tipo: 'rechazo',
    servicioId: String(respuesta.servicioId),
    titulo: 'Servicio rechazado',
    texto: recortar(`${quien} rechazó el servicio ${respuesta.servicioId} (${motivo}). Hay que reasignarlo.`),
  };
}

/**
 * Mensaje de Firebase Cloud Messaging. Con la app cerrada, Android lo muestra en el canal "servicios";
 * la etiqueta es la misma de NotificadorLocal, así que si la app estaba viva y ya avisó, se reemplazan.
 * Al tocarlo, la app recibe `servicioId` y abre ese servicio.
 */
export function mensaje(token, aviso) {
  // Los avisos de privacidad no son de un servicio: van sin servicioId y con su propia etiqueta.
  const data = aviso.servicioId ? { servicioId: aviso.servicioId, tipo: aviso.tipo } : { tipo: aviso.tipo };
  return {
    token,
    notification: { title: aviso.titulo, body: aviso.texto },
    data,
    android: {
      priority: 'high',
      notification: { channelId: 'servicios', tag: aviso.etiqueta ?? `servicio|${aviso.servicioId}` },
    },
  };
}

// ---------- Privacidad (Ley 21.719) ----------

/** solicitudesPrivacidad/{uid}: avisa a Coordinación cuando alguien pide (o vuelve a pedir) la eliminación. */
export function avisoSolicitudPrivacidad(antes, despues) {
  if (despues?.estado !== 'pendiente' || antes?.estado === 'pendiente' || !despues.uid) return null;
  const quien = (despues.nombre ?? '').trim() || (despues.rol === 'guia' ? 'Un guía' : 'Un conductor');
  const motivo = (despues.motivo ?? '').trim();
  return {
    tipo: 'privacidad_solicitud',
    servicioId: '',
    etiqueta: `privacidad|${despues.uid}`,
    titulo: 'Solicitud de eliminación de datos',
    texto: recortar(motivo ? `${quien} pidió eliminar sus datos: ${motivo}` : `${quien} pidió eliminar sus datos personales.`),
  };
}

/** Avisa a la persona si Coordinación rechazó su solicitud (si se aprueba, su cuenta se borra). */
export function avisoRechazoPrivacidad(antes, despues) {
  if (antes?.estado !== 'pendiente' || despues?.estado !== 'rechazada' || !despues.uid) return null;
  return {
    tipo: 'privacidad_rechazo',
    servicioId: '',
    etiqueta: `privacidad|${despues.uid}`,
    titulo: 'Solicitud de eliminación rechazada',
    texto: recortar(`Coordinación: ${(despues.respuesta ?? '').trim()}`),
  };
}

/** true cuando Coordinación acaba de aprobar la solicitud: hay que borrar los datos. */
export function esAprobacionPrivacidad(antes, despues) {
  return antes?.estado === 'pendiente' && despues?.estado === 'aprobada' && Boolean(despues.uid);
}

/**
 * Lo que queda de la solicitud tras borrar los datos: fechas y cantidades, sin nombre, motivo ni respuesta.
 * Sirve como respaldo de que se cumplió.
 */
export function registroCompletado(solicitud, borrados, ahora) {
  return {
    uid: solicitud.uid,
    estado: 'completada',
    creadaEn: solicitud.creadaEn ?? null,
    atendidaEn: solicitud.atendidaEn ?? null,
    completadaEn: ahora,
    borrados,
  };
}

/** Colecciones de Firestore donde una persona tiene documentos con su campo "uid". */
export const COLECCIONES_CON_UID = ['checkins', 'incidencias', 'respuestas', 'estadosServicio', 'asignaciones'];

/** Carpetas de Storage con fotos de la persona (perfil y check-in). */
export const CARPETAS_STORAGE = (uid) => [`perfil/${uid}/`, `checkin/${uid}/`];

/** Códigos de FCM que indican que el teléfono ya no usa ese token (desinstaló o cerró sesión). */
export function tokenInvalido(codigo) {
  return codigo === 'messaging/registration-token-not-registered'
    || codigo === 'messaging/invalid-registration-token';
}
