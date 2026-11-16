package cl.duoc.lince.backend.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

/** Da a los tests de otros paquetes acceso al convertidor de roles (que es interno de SeguridadConfig). */
public final class SeguridadConfigTestAcceso {

    private SeguridadConfigTestAcceso() {
    }

    public static Collection<GrantedAuthority> permisos(Jwt jwt) {
        return SeguridadConfig.convertidorRoles().convert(jwt).getAuthorities();
    }
}
