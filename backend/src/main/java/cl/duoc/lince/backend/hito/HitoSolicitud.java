package cl.duoc.lince.backend.hito;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Lo que manda la app al marcar. La hora no viene del teléfono: la pone el servidor. */
public record HitoSolicitud(
        @NotNull(message = "es obligatorio") Long servicioId,
        @NotNull(message = "es obligatorio") TipoHito tipo,
        @NotNull(message = "es obligatorio")
        @DecimalMin(value = "-90", message = "debe estar entre -90 y 90")
        @DecimalMax(value = "90", message = "debe estar entre -90 y 90") Double lat,
        @NotNull(message = "es obligatorio")
        @DecimalMin(value = "-180", message = "debe estar entre -180 y 180")
        @DecimalMax(value = "180", message = "debe estar entre -180 y 180") Double lon,
        @NotNull(message = "es obligatorio")
        @PositiveOrZero(message = "no puede ser negativa") Double precisionMetros,
        @NotBlank(message = "es obligatoria")
        @Size(max = 64, message = "debe tener a lo más 64 caracteres") String claveIdempotencia
) {}
