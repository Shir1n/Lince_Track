package cl.duoc.lince.backend.privacidad;

import cl.duoc.lince.backend.bloqueo.Bloqueo;
import cl.duoc.lince.backend.hito.Hito;

import java.util.List;

/** Lo que el backend guarda de una persona, para descargarlo (acceso y portabilidad). */
public record DatosPersona(String uid, List<Hito> hitos, List<Bloqueo> bloqueos) {}
