package cl.pagoflex.compromisos.dominio.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "organizaciones")
@Data
public class OrganizacionEntidad {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String nombre;
    private String rut;
    @Column(nullable = false)
    private String tipo;
}
