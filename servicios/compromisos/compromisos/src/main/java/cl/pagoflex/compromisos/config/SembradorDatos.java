package cl.pagoflex.compromisos.config;

import cl.pagoflex.compromisos.dominio.model.CompromisoEntidad;
import cl.pagoflex.compromisos.dominio.model.OrganizacionEntidad;
import cl.pagoflex.compromisos.infraestructura.persistencia.RepositorioCompromisoJpa;
import cl.pagoflex.compromisos.infraestructura.persistencia.RepositorioOrganizacionJpa;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Configuration
public class SembradorDatos {

    // UUID fijo de Ana (debe coincidir con el de autenticación)
    private static final UUID ANA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Bean
    CommandLineRunner sembrarCompromisos(RepositorioCompromisoJpa compromisosRepo,
                                         RepositorioOrganizacionJpa orgRepo) {
        return args -> {
            if (compromisosRepo.count() > 0) return;

            OrganizacionEntidad inmobiliaria = new OrganizacionEntidad();
            inmobiliaria.setId(UUID.randomUUID());
            inmobiliaria.setNombre("Inmobiliaria Andes");
            inmobiliaria.setRut("76.123.456-7");
            inmobiliaria.setTipo("INMOBILIARIA");
            // Usamos el repositorio en lugar de EntityManager
            orgRepo.save(inmobiliaria);

            crearCompromiso(compromisosRepo, ANA_ID, inmobiliaria, "Arriendo Marzo", 350000L, LocalDate.now().plusDays(5), "PENDIENTE");
            crearCompromiso(compromisosRepo, ANA_ID, inmobiliaria, "Gastos Comunes", 45000L, LocalDate.now().minusDays(2), "VENCIDO");
            crearCompromiso(compromisosRepo, ANA_ID, inmobiliaria, "Arriendo Febrero", 350000L, LocalDate.now().minusMonths(1), "PAGADO");

            System.out.println("✅ Compromisos semilla creados.");
        };
    }

    private void crearCompromiso(RepositorioCompromisoJpa repo, UUID usuarioId, OrganizacionEntidad org, String desc, Long monto, LocalDate fecha, String estado) {
        CompromisoEntidad c = new CompromisoEntidad();
        c.setId(UUID.randomUUID());
        c.setUsuarioId(usuarioId);
        c.setOrganizacion(org);
        c.setDescripcion(desc);
        c.setMontoClp(monto);
        c.setFechaVencimiento(fecha);
        c.setEstado(estado);
        c.setCreadoEn(Instant.now());
        c.setActualizadoEn(Instant.now());
        repo.save(c);
    }
}