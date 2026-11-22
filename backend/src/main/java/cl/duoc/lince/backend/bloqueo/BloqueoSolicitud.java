package cl.duoc.lince.backend.bloqueo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BloqueoSolicitud(
        @NotNull(message = "es obligatoria") LocalDate fecha,
        // Obligatorio: si falta o está en blanco, BloqueoService responde 400 MOTIVO_OBLIGATORIO.
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Trámite personal")
        @Size(max = 200, message = "debe tener a lo más 200 caracteres") String motivo
) {}
