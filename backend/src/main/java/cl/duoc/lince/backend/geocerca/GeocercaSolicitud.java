package cl.duoc.lince.backend.geocerca;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Datos para crear o editar una geocerca. Sin radio se usa el de application.yml (150 m). */
public record GeocercaSolicitud(
        @NotBlank(message = "es obligatorio") String nombre,
        @DecimalMin(value = "-90", message = "debe estar entre -90 y 90")
        @DecimalMax(value = "90", message = "debe estar entre -90 y 90") double lat,
        @DecimalMin(value = "-180", message = "debe estar entre -180 y 180")
        @DecimalMax(value = "180", message = "debe estar entre -180 y 180") double lon,
        @Min(value = 10, message = "debe ser de al menos 10 m")
        @Max(value = 5000, message = "debe ser de a lo más 5000 m") Integer radioMetros,
        Boolean activa
) {}
