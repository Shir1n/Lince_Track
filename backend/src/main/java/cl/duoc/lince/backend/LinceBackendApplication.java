package cl.duoc.lince.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Microservicio de Lince Track: guarda lo que la API de Southbound (solo lectura) no puede guardar.
 * Geocercas de hoteles, hitos (check-in y check-out validados) y bloqueos de disponibilidad.
 * El login, las respuestas, los reemplazos y las incidencias siguen en Firebase.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LinceBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinceBackendApplication.class, args);
    }
}
