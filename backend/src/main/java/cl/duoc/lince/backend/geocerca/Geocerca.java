package cl.duoc.lince.backend.geocerca;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Punto de pickup o dropoff (hotel, aeropuerto) con su radio.
 * La API de Southbound trae pickup y dropoff solo como texto: un hito busca la geocerca cuyo nombre
 * coincide con ese texto. Las coordenadas son ficticias, como pide la pauta.
 */
@Entity
@Table(name = "geocercas")
public class Geocerca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Igual al texto de pickup/dropoff de la API, por ejemplo "Hotel Costanera Puerto Varas". */
    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lon;

    @Column(nullable = false)
    private int radioMetros;

    @Column(nullable = false)
    private boolean activa = true;

    protected Geocerca() {
    }

    public Geocerca(String nombre, double lat, double lon, int radioMetros) {
        this.nombre = nombre;
        this.lat = lat;
        this.lon = lon;
        this.radioMetros = radioMetros;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public int getRadioMetros() {
        return radioMetros;
    }

    public void setRadioMetros(int radioMetros) {
        this.radioMetros = radioMetros;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
