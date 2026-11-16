package cl.duoc.lince.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** La hora del servidor es la evidencia de un hito. Como bean, los tests pueden fijarla. */
@Configuration
public class RelojConfig {

    @Bean
    Clock reloj(LinceProperties propiedades) {
        return Clock.system(propiedades.reglas().zona());
    }
}
