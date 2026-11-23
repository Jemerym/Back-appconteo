package cl.duocuc.registros_ms.controller;

import cl.duocuc.registros_ms.dto.ConteoResponse;
import cl.duocuc.registros_ms.dto.ModificarRegistroAforoRequest;
import cl.duocuc.registros_ms.dto.RegistroAforoRequest;
import cl.duocuc.registros_ms.model.Registro;
import cl.duocuc.registros_ms.service.RegistroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RegistroControllerTest {

    @Mock
    private RegistroService service;

    @InjectMocks
    private RegistroController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static Registro registro(Long id, Boolean anulado) {
        Registro r = new Registro();
        r.setId(id);
        r.setUsuarioId(2L);
        r.setObservadorNombre("Camila");
        r.setTurnoId(3L);
        r.setAcceso(1);
        r.setTipoRegistro("Cliente");
        r.setTipoGrupo("Solo");
        r.setRangoEtario("18-24");
        r.setCodigoBoleta("B-1");
        r.setFecha(LocalDate.of(2026, 11, 21));
        r.setHora(LocalTime.of(14, 5, 33));
        r.setAnulado(anulado);
        return r;
    }

    @Test
    void listar() throws Exception {
        when(service.listar(2L, 3L, "2026-11-21", true)).thenReturn(List.of(registro(1L, false), registro(2L, null)));

        mockMvc.perform(get("/api/registros")
                        .param("usuarioId", "2").param("turnoId", "3")
                        .param("fecha", "2026-11-21").param("incluirAnulados", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].hora").value("14:05"))
                .andExpect(jsonPath("$[0].fecha").value("2026-11-21"))
                .andExpect(jsonPath("$[0].codigoBoleta").value("B-1"))
                .andExpect(jsonPath("$[1].anulado").value(false));
    }

    @Test
    void conteo() throws Exception {
        when(service.conteo(null, null)).thenReturn(new ConteoResponse("2026-11-21", 2, 3, 5));

        mockMvc.perform(get("/api/registros/conteo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.clientes").value(2));
    }

    @Test
    void existeBoleta() throws Exception {
        when(service.existeBoleta("B-1")).thenReturn(true);

        mockMvc.perform(get("/api/registros/boleta/existe").param("codigo", "B-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(true));
    }

    @Test
    void obtener() throws Exception {
        when(service.obtener(1L)).thenReturn(registro(1L, false));

        mockMvc.perform(get("/api/registros/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observadorNombre").value("Camila"));
    }

    @Test
    void obtenerInexistente() throws Exception {
        when(service.obtener(9L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/registros/9")).andExpect(status().isNotFound());
    }

    @Test
    void crear() throws Exception {
        when(service.crear(any(RegistroAforoRequest.class))).thenReturn(registro(1L, false));

        mockMvc.perform(post("/api/registros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuarioId":2,"observadorNombre":"Camila","turnoId":3,"acceso":1,
                                 "tipoRegistro":"Cliente","tipoGrupo":"Solo","rangoEtario":"18-24","codigoBoleta":"B-1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void modificar() throws Exception {
        when(service.modificar(eq(1L), any(ModificarRegistroAforoRequest.class))).thenReturn(registro(1L, false));

        mockMvc.perform(put("/api/registros/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acceso\":1,\"tipoGrupo\":\"Solo\",\"rangoEtario\":\"18-24\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceso").value(1));
    }

    @Test
    void anular() throws Exception {
        when(service.anular(1L)).thenReturn(registro(1L, true));

        mockMvc.perform(put("/api/registros/1/anular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anulado").value(true));
    }

    @Test
    void restaurar() throws Exception {
        when(service.restaurar(1L)).thenReturn(registro(1L, false));

        mockMvc.perform(put("/api/registros/1/restaurar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anulado").value(false));
    }
}
