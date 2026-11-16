package cl.duoc.lince.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI en /swagger-ui.html. El botón Authorize recibe el token de Firebase (sin "Bearer "). */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI documentacion() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lince Track - Backend")
                        .version("0.1.0")
                        .description("Geocercas, hitos (check-in y check-out) y bloqueos de disponibilidad. "
                                + "La API de Southbound es de solo lectura: lo que la app registra se guarda aquí."))
                .components(new Components().addSecuritySchemes("firebase", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token de Firebase Auth (FirebaseUser.getIdToken)")))
                .addSecurityItem(new SecurityRequirement().addList("firebase"));
    }
}
