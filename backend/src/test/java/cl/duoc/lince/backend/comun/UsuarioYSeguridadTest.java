package cl.duoc.lince.backend.comun;

import cl.duoc.lince.backend.config.SeguridadConfigTestAcceso;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Lectura de los claims del token de Firebase: rol e IDs de la API. */
class UsuarioYSeguridadTest {

    static Jwt token(String rol, Object transportistaId) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "none").subject("uid-1")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (rol != null) b.claim("rol", rol);
        if (transportistaId != null) b.claim("transportistaId", transportistaId);
        return b.build();
    }

    @Test
    void leeLosIdsComoNumeroOComoTexto() {
        assertThat(UsuarioActual.desde(token("conductor", 37)).transportistaId()).isEqualTo(37L);
        assertThat(UsuarioActual.desde(token("conductor", "43")).transportistaId()).isEqualTo(43L);
    }

    @Test
    void unIdInvalidoOVacioQuedaNulo() {
        assertThat(UsuarioActual.desde(token("conductor", "abc")).transportistaId()).isNull();
        assertThat(UsuarioActual.desde(token("conductor", " ")).transportistaId()).isNull();
        assertThat(UsuarioActual.desde(token("conductor", null)).guiaId()).isNull();
    }

    @Test
    void reconoceAlAdministradorSinImportarMayusculas() {
        assertThat(UsuarioActual.desde(token("Administrador", null)).esAdministrador()).isTrue();
        assertThat(UsuarioActual.desde(token("guia", null)).esAdministrador()).isFalse();
    }

    @Test
    void elClaimRolSeConvierteEnPermiso() {
        assertThat(SeguridadConfigTestAcceso.permisos(token(" guia ", null)))
                .extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_GUIA");
    }

    @Test
    void sinClaimRolNoHayPermisos() {
        assertThat(SeguridadConfigTestAcceso.permisos(token(null, null))).isEmpty();
        assertThat(SeguridadConfigTestAcceso.permisos(token("", null))).isEmpty();
    }
}
