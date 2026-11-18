package cl.pagoflex.autenticacion.infraestructura.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.pagoflex.autenticacion.aplicacion.dto.LoginRequest;
import cl.pagoflex.autenticacion.aplicacion.dto.LoginResponse;
import cl.pagoflex.autenticacion.aplicacion.service.AutenticacionService;

@RestController
@RequestMapping("/api/v1/autenticacion")
@CrossOrigin(origins = "*") // Permite que la app Android se conecte
public class ControladorAutenticacion {

    private final AutenticacionService servicioAutenticacion;

    public ControladorAutenticacion(AutenticacionService servicioAutenticacion) {
        this.servicioAutenticacion = servicioAutenticacion;
    }

    @PostMapping("/inicio-sesion")
    public ResponseEntity<LoginResponse> iniciarSesion(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(servicioAutenticacion.iniciarSesion(request));
    }
}