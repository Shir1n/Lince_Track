package cl.duoc.lince.backend.parametro;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Nuevos valores de las reglas. Se mandan los cuatro (PUT reemplaza todo). */
public record ParametrosSolicitud(
        @NotNull(message = "es obligatoria")
        @Min(value = 0, message = "no puede ser negativa")
        @Max(value = 240, message = "debe ser de a lo más 240 minutos") Integer anticipacionMinutos,
        @NotNull(message = "es obligatoria")
        @Min(value = 5, message = "debe ser de al menos 5 m")
        @Max(value = 500, message = "debe ser de a lo más 500 m") Integer precisionMaximaMetros,
        @NotNull(message = "es obligatorio")
        @Min(value = 10, message = "debe ser de al menos 10 m")
        @Max(value = 5000, message = "debe ser de a lo más 5000 m") Integer radioPorDefectoMetros,
        @NotNull(message = "es obligatoria")
        @Min(value = 0, message = "no puede ser negativa")
        @Max(value = 60, message = "debe ser de a lo más 60 días") Integer diasAnticipacionBloqueo
) {}
