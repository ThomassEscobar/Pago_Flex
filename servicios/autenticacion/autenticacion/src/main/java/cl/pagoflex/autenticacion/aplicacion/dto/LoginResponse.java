package cl.pagoflex.autenticacion.aplicacion.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String tokenAcceso;
    private String tokenRenovacion;
    private Long expiraEn;
    private UsuarioDto usuario;
}