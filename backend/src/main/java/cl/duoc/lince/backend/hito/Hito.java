package cl.duoc.lince.backend.hito;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Check-in, check-out o posición de un conductor o guía en un servicio.
 * Se guarda siempre, también cuando falla la validación: es la evidencia que ve coordinación.
 * La hora es la del servidor, no la del teléfono.
 */
@Entity
@Table(name = "hitos", indexes = @Index(name = "ix_hitos_servicio", columnList = "servicioId"))
public class Hito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Generada por la app (UUID). Si el teléfono reenvía al recuperar conexión, no se duplica. */
    @Column(nullable = false, unique = true, length = 64)
    private String claveIdempotencia;

    /** uid de Firebase de quien registra. */
    @Column(nullable = false)
    private String uid;

    @Column(nullable = false)
    private long servicioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoHito tipo;

    private double lat;
    private double lon;
    private double precisionMetros;

    private Long geocercaId;
    private String geocercaNombre;
    private Integer radioMetros;
    private Double distanciaMetros;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultadoHito resultado;

    @Column(nullable = false)
    private Instant registradoEn;

    /** "conductor" o "guia", del token al registrar (para agrupar el resumen por recurso). */
    @Column(length = 20)
    private String rol;

    /** Fecha y hora de pickup del servicio (hora de Chile), para calcular con cuánta anticipación llegó. */
    private LocalDateTime inicioServicio;

    protected Hito() {
    }

    public Hito(String claveIdempotencia, String uid, long servicioId, TipoHito tipo,
                double lat, double lon, double precisionMetros, Instant registradoEn) {
        this.claveIdempotencia = claveIdempotencia;
        this.uid = uid;
        this.servicioId = servicioId;
        this.tipo = tipo;
        this.lat = lat;
        this.lon = lon;
        this.precisionMetros = precisionMetros;
        this.registradoEn = registradoEn;
    }

    void asignarGeocerca(Long geocercaId, String nombre, Integer radioMetros, Double distanciaMetros) {
        this.geocercaId = geocercaId;
        this.geocercaNombre = nombre;
        this.radioMetros = radioMetros;
        this.distanciaMetros = distanciaMetros;
    }

    void asignarResultado(ResultadoHito resultado) {
        this.resultado = resultado;
    }

    void asignarContexto(String rol, LocalDateTime inicioServicio) {
        this.rol = rol;
        this.inicioServicio = inicioServicio;
    }

    public String getRol() {
        return rol;
    }

    public LocalDateTime getInicioServicio() {
        return inicioServicio;
    }

    public Long getId() {
        return id;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public String getUid() {
        return uid;
    }

    public long getServicioId() {
        return servicioId;
    }

    public TipoHito getTipo() {
        return tipo;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public double getPrecisionMetros() {
        return precisionMetros;
    }

    public Long getGeocercaId() {
        return geocercaId;
    }

    public String getGeocercaNombre() {
        return geocercaNombre;
    }

    public Integer getRadioMetros() {
        return radioMetros;
    }

    public Double getDistanciaMetros() {
        return distanciaMetros;
    }

    public ResultadoHito getResultado() {
        return resultado;
    }

    public Instant getRegistradoEn() {
        return registradoEn;
    }
}
