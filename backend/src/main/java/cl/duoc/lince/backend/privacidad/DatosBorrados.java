package cl.duoc.lince.backend.privacidad;

/** Cuántos registros se borraron de una persona (queda como respaldo de que se cumplió la solicitud). */
public record DatosBorrados(String uid, long hitos, long bloqueos) {}
