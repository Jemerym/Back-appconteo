package cl.duocuc.roles_ms.service;

import cl.duocuc.roles_ms.model.Rol;
import cl.duocuc.roles_ms.repository.RolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private RolService rolService;

    @Test
    void listarDevuelveTodosLosRoles() {
        List<Rol> roles = List.of(new Rol(1L, "Administrador"), new Rol(2L, "Observador"));
        when(rolRepository.findAll()).thenReturn(roles);

        assertThat(rolService.listar()).hasSize(2).extracting(Rol::getNombre)
                .containsExactly("Administrador", "Observador");
    }

    @Test
    void obtenerDevuelveElRolCuandoExiste() {
        when(rolRepository.findById(1L)).thenReturn(Optional.of(new Rol(1L, "Administrador")));

        Rol rol = rolService.obtener(1L);

        assertThat(rol.getId()).isEqualTo(1L);
        assertThat(rol.getNombre()).isEqualTo("Administrador");
    }

    @Test
    void obtenerLanza404CuandoNoExiste() {
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rolService.obtener(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
