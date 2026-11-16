package cl.duoc.lince.backend.comun;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Quién hace la solicitud, leído del token de Firebase.
 * transportistaId y guiaId son los IDs de la persona en la API de Southbound (claims copiados desde usuarios/{uid}).
 */
public record UsuarioActual(String uid, String rol, Long transportistaId, Long guiaId) {

    public static UsuarioActual desde(Jwt jwt) {
        return new UsuarioActual(
                jwt.getSubject(),
                jwt.getClaimAsString("rol"),
                comoLong(jwt.getClaim("transportistaId")),
                comoLong(jwt.getClaim("guiaId")));
    }

    public boolean esAdministrador() {
        return "administrador".equalsIgnoreCase(rol);
    }

    private static Long comoLong(Object valor) {
        if (valor instanceof Number n) return n.longValue();
        if (valor instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
