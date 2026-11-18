package cl.pagoflex.autenticacion.config;


import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import cl.pagoflex.autenticacion.dominio.model.RolEntidad;
import cl.pagoflex.autenticacion.dominio.model.UsuarioEntidad;
import cl.pagoflex.autenticacion.infraestructura.persistencia.repository.RepositorioRolJpa;
import cl.pagoflex.autenticacion.infraestructura.persistencia.repository.RepositorioUsuarioJpa;

@Configuration
public class SembradorDatos {

    private static final String CLAVE_SEMILLA = "Pagoflex2026!";
    
    // ID fijo para Ana (coincide con el microservicio de compromisos)
    private static final UUID ANA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Bean
    CommandLineRunner sembrarUsuarios(RepositorioUsuarioJpa usuarios,
                                      RepositorioRolJpa roles,
                                      PasswordEncoder encoder) {
        return args -> {
            RolEntidad rolUser = roles.findByNombre("USER")
                    .orElseGet(() -> roles.save(new RolEntidad(null, "USER")));
            roles.findByNombre("SERVICE")
                    .orElseGet(() -> roles.save(new RolEntidad(null, "SERVICE")));

            // Ana tiene el UUID fijo
            crearSiNoExiste(usuarios, encoder, rolUser, ANA_ID, "ana@pagoflex.cl", "Ana Pérez");
            // Los demás tienen UUID aleatorio
            crearSiNoExiste(usuarios, encoder, rolUser, UUID.randomUUID(), "bruno@pagoflex.cl", "Bruno Soto");
            crearSiNoExiste(usuarios, encoder, rolUser, UUID.randomUUID(), "carola@pagoflex.cl", "Carola Muñoz");
            
            System.out.println("✅ Usuarios semilla verificados/creados.");
        };
    }

    private void crearSiNoExiste(RepositorioUsuarioJpa usuarios,
                                 PasswordEncoder encoder,
                                 RolEntidad rol,
                                 UUID id,
                                 String correo,
                                 String nombre) {
        if (usuarios.findByCorreo(correo).isPresent()) return;
        
        UsuarioEntidad u = new UsuarioEntidad();
        u.setId(id);
        u.setCorreo(correo);
        u.setNombreCompleto(nombre);
        u.setHashContrasena(encoder.encode(CLAVE_SEMILLA));
        u.setEstado("ACTIVO");
        u.setCreadoEn(Instant.now());
        u.setActualizadoEn(Instant.now());
        u.setRoles(Set.of(rol));
        usuarios.save(u);
    }
}