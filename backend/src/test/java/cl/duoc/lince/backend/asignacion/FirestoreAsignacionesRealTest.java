package cl.duoc.lince.backend.asignacion;

import cl.duoc.lince.backend.config.LinceProperties;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lee el Firestore real (solo lectura). No corre en la suite normal: hay que definir LINCE_PRUEBA_FIRESTORE=1
 * y GOOGLE_APPLICATION_CREDENTIALS con la cuenta de servicio. Sirve para comprobar la conexión.
 */
@EnabledIfEnvironmentVariable(named = "LINCE_PRUEBA_FIRESTORE", matches = "1")
class FirestoreAsignacionesRealTest {

    @Test
    void leeLosReemplazosGuardadosPorLaApp() throws Exception {
        LinceProperties propiedades = new LinceProperties(
                new LinceProperties.Firebase("lince-track-66c55", System.getenv("GOOGLE_APPLICATION_CREDENTIALS")),
                null, null);
        AsignacionesClient cliente = new AsignacionesConfig().asignaciones(propiedades);
        assertThat(cliente.activo()).isTrue();

        List<QueryDocumentSnapshot> documentos = FirestoreClient.getFirestore(FirebaseApp.getInstance("lince-backend"))
                .collection(FirestoreAsignaciones.COLECCION).limit(5).get().get().getDocuments();
        for (QueryDocumentSnapshot doc : documentos) {
            long id = Long.parseLong(doc.getId());
            Optional<Reemplazo> r = cliente.de(id);
            assertThat(r).isPresent();
            assertThat(r.get().uid()).isEqualTo(doc.getString("uid"));

            List<Reemplazo> recibidos = cliente.recibidos(r.get().uid(), LocalDate.parse(r.get().fecha()));
            assertThat(recibidos).extracting(Reemplazo::servicioId).contains(id);
            System.out.println("OK " + id + " → " + r.get().rol() + " " + r.get().fecha());
        }
        assertThat(cliente.de(1)).isEmpty();
    }
}
