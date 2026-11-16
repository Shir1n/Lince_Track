package cl.duoc.lince.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * El backend no tiene login propio: acepta el token (JWT) que Firebase Auth entrega a la app.
 * La app lo manda en "Authorization: Bearer ..." y aquí se valida la firma con las claves públicas de Google,
 * el emisor y la audiencia (nuestro proyecto de Firebase).
 *
 * El rol sale del claim "rol" (administrador | conductor | guia), que copia scripts/sincronizar-claims.mjs
 * desde usuarios/{uid} en Firestore. Se convierte en ROLE_ADMINISTRADOR, ROLE_CONDUCTOR o ROLE_GUIA.
 */
@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

    /** Claves públicas con que Google firma los tokens de Firebase Auth. */
    static final String CLAVES_FIREBASE =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorRoles())));
        return http.build();
    }

    @Bean
    JwtDecoder decodificadorFirebase(LinceProperties propiedades) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(CLAVES_FIREBASE).build();
        String proyecto = propiedades.firebase().proyecto();
        OAuth2TokenValidator<Jwt> audiencia = jwt -> jwt.getAudience().contains(proyecto)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audiencia incorrecta", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(propiedades.firebase().emisor()), audiencia));
        return decoder;
    }

    static Converter<Jwt, AbstractAuthenticationToken> convertidorRoles() {
        return jwt -> {
            String rol = jwt.getClaimAsString("rol");
            List<SimpleGrantedAuthority> permisos = rol == null || rol.isBlank()
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + rol.trim().toUpperCase()));
            return new JwtAuthenticationToken(jwt, permisos, jwt.getSubject());
        };
    }
}
