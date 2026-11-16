package cl.duoc.lince.backend.asignacion;

/**
 * Documento asignaciones/{servicioId} de Firestore: el administrador envió a otra persona al servicio.
 * Lo escribe la app (AsignacionRepository) y manda sobre la persona que trae la API de Southbound.
 *
 * @param uid   cuenta de Firebase que recibió el servicio
 * @param rol   "conductor" o "guia"
 * @param fecha fecha del servicio (AAAA-MM-DD)
 */
public record Reemplazo(long servicioId, String uid, String rol, String fecha) {}
