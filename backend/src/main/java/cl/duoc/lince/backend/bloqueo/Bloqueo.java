package cl.duoc.lince.backend.bloqueo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;

/** Día en que un conductor o guía no está disponible. Una fila por persona y día. */
@Entity
@Table(name = "bloqueos", uniqueConstraints = @UniqueConstraint(name = "uk_bloqueo_uid_fecha", columnNames = {"uid", "fecha"}))
public class Bloqueo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String uid;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 200)
    private String motivo;

    @Column(nullable = false)
    private Instant creadoEn;

    protected Bloqueo() {
    }

    public Bloqueo(String uid, LocalDate fecha, String motivo, Instant creadoEn) {
        this.uid = uid;
        this.fecha = fecha;
        this.motivo = motivo;
        this.creadoEn = creadoEn;
    }

    public Long getId() {
        return id;
    }

    public String getUid() {
        return uid;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
