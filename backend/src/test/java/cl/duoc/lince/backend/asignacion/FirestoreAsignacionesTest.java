package cl.duoc.lince.backend.asignacion;

import cl.duoc.lince.backend.comun.ErrorFirestore;
import cl.duoc.lince.backend.config.LinceProperties;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Firestore simulado con Mockito: prueba cómo se leen los documentos sin conectarse a Firebase. */
class FirestoreAsignacionesTest {

    final Firestore db = mock(Firestore.class);
    final CollectionReference coleccion = mock(CollectionReference.class);
    final FirestoreAsignaciones asignaciones = new FirestoreAsignaciones(db);

    static <T extends DocumentSnapshot> T documento(Class<T> tipo, String id, String uid) {
        T doc = mock(tipo);
        when(doc.exists()).thenReturn(true);
        when(doc.getId()).thenReturn(id);
        when(doc.getString("uid")).thenReturn(uid);
        when(doc.getString("rol")).thenReturn("conductor");
        when(doc.getString("fecha")).thenReturn("2026-11-20");
        return doc;
    }

    void documentoEn(String id, DocumentSnapshot doc) {
        DocumentReference ref = mock(DocumentReference.class);
        when(db.collection("asignaciones")).thenReturn(coleccion);
        when(coleccion.document(id)).thenReturn(ref);
        when(ref.get()).thenReturn(ApiFutures.immediateFuture(doc));
    }

    @Test
    void leeElReemplazoDeUnServicio() {
        documentoEn("3008", documento(DocumentSnapshot.class, "3008", "uid-patricio"));
        assertThat(asignaciones.de(3008))
                .contains(new Reemplazo(3008, "uid-patricio", "conductor", "2026-11-20"));
        assertThat(asignaciones.activo()).isTrue();
    }

    @Test
    void sinDocumentoNoHayReemplazo() {
        DocumentSnapshot vacio = mock(DocumentSnapshot.class);
        when(vacio.exists()).thenReturn(false);
        documentoEn("1", vacio);
        assertThat(asignaciones.de(1)).isEmpty();
    }

    @Test
    void unDocumentoSinUidOConIdRaroSeIgnora() {
        assertThat(FirestoreAsignaciones.aReemplazo(documento(DocumentSnapshot.class, "3008", " "))).isNull();
        assertThat(FirestoreAsignaciones.aReemplazo(documento(DocumentSnapshot.class, "abc", "uid-x"))).isNull();
    }

    @Test
    void buscaLosRecibidosPorUidYFecha() {
        Query porUid = mock(Query.class);
        Query porFecha = mock(Query.class);
        QuerySnapshot resultado = mock(QuerySnapshot.class);
        when(db.collection("asignaciones")).thenReturn(coleccion);
        when(coleccion.whereEqualTo("uid", "uid-patricio")).thenReturn(porUid);
        when(porUid.whereEqualTo("fecha", "2026-11-20")).thenReturn(porFecha);
        when(porFecha.get()).thenReturn(ApiFutures.immediateFuture(resultado));
        // Los documentos se crean antes: Mockito no permite simular dentro de otro when(...).
        List<QueryDocumentSnapshot> documentos = List.of(
                documento(QueryDocumentSnapshot.class, "3008", "uid-patricio"),
                documento(QueryDocumentSnapshot.class, "malo", "uid-patricio"));
        when(resultado.getDocuments()).thenReturn(documentos);

        assertThat(asignaciones.recibidos("uid-patricio", LocalDate.of(2026, 11, 20)))
                .extracting(Reemplazo::servicioId).containsExactly(3008L);
    }

    @Test
    void siFirestoreFallaSeAvisaConErrorFirestore() {
        DocumentReference ref = mock(DocumentReference.class);
        when(db.collection("asignaciones")).thenReturn(coleccion);
        when(coleccion.document("9")).thenReturn(ref);
        when(ref.get()).thenReturn(ApiFutures.immediateFailedFuture(new IllegalStateException("sin red")));

        assertThatThrownBy(() -> asignaciones.de(9)).isInstanceOf(ErrorFirestore.class);
    }

    @Test
    void sinCuentaDeServicioSeUsaSinAsignaciones() {
        AsignacionesClient cliente = new AsignacionesConfig().asignaciones(
                new LinceProperties(new LinceProperties.Firebase("lince-track-66c55", ""), null, null));

        assertThat(cliente).isInstanceOf(SinAsignaciones.class);
        assertThat(cliente.activo()).isFalse();
        assertThat(cliente.de(1)).isEmpty();
        assertThat(cliente.recibidos("x", LocalDate.now())).isEmpty();
    }

    @Test
    void unaRutaQueNoExisteImpideArrancar() {
        LinceProperties propiedades = new LinceProperties(
                new LinceProperties.Firebase("lince-track-66c55", "no/existe.json"), null, null);
        assertThatThrownBy(() -> new AsignacionesConfig().asignaciones(propiedades))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no/existe.json");
    }
}
