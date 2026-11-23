package cl.duocuc.roles_ms.controller;

import cl.duocuc.roles_ms.model.Rol;
import cl.duocuc.roles_ms.service.RolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RolControllerTest {

    @Mock
    private RolService rolService;

    @InjectMocks
    private RolController rolController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rolController).build();
    }

    @Test
    void listarRoles() throws Exception {
        when(rolService.listar()).thenReturn(List.of(new Rol(1L, "Administrador"), new Rol(2L, "Observador")));

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Administrador"))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void obtenerRol() throws Exception {
        when(rolService.obtener(2L)).thenReturn(Rol.builder().id(2L).nombre("Observador").build());

        mockMvc.perform(get("/api/roles/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Observador"));
    }

    @Test
    void obtenerRolInexistenteDevuelve404() throws Exception {
        when(rolService.obtener(9L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));

        mockMvc.perform(get("/api/roles/9"))
                .andExpect(status().isNotFound());
    }
}
