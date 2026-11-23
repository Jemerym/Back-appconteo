package cl.duocuc.usuarios_ms.config;

import cl.duocuc.usuarios_ms.model.Usuario;
import cl.duocuc.usuarios_ms.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatosInicialesTest {

    @Mock
    private UsuarioRepository repo;

    @Mock
    private PasswordEncoder encoder;

    @Test
    @SuppressWarnings("unchecked")
    void creaUsuariosDePruebaSiNoHayNinguno() {
        when(repo.count()).thenReturn(0L);
        when(encoder.encode(anyString())).thenAnswer(inv -> "hash:" + inv.getArgument(0));

        new DatosIniciales(repo, encoder).run();

        ArgumentCaptor<List<Usuario>> captor = ArgumentCaptor.forClass(List.class);
        verify(repo).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(Usuario::getCorreo)
                .containsExactly("admin@demo.cl", "camila@duocuc.cl", "diego@duocuc.cl");
        assertThat(captor.getValue().get(0).getRolId()).isEqualTo(1L);
        assertThat(captor.getValue().get(0).getActivo()).isTrue();
        assertThat(captor.getValue().get(1).getPalabraSecretaHash()).isEqualTo("hash:duoc");
    }

    @Test
    void noHaceNadaSiYaHayUsuarios() {
        when(repo.count()).thenReturn(3L);

        new DatosIniciales(repo, encoder).run();

        verify(repo, never()).saveAll(any());
    }

    @Test
    void passwordConfigUsaBCrypt() {
        PasswordEncoder bcrypt = new PasswordConfig().passwordEncoder();
        assertThat(bcrypt.matches("1234", bcrypt.encode("1234"))).isTrue();
    }
}
