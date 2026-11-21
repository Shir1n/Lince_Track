package cl.duoc.lince.backend.geocerca;

import cl.duoc.lince.backend.parametro.ParametrosService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Si la tabla está vacía, crea las geocercas de los cuatro lugares de pickup/dropoff que usa la API en
 * Puerto Varas (PUX). Las coordenadas son aproximadas y ficticias, solo para la demostración.
 */
@Component
public class CargaInicialGeocercas implements ApplicationRunner {

    private final GeocercaRepository repositorio;
    private final ParametrosService parametros;

    public CargaInicialGeocercas(GeocercaRepository repositorio, ParametrosService parametros) {
        this.repositorio = repositorio;
        this.parametros = parametros;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repositorio.count() > 0) return;
        int radio = parametros.actuales().getRadioPorDefectoMetros();
        repositorio.save(new Geocerca("Hotel Costanera Puerto Varas", -41.3176, -72.9822, radio));
        repositorio.save(new Geocerca("Hotel Centro Puerto Varas", -41.3192, -72.9858, radio));
        repositorio.save(new Geocerca("Hostería Lago Llanquihue", -41.3090, -72.9665, radio));
        repositorio.save(new Geocerca("Aeropuerto El Tepual", -41.4386, -73.0939, radio));
    }
}
