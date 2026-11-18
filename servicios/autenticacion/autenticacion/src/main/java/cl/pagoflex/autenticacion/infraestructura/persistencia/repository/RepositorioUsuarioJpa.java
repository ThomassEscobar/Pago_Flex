package cl.pagoflex.autenticacion.infraestructura.persistencia.repository;


import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.pagoflex.autenticacion.dominio.model.UsuarioEntidad;

public interface RepositorioUsuarioJpa extends JpaRepository<UsuarioEntidad, UUID> {
    Optional<UsuarioEntidad> findByCorreo(String correo);
}
    

