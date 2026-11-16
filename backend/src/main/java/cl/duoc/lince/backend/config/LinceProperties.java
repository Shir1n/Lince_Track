package cl.duoc.lince.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;

/** Valores de application.yml bajo "lince". Cambiar una regla aquí no requiere tocar código. */
@ConfigurationProperties(prefix = "lince")
public record LinceProperties(Firebase firebase, Api api, Reglas reglas) {

    /**
     * @param credenciales ruta al JSON de la cuenta de servicio (fuera del repo). Vacía = sin reemplazos.
     */
    public record Firebase(String proyecto, String credenciales) {
        public String emisor() {
            return "https://securetoken.google.com/" + proyecto;
        }
    }

    public record Api(String urlBase, String clave) {
        public boolean tieneClave() {
            return clave != null && !clave.isBlank();
        }
    }

    public record Reglas(
            int radioPorDefectoMetros,
            int anticipacionMinutos,
            int precisionMaximaMetros,
            int diasAnticipacionBloqueo,
            String zonaHoraria
    ) {
        public ZoneId zona() {
            return ZoneId.of(zonaHoraria);
        }
    }
}
