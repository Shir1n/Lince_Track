package cl.duoc.lince.backend.comun;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Para comprobar desde el celular que el backend está arriba (no pide token). */
@RestController
@Tag(name = "Salud")
public class SaludController {

    @GetMapping("/health")
    @Operation(summary = "Estado del servicio", security = {})
    Map<String, String> salud() {
        return Map.of("status", "ok");
    }
}
