package cl.duoc.lince.backend.geocerca;

import cl.duoc.lince.backend.comun.ErrorNegocio;
import cl.duoc.lince.backend.parametro.ParametrosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** CRUD del catálogo de hoteles y puntos de pickup. Todos leen; solo coordinación (administrador) edita. */
@RestController
@RequestMapping("/api/geocercas")
@Tag(name = "Geocercas", description = "Hoteles y puntos de pickup/dropoff con su radio")
public class GeocercaController {

    private final GeocercaRepository repositorio;
    private final ParametrosService parametros;

    public GeocercaController(GeocercaRepository repositorio, ParametrosService parametros) {
        this.repositorio = repositorio;
        this.parametros = parametros;
    }

    @GetMapping
    @Operation(summary = "Lista las geocercas")
    List<Geocerca> listar() {
        return repositorio.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una geocerca")
    Geocerca obtener(@PathVariable long id) {
        return buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Crea una geocerca (administrador)")
    @Transactional
    Geocerca crear(@Valid @RequestBody GeocercaSolicitud s) {
        if (repositorio.existsByNombreIgnoreCase(s.nombre().trim())) {
            throw ErrorNegocio.conflicto("GEOCERCA_DUPLICADA", "Ya existe una geocerca con ese nombre.");
        }
        int radio = s.radioMetros() != null ? s.radioMetros() : parametros.actuales().getRadioPorDefectoMetros();
        Geocerca g = new Geocerca(s.nombre().trim(), s.lat(), s.lon(), radio);
        if (s.activa() != null) g.setActiva(s.activa());
        return repositorio.save(g);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Edita una geocerca, por ejemplo su radio (administrador)")
    @Transactional
    Geocerca editar(@PathVariable long id, @Valid @RequestBody GeocercaSolicitud s) {
        Geocerca g = buscar(id);
        g.setNombre(s.nombre().trim());
        g.setLat(s.lat());
        g.setLon(s.lon());
        if (s.radioMetros() != null) g.setRadioMetros(s.radioMetros());
        if (s.activa() != null) g.setActiva(s.activa());
        return repositorio.save(g);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Elimina una geocerca (administrador)")
    @Transactional
    void eliminar(@PathVariable long id) {
        repositorio.delete(buscar(id));
    }

    private Geocerca buscar(long id) {
        return repositorio.findById(id).orElseThrow(() -> ErrorNegocio.noEncontrado("La geocerca"));
    }
}
