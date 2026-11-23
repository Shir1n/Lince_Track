package cl.duoc.lince.backend.privacidad;

import cl.duoc.lince.backend.comun.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Derechos de la Ley 21.719 sobre lo que guarda el backend (hitos con coordenadas y bloqueos de disponibilidad):
 * acceso y portabilidad (cada persona descarga lo suyo) y supresión (Coordinación borra al aprobar una solicitud).
 * Lo que está en Firestore lo borra la Cloud Function al aprobar.
 */
@RestController
@RequestMapping("/api/privacidad")
@Tag(name = "Privacidad", description = "Acceso, portabilidad y eliminación de los datos de una persona (Ley 21.719)")
public class PrivacidadController {

    private final PrivacidadService servicio;

    public PrivacidadController(PrivacidadService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/mis-datos")
    @Operation(summary = "Todo lo que el backend guarda de la persona del token (hitos y bloqueos)")
    DatosPersona misDatos(@AuthenticationPrincipal Jwt jwt) {
        return servicio.datosDe(UsuarioActual.desde(jwt).uid());
    }

    @DeleteMapping("/{uid}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Borra los hitos y bloqueos de una persona (administrador, al aprobar su solicitud)")
    DatosBorrados borrar(@PathVariable String uid) {
        return servicio.borrar(uid);
    }
}
