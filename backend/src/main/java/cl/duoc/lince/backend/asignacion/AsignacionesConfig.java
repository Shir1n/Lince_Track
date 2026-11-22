package cl.duoc.lince.backend.asignacion;

import cl.duoc.lince.backend.config.LinceProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Conecta con Firestore si hay cuenta de servicio (lince.firebase.credenciales o GOOGLE_APPLICATION_CREDENTIALS).
 * Con el valor "nube" usa la identidad del servidor de Google (Cloud Run), sin archivo de clave.
 * Sin ella el backend arranca igual, pero no ve los reemplazos y lo avisa en el log.
 */
@Configuration
public class AsignacionesConfig {

    private static final Logger log = LoggerFactory.getLogger(AsignacionesConfig.class);
    private static final String NOMBRE_APP = "lince-backend";
    /** Valor de lince.firebase.credenciales para usar la identidad de Cloud Run. */
    static final String IDENTIDAD_NUBE = "nube";

    @Bean
    AsignacionesClient asignaciones(LinceProperties propiedades) {
        String ruta = propiedades.firebase().credenciales();
        if (ruta == null || ruta.isBlank()) {
            log.warn("Sin cuenta de servicio de Firebase: los reemplazos (asignaciones) no se tendrán en cuenta.");
            return new SinAsignaciones();
        }
        try {
            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(a -> a.getName().equals(NOMBRE_APP))
                    .findFirst()
                    .orElse(null);
            if (app == null) {
                FirebaseOptions opciones = FirebaseOptions.builder()
                        .setCredentials(credenciales(ruta.trim()))
                        .setProjectId(propiedades.firebase().proyecto())
                        .build();
                app = FirebaseApp.initializeApp(opciones, NOMBRE_APP);
            }
            log.info("Reemplazos activos: se leen desde Firestore ({}).", propiedades.firebase().proyecto());
            return new FirestoreAsignaciones(FirestoreClient.getFirestore(app));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la cuenta de servicio de Firebase en " + ruta, e);
        }
    }

    private static GoogleCredentials credenciales(String ruta) throws IOException {
        if (IDENTIDAD_NUBE.equalsIgnoreCase(ruta)) {
            return GoogleCredentials.getApplicationDefault();
        }
        try (InputStream archivo = new FileInputStream(ruta)) {
            return GoogleCredentials.fromStream(archivo);
        }
    }
}
