package cl.duocuc.turnos_ms.controller;

import cl.duocuc.turnos_ms.dto.TurnoRequest;
import cl.duocuc.turnos_ms.model.Turno;
import cl.duocuc.turnos_ms.service.TurnoService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TurnoControllerTest {

    @Mock
    private TurnoService service;

    @InjectMocks
    private TurnoController controller;

    private MockMvc mockMvc;

    private static final String JSON = """
            {"usuarioId":2,"acceso":1,"horaInicio":"10:00","horaFin":"10:30","fecha":"2030-01-01"}
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static Turno turno(Long id, Boolean anulado) {
        Turno t = new Turno();
        t.setId(id);
        t.setUsuarioId(2L);
        t.setObservadorNombre("Camila");
        t.setAcceso(1);
        t.setFecha(LocalDate.of(2030, 1, 1));
        t.setHoraInicio(LocalTime.of(10, 0));
        t.setHoraFin(LocalTime.of(10, 30));
        t.setAnulado(anulado);
        return t;
    }

    @Test
    void listar() throws Exception {
        when(service.listar(2L)).thenReturn(List.of(turno(1L, false), turno(2L, null)));

        mockMvc.perform(get("/api/turnos").param("usuarioId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].horaInicio").value("10:00"))
                .andExpect(jsonPath("$[0].horaFin").value("10:30"))
                .andExpect(jsonPath("$[0].fecha").value("2030-01-01"))
                .andExpect(jsonPath("$[1].anulado").value(false));
    }

    @Test
    void actualConTurno() throws Exception {
        when(service.obtenerActual(2L)).thenReturn(Optional.of(turno(1L, false)));

        mockMvc.perform(get("/api/turnos/actual").param("usuarioId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void actualSinTurnoDevuelve204() throws Exception {
        when(service.obtenerActual(2L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/turnos/actual").param("usuarioId", "2"))
                .andExpect(status().isNoContent());
    }

    @Test
    void obtener() throws Exception {
        when(service.obtener(1L)).thenReturn(turno(1L, false));

        mockMvc.perform(get("/api/turnos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observadorNombre").value("Camila"));
    }

    @Test
    void obtenerInexistente() throws Exception {
        when(service.obtener(9L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mockMvc.perform(get("/api/turnos/9")).andExpect(status().isNotFound());
    }

    @Test
    void crear() throws Exception {
        when(service.crear(any(TurnoRequest.class))).thenReturn(turno(1L, false));

        mockMvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void actualizar() throws Exception {
        when(service.actualizar(eq(1L), any(TurnoRequest.class))).thenReturn(turno(1L, false));

        mockMvc.perform(put("/api/turnos/1").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceso").value(1));
    }

    @Test
    void anular() throws Exception {
        when(service.anular(1L)).thenReturn(turno(1L, true));

        mockMvc.perform(put("/api/turnos/1/anular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anulado").value(true));
    }

    @Test
    void restaurar() throws Exception {
        when(service.restaurar(1L)).thenReturn(turno(1L, false));

        mockMvc.perform(put("/api/turnos/1/restaurar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anulado").value(false));
    }

    @Test
    void anularDeUsuario() throws Exception {
        mockMvc.perform(put("/api/turnos/usuario/2/anular"))
                .andExpect(status().isNoContent());
        verify(service).anularDeUsuario(2L);
    }
}
