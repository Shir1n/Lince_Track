package cl.duoc.lince.backend.hito;

import java.time.LocalDate;
import java.util.List;

/** Respuesta de GET /api/hitos/resumen: el total y una fila por persona (la menos puntual primero). */
public record ResumenHitos(LocalDate desde, LocalDate hasta, ResumenRecurso total, List<ResumenRecurso> recursos) {}
