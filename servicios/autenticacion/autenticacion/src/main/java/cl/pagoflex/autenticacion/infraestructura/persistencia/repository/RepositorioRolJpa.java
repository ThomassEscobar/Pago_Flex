package cl.pagoflex.autenticacion.infraestructura.persistencia.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.pagoflex.autenticacion.dominio.model.RolEntidad;

public interface RepositorioRolJpa extends JpaRepository<RolEntidad, Integer> {
    Optional<RolEntidad> findByNombre(String nombre);
}