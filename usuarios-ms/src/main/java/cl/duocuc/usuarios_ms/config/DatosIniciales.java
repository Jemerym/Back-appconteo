package cl.duocuc.usuarios_ms.config;

import cl.duocuc.usuarios_ms.model.Usuario;
import cl.duocuc.usuarios_ms.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;    

import java.util.List;

@Component
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            usuarioRepository.saveAll(List.of(
                    usuario("Administrador", "admin@demo.cl", "1234", 1L),
                    usuario("Camila", "camila@duocuc.cl", "123456", 2L),
                    usuario("Diego", "diego@duocuc.cl", "123456", 2L)
            ));
        }
    }

    private Usuario usuario(String nombre, String correo, String contrasena, Long rolId) {
        return Usuario.builder()
                .nombre(nombre)
                .correo(correo)
                .contrasenaHash(passwordEncoder.encode(contrasena))
                .palabraSecretaHash(passwordEncoder.encode("duoc"))
                .rolId(rolId)
                .build();
    }
}
