package cl.duoc.lince.backend.comun;

/** Firestore no respondió al leer los reemplazos. */
public class ErrorFirestore extends RuntimeException {

    public ErrorFirestore(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
