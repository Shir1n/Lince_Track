package cl.duoc.lince.backend.comun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Todos los errores salen con el mismo formato que la API de Southbound:
 * { "error": { "code", "message", "details", "requestId" } }. El requestId también queda en el log.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    public record Detalle(String field, String message) {}

    public record CuerpoError(String code, String message, List<Detalle> details, String requestId) {}

    @ExceptionHandler(ErrorNegocio.class)
    ResponseEntity<Map<String, CuerpoError>> negocio(ErrorNegocio e) {
        return responder(e.estado(), e.codigo(), e.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, CuerpoError>> validacion(MethodArgumentNotValidException e) {
        List<Detalle> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new Detalle(f.getField(), f.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Parámetros inválidos.", detalles);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    ResponseEntity<Map<String, CuerpoError>> sinPermiso(RuntimeException e) {
        return responder(HttpStatus.FORBIDDEN, "FORBIDDEN", "Tu rol no permite esta acción.", List.of());
    }

    @ExceptionHandler(ErrorLince.class)
    ResponseEntity<Map<String, CuerpoError>> lince(ErrorLince e) {
        return responder(HttpStatus.BAD_GATEWAY, "LINCE_API_ERROR", e.getMessage(), List.of());
    }

    private ResponseEntity<Map<String, CuerpoError>> responder(
            HttpStatus estado, String codigo, String mensaje, List<Detalle> detalles) {
        String requestId = UUID.randomUUID().toString();
        log.info("{} {} {} requestId={}", estado.value(), codigo, mensaje, requestId);
        return ResponseEntity.status(estado)
                .body(Map.of("error", new CuerpoError(codigo, mensaje, detalles, requestId)));
    }
}
