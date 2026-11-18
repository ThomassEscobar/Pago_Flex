package cl.pagoflex.compromisos.infraestructura.persistencia;


import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.pagoflex.compromisos.dominio.model.OrganizacionEntidad;

public interface RepositorioOrganizacionJpa extends JpaRepository<OrganizacionEntidad, UUID> {
}