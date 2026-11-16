package cl.duoc.lince.backend.comun;

/** La API de Southbound no respondió o respondió con error (sin clave, caída, límite de solicitudes). */
public class ErrorLince extends RuntimeException {

    public ErrorLince(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
