package cl.duocuc.roles_ms.config;

import cl.duocuc.roles_ms.model.Rol;
import cl.duocuc.roles_ms.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    private final RolRepository rolRepository;

    @Override
    public void run(String... args) {
        if (rolRepository.count() == 0) {
            rolRepository.saveAll(List.of(
                    Rol.builder().nombre("Administrador").build(),
                    Rol.builder().nombre("Observador").build()
            ));
        }
    }
}
