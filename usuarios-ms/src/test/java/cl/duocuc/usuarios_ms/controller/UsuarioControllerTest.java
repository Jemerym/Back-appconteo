package cl.duocuc.usuarios_ms.controller;

import cl.duocuc.usuarios_ms.dto.ActualizarUsuarioRequest;
import cl.duocuc.usuarios_ms.dto.CambiarContrasenaRequest;
import cl.duocuc.usuarios_ms.dto.LoginRequest;
import cl.duocuc.usuarios_ms.dto.RecuperarClaveRequest;
import cl.duocuc.usuarios_ms.dto.RegistroRequest;
import cl.duocuc.usuarios_ms.dto.UsuarioResponse;
import cl.duocuc.usuarios_ms.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService service;

    @InjectMocks
    private UsuarioController controller;

    private MockMvc mockMvc;

    private static final UsuarioResponse CAMILA =
            new UsuarioResponse(2L, "Camila", "camila@duocuc.cl", 2L, "Observador", null, true);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listar() throws Exception {
        when(service.listar("cam", true)).thenReturn(List.of(CAMILA));

        mockMvc.perform(get("/api/usuarios").param("buscar", "cam").param("incluirAnulados", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Camila"));
    }

    @Test
    void obtener() throws Exception {
        when(service.obtener(2L)).thenReturn(CAMILA);

        mockMvc.perform(get("/api/usuarios/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolNombre").value("Observador"));
    }

    @Test
    void obtenerInexistente() throws Exception {
        when(service.obtener(9L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mockMvc.perform(get("/api/usuarios/9")).andExpect(status().isNotFound());
    }

    @Test
    void registrar() throws Exception {
        when(service.registrar(any(RegistroRequest.class))).thenReturn(CAMILA);

        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Camila\",\"correo\":\"camila@duocuc.cl\",\"contrasena\":\"Clave123!\",\"palabraSecreta\":\"duoc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void login() throws Exception {
        when(service.iniciarSesion(any(LoginRequest.class))).thenReturn(CAMILA);

        mockMvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"camila@duocuc.cl\",\"contrasena\":\"Clave123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("camila@duocuc.cl"));
    }

    @Test
    void actualizar() throws Exception {
        when(service.actualizar(eq(2L), any(ActualizarUsuarioRequest.class))).thenReturn(CAMILA);

        mockMvc.perform(put("/api/usuarios/2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Camila\",\"correo\":\"camila@duocuc.cl\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void cambiarContrasena() throws Exception {
        mockMvc.perform(put("/api/usuarios/2/contrasena").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"Clave123!\",\"nueva\":\"Nueva123#\"}"))
                .andExpect(status().isNoContent());
        verify(service).cambiarContrasena(eq(2L), any(CambiarContrasenaRequest.class));
    }

    @Test
    void recuperarClave() throws Exception {
        mockMvc.perform(post("/api/usuarios/recuperar-clave").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"camila@duocuc.cl\",\"palabraSecreta\":\"duoc\",\"nuevaClave\":\"Nueva123#\"}"))
                .andExpect(status().isNoContent());
        verify(service).recuperarClave(any(RecuperarClaveRequest.class));
    }

    @Test
    void subirAvatar() throws Exception {
        when(service.subirAvatar(eq(2L), any(MultipartFile.class))).thenReturn(CAMILA);

        mockMvc.perform(multipart("/api/usuarios/2/avatar")
                        .file(new MockMultipartFile("archivo", "f.jpg", "image/jpeg", new byte[]{1})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void obtenerAvatar() throws Exception {
        when(service.obtenerAvatar(2L)).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/usuarios/2/avatar"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void anularYRestaurar() throws Exception {
        when(service.anular(2L)).thenReturn(CAMILA);
        when(service.restaurar(2L)).thenReturn(CAMILA);

        mockMvc.perform(put("/api/usuarios/2/anular")).andExpect(status().isOk());
        mockMvc.perform(put("/api/usuarios/2/restaurar")).andExpect(status().isOk());
    }
}
