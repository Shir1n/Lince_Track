package cl.duoc.lince.backend.hito;

import cl.duoc.lince.backend.comun.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hitos")
@Tag(name = "Hitos", description = "Check-in, check-out y posición puntual, validados en el servidor")
public class HitoController {

    private final HitoService servicio;

    public HitoController(HitoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CONDUCTOR', 'GUIA')")
    @Operation(summary = "Registra un hito",
            description = "Responde 201 con el resultado (VERIFICADO, FUERA_DE_ZONA, TARDE, GPS_IMPRECISO, "
                    + "SIN_GEOCERCA o REGISTRADO). Si la clave de idempotencia ya existe, responde 200 con el mismo hito.")
    ResponseEntity<Hito> registrar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody HitoSolicitud solicitud) {
        HitoService.Registro r = servicio.registrar(UsuarioActual.desde(jwt), solicitud);
        return ResponseEntity.status(r.yaExistia() ? HttpStatus.OK : HttpStatus.CREATED).body(r.hito());
    }

    @GetMapping
    @Operation(summary = "Hitos de un servicio (el administrador ve los de todos; el resto, solo los suyos)")
    List<Hito> listar(@AuthenticationPrincipal Jwt jwt, @RequestParam long servicioId) {
        return servicio.listar(UsuarioActual.desde(jwt), servicioId);
    }

    @GetMapping("/ultima-posicion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Último hito reportado en un servicio (administrador)")
    Hito ultimaPosicion(@RequestParam long servicioId) {
        return servicio.ultimaPosicion(servicioId);
    }
}
