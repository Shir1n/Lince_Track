package cl.duoc.lince.backend.bloqueo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BloqueoSolicitud(
        @NotNull(message = "es obligatoria") LocalDate fecha,
        @Size(max = 200, message = "debe tener a lo más 200 caracteres") String motivo
) {}
