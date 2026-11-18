package cl.pagoflex.autenticacion.aplicacion.dto;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsuarioDto {
    private UUID id;
    private String correo;
    private String nombreCompleto;
    private List<String> roles;
}