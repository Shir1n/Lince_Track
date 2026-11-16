package cl.duoc.lince.backend.parametro;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parametros")
@Tag(name = "Parámetros", description = "Reglas que coordinación puede cambiar: anticipación, precisión, radio y días")
public class ParametrosController {

    private final ParametrosService servicio;

    public ParametrosController(ParametrosService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Reglas vigentes (la app las usa para validar antes de enviar)")
    Parametros obtener() {
        return servicio.actuales();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Cambia las reglas (administrador). Valen desde la siguiente solicitud.")
    Parametros actualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ParametrosSolicitud solicitud) {
        return servicio.actualizar(solicitud, jwt.getSubject());
    }
}
