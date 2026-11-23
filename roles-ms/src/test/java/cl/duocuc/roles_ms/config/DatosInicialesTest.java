package cl.duocuc.roles_ms.config;

import cl.duocuc.roles_ms.model.Rol;
import cl.duocuc.roles_ms.repository.RolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatosInicialesTest {

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private DatosIniciales datosIniciales;

    @Test
    @SuppressWarnings("unchecked")
    void creaLosRolesCuandoLaTablaEstaVacia() {
        when(rolRepository.count()).thenReturn(0L);

        datosIniciales.run();

        ArgumentCaptor<List<Rol>> captor = ArgumentCaptor.forClass(List.class);
        verify(rolRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(Rol::getNombre).containsExactly("Administrador", "Observador");
    }

    @Test
    void noHaceNadaSiYaHayRoles() {
        when(rolRepository.count()).thenReturn(2L);

        datosIniciales.run();

        verify(rolRepository, never()).saveAll(any());
    }
}
