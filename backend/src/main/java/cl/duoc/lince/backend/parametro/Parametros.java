package cl.duoc.lince.backend.parametro;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Reglas que coordinación puede cambiar sin tocar código. Hay una sola fila (id 1); la primera vez se crea con
 * los valores de application.yml (lince.reglas).
 */
@Entity
@Table(name = "parametros")
public class Parametros {

    public static final long ID_UNICO = 1L;

    @Id
    private Long id = ID_UNICO;

    /** El check-in vale si se hace a más tardar estos minutos antes del pickup. */
    @Column(nullable = false)
    private int anticipacionMinutos;

    /** Sobre esta precisión (metros) el GPS se considera impreciso. */
    @Column(nullable = false)
    private int precisionMaximaMetros;

    /** Radio con que se crean las geocercas nuevas (las existentes conservan el suyo). */
    @Column(nullable = false)
    private int radioPorDefectoMetros;

    /** Un día se puede bloquear solo con esta anticipación. */
    @Column(nullable = false)
    private int diasAnticipacionBloqueo;

    private Instant actualizadoEn;

    /** uid de Firebase de quien hizo el último cambio. */
    private String actualizadoPor;

    protected Parametros() {
    }

    public Parametros(int anticipacionMinutos, int precisionMaximaMetros, int radioPorDefectoMetros,
                      int diasAnticipacionBloqueo) {
        this.anticipacionMinutos = anticipacionMinutos;
        this.precisionMaximaMetros = precisionMaximaMetros;
        this.radioPorDefectoMetros = radioPorDefectoMetros;
        this.diasAnticipacionBloqueo = diasAnticipacionBloqueo;
    }

    void actualizar(ParametrosSolicitud s, String uid, Instant cuando) {
        this.anticipacionMinutos = s.anticipacionMinutos();
        this.precisionMaximaMetros = s.precisionMaximaMetros();
        this.radioPorDefectoMetros = s.radioPorDefectoMetros();
        this.diasAnticipacionBloqueo = s.diasAnticipacionBloqueo();
        this.actualizadoPor = uid;
        this.actualizadoEn = cuando;
    }

    public Long getId() {
        return id;
    }

    public int getAnticipacionMinutos() {
        return anticipacionMinutos;
    }

    public int getPrecisionMaximaMetros() {
        return precisionMaximaMetros;
    }

    public int getRadioPorDefectoMetros() {
        return radioPorDefectoMetros;
    }

    public int getDiasAnticipacionBloqueo() {
        return diasAnticipacionBloqueo;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public String getActualizadoPor() {
        return actualizadoPor;
    }
}
