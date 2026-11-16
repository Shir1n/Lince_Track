package cl.duoc.lince.backend.comun;

import org.springframework.http.HttpStatus;

/** Error esperado (regla de negocio, permiso o dato inexistente) con su código para la app. */
public class ErrorNegocio extends RuntimeException {

    private final HttpStatus estado;
    private final String codigo;

    public ErrorNegocio(HttpStatus estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public HttpStatus estado() {
        return estado;
    }

    public String codigo() {
        return codigo;
    }

    public static ErrorNegocio noEncontrado(String que) {
        return new ErrorNegocio(HttpStatus.NOT_FOUND, "NOT_FOUND", que + " no existe.");
    }

    public static ErrorNegocio prohibido(String mensaje) {
        return new ErrorNegocio(HttpStatus.FORBIDDEN, "FORBIDDEN", mensaje);
    }

    public static ErrorNegocio conflicto(String codigo, String mensaje) {
        return new ErrorNegocio(HttpStatus.CONFLICT, codigo, mensaje);
    }
}
