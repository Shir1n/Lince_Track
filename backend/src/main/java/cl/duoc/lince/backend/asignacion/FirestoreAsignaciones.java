package cl.duoc.lince.backend.asignacion;

import cl.duoc.lince.backend.comun.ErrorFirestore;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Lee asignaciones/{servicioId} con Firebase Admin (cuenta de servicio, salta las reglas de Firestore). */
public class FirestoreAsignaciones implements AsignacionesClient {

    static final String COLECCION = "asignaciones";
    private static final long ESPERA_SEGUNDOS = 10;

    private final Firestore db;

    public FirestoreAsignaciones(Firestore db) {
        this.db = db;
    }

    @Override
    public Optional<Reemplazo> de(long servicioId) {
        DocumentSnapshot doc = esperar(db.collection(COLECCION).document(String.valueOf(servicioId)).get());
        return doc.exists() ? Optional.ofNullable(aReemplazo(doc)) : Optional.empty();
    }

    @Override
    public List<Reemplazo> recibidos(String uid, LocalDate fecha) {
        return esperar(db.collection(COLECCION)
                .whereEqualTo("uid", uid)
                .whereEqualTo("fecha", fecha.toString())
                .get())
                .getDocuments().stream()
                .map(FirestoreAsignaciones::aReemplazo)
                .filter(r -> r != null)
                .toList();
    }

    @Override
    public boolean activo() {
        return true;
    }

    /** El id del documento es el del servicio; sin uid el documento no sirve. */
    static Reemplazo aReemplazo(DocumentSnapshot doc) {
        String uid = doc.getString("uid");
        if (uid == null || uid.isBlank()) return null;
        long servicioId;
        try {
            servicioId = Long.parseLong(doc.getId());
        } catch (NumberFormatException e) {
            return null;
        }
        return new Reemplazo(servicioId, uid, doc.getString("rol"), doc.getString("fecha"));
    }

    private static <T> T esperar(com.google.api.core.ApiFuture<T> futuro) {
        try {
            return futuro.get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErrorFirestore("Se interrumpió la lectura de Firestore.", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new ErrorFirestore("No se pudieron leer los reemplazos en Firestore.", e);
        }
    }
}
