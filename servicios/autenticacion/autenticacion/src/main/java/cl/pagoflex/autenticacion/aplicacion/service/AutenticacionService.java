package cl.pagoflex.autenticacion.aplicacion.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import cl.pagoflex.autenticacion.aplicacion.dto.LoginRequest;
import cl.pagoflex.autenticacion.aplicacion.dto.LoginResponse;
import cl.pagoflex.autenticacion.aplicacion.dto.UsuarioDto;
import cl.pagoflex.autenticacion.dominio.model.UsuarioEntidad;
import cl.pagoflex.autenticacion.infraestructura.persistencia.repository.RepositorioUsuarioJpa;
import cl.pagoflex.autenticacion.infraestructura.security.JwtEmisor;

@Service
public class AutenticacionService {

    private final RepositorioUsuarioJpa usuarios;
    private final PasswordEncoder encoder;
    private final JwtEmisor jwtEmisor;

    public AutenticacionService(RepositorioUsuarioJpa usuarios, PasswordEncoder encoder, JwtEmisor jwtEmisor) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtEmisor = jwtEmisor;
    }

    public LoginResponse iniciarSesion(LoginRequest request) {
        // 1. Buscar usuario
        UsuarioEntidad usuario = usuarios.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        // 2. Validar contraseña
        if (!encoder.matches(request.getContrasena(), usuario.getHashContrasena())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        // 3. Generar token
        String tokenAcceso = jwtEmisor.generarToken(usuario);
        
        // (Por ahora el token de renovación será un mock, luego lo haremos real)
        String tokenRenovacion = UUID.randomUUID().toString(); 

        List<String> roles = usuario.getRoles().stream()
                .map(r -> r.getNombre())
                .collect(Collectors.toList());

        UsuarioDto usuarioDto = UsuarioDto.builder()
                .id(usuario.getId())
                .correo(usuario.getCorreo())
                .nombreCompleto(usuario.getNombreCompleto())
                .roles(roles)
                .build();

        return LoginResponse.builder()
                .tokenAcceso(tokenAcceso)
                .tokenRenovacion(tokenRenovacion)
                .expiraEn(Instant.now().plusSeconds(15 * 60).toEpochMilli())
                .usuario(usuarioDto)
                .build();
    }
}
