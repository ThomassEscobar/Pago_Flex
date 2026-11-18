package cl.pagoflex.autenticacion.infraestructura.security;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import cl.pagoflex.autenticacion.dominio.model.UsuarioEntidad;
import io.jsonwebtoken.Jwts;

@Component
public class JwtEmisor {

    @Value("${pagoflex.jwt.clave-privada}")
    private Resource clavePrivadaResource;

    @Value("${pagoflex.jwt.emisor}")
    private String emisor;

    @Value("${pagoflex.jwt.minutos-acceso}")
    private int minutosAcceso;

    public String generarToken(UsuarioEntidad usuario) {
        try {
            PrivateKey privateKey = obtenerLlavePrivada();
            Date ahora = new Date();
            Date expiracion = new Date(ahora.getTime() + (minutosAcceso * 60 * 1000));

            List<String> roles = usuario.getRoles().stream()
                    .map(r -> r.getNombre())
                    .collect(Collectors.toList());

            return Jwts.builder()
                    .subject(usuario.getId().toString())
                    .issuer(emisor)
                    .claim("correo", usuario.getCorreo())
                    .claim("roles", roles)
                    .issuedAt(ahora)
                    .expiration(expiracion)
                    .signWith(privateKey)
                    .compact();
        } catch (Exception e) {
            e.printStackTrace(); // Esto imprimirá el error real en la consola
            throw new RuntimeException("Error al generar el token JWT", e);
        }
    }
    private PrivateKey obtenerLlavePrivada() throws Exception {
        try (InputStream is = clavePrivadaResource.getInputStream()) {
            String key = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s+", ""); // Quita saltos de línea y espacios
            byte[] keyBytes = Base64.getDecoder().decode(key);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        }
    }
}