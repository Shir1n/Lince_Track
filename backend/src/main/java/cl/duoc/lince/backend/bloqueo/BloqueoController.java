package cl.duoc.lince.backend.bloqueo;

import cl.duoc.lince.backend.comun.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bloqueos")
@Tag(name = "Disponibilidad", description = "Días bloqueados: con 7 días de anticipación y nunca sobre un servicio")
public class BloqueoController {

    private final BloqueoService servicio;

    public BloqueoController(BloqueoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Bloqueos (el administrador ve los de todos; el resto, solo los suyos)")
    List<Bloqueo> listar(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listar(UsuarioActual.desde(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CONDUCTOR', 'GUIA')")
    @Operation(summary = "Bloquea un día")
    Bloqueo crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BloqueoSolicitud solicitud) {
        return servicio.crear(UsuarioActual.desde(jwt), solicitud);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quita un bloqueo (el dueño o el administrador)")
    void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        servicio.eliminar(UsuarioActual.desde(jwt), id);
    }
}
