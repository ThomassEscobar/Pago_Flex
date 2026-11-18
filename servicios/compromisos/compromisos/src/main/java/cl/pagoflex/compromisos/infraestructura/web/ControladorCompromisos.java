package cl.pagoflex.compromisos.infraestructura.web;


import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.pagoflex.compromisos.dominio.model.CompromisoEntidad;
import cl.pagoflex.compromisos.infraestructura.persistencia.RepositorioCompromisoJpa;

@RestController
@RequestMapping("/api/v1/compromisos")
@CrossOrigin(origins = "*")
public class ControladorCompromisos {

    private final RepositorioCompromisoJpa repositorio;

    public ControladorCompromisos(RepositorioCompromisoJpa repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping
    public ResponseEntity<List<CompromisoEntidad>> listar() {
        // Por ahora, hardcodeamos el ID de Ana para probar.
        // En el futuro, lo sacaremos del Token JWT.
        UUID anaId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        return ResponseEntity.ok(repositorio.findByUsuarioIdOrderByFechaVencimientoAsc(anaId));
    }
}
