package cl.duocuc.turnos_ms.service;

import cl.duocuc.turnos_ms.client.UsuarioClient;
import cl.duocuc.turnos_ms.client.UsuarioRemoto;
import cl.duocuc.turnos_ms.dto.TurnoRequest;
import cl.duocuc.turnos_ms.model.Turno;
import cl.duocuc.turnos_ms.repository.TurnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurnoServiceTest {

    @Mock
    private TurnoRepository repo;

    @Mock
    private UsuarioClient usuarios;

    @InjectMocks
    private TurnoService service;

    private static final String MANANA = LocalDate.now(ZoneId.of("America/Santiago")).plusDays(1).toString();
    private static final UsuarioRemoto OBSERVADOR = new UsuarioRemoto(2L, "Camila", 2L, true);

    private static TurnoRequest request(Long usuarioId, Integer acceso, String inicio, String fin, String fecha) {
        return new TurnoRequest(usuarioId, acceso, inicio, fin, fecha);
    }

    private static TurnoRequest requestValido() {
        return request(2L, 1, "10:00", "10:30", MANANA);
    }

    private static Turno turno(Long id, Boolean anulado) {
        Turno t = new Turno();
        t.setId(id);
        t.setUsuarioId(2L);
        t.setObservadorNombre("Camila");
        t.setAcceso(1);
        t.setFecha(LocalDate.of(2030, 1, 1));
        t.setHoraInicio(LocalTime.of(10, 0));
        t.setHoraFin(LocalTime.of(11, 0));
        t.setAnulado(anulado);
        return t;
    }

    private static void assertStatus(Throwable e, HttpStatus estado) {
        assertThat(e).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(estado);
    }

    // ---------- consultas ----------

    @Test
    void listarTodos() {
        when(repo.findAllByOrderByIdDesc()).thenReturn(List.of(turno(1L, false)));
        assertThat(service.listar(null)).hasSize(1);
    }

    @Test
    void listarDeUnUsuario() {
        when(repo.findByUsuarioIdOrderByIdDesc(2L)).thenReturn(List.of(turno(1L, false), turno(2L, false)));
        assertThat(service.listar(2L)).hasSize(2);
    }

    @Test
    void obtenerExistente() {
        Turno t = turno(1L, false);
        when(repo.findById(1L)).thenReturn(Optional.of(t));
        assertThat(service.obtener(1L)).isSameAs(t);
    }

    @Test
    void obtenerInexistenteLanza404() {
        when(repo.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtener(1L)).satisfies(e -> assertStatus(e, HttpStatus.NOT_FOUND));
    }

    @Test
    void obtenerActual() {
        Turno t = turno(1L, false);
        when(repo.buscarActivos(eq(2L), any(), any())).thenReturn(List.of(t));
        assertThat(service.obtenerActual(2L)).contains(t);

        when(repo.buscarActivos(eq(3L), any(), any())).thenReturn(List.of());
        assertThat(service.obtenerActual(3L)).isEmpty();
    }

    // ---------- crear / actualizar ----------

    @Test
    void crearTurnoValido() {
        when(usuarios.obtener(2L)).thenReturn(OBSERVADOR);
        when(repo.contarSolapamientos(any(), any(), any(), eq(2L), eq(1), eq(0L))).thenReturn(0L);
        when(repo.save(any(Turno.class))).thenAnswer(inv -> inv.getArgument(0));

        Turno t = service.crear(requestValido());

        assertThat(t.getUsuarioId()).isEqualTo(2L);
        assertThat(t.getObservadorNombre()).isEqualTo("Camila");
        assertThat(t.getAcceso()).isEqualTo(1);
        assertThat(t.getFecha()).isEqualTo(LocalDate.parse(MANANA));
        assertThat(t.getHoraInicio()).isEqualTo(LocalTime.of(10, 0));
        assertThat(t.getHoraFin()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    void crearConSolapamientoLanza409() {
        when(usuarios.obtener(2L)).thenReturn(OBSERVADOR);
        when(repo.contarSolapamientos(any(), any(), any(), any(), any(), anyLong())).thenReturn(1L);

        assertThatThrownBy(() -> service.crear(requestValido())).satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
        verify(repo, never()).save(any());
    }

    @Test
    void crearValidaLosDatosBasicos() {
        List<TurnoRequest> invalidos = List.of(
                request(null, 1, "10:00", "10:30", MANANA),
                request(2L, null, "10:00", "10:30", MANANA),
                request(2L, 0, "10:00", "10:30", MANANA),
                request(2L, 5, "10:00", "10:30", MANANA),
                request(2L, 1, "10:00", "10:30", null),
                request(2L, 1, "10:00", "10:30", "01-01-2030"),
                request(2L, 1, null, "10:30", MANANA),
                request(2L, 1, "10am", "10:30", MANANA),
                request(2L, 1, "10:30", "10:30", MANANA),
                request(2L, 1, "11:00", "10:30", MANANA),
                request(2L, 1, "10:00", "10:30", "2000-01-01")
        );
        for (TurnoRequest req : invalidos) {
            assertThatThrownBy(() -> service.crear(req)).satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        }
        verify(usuarios, never()).obtener(any());
    }

    @Test
    void crearValidaQueSeaObservadorActivo() {
        when(usuarios.obtener(2L))
                .thenReturn(new UsuarioRemoto(2L, "Admin", 1L, true))
                .thenReturn(new UsuarioRemoto(2L, "SinRol", null, true))
                .thenReturn(new UsuarioRemoto(2L, "Camila", 2L, false));

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.crear(requestValido())).satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        }
        verify(repo, never()).save(any());
    }

    @Test
    void crearConObservadorActivoNullEsValido() {
        when(usuarios.obtener(2L)).thenReturn(new UsuarioRemoto(2L, "Camila", 2L, null));
        when(repo.save(any(Turno.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.crear(requestValido()).getObservadorNombre()).isEqualTo("Camila");
    }

    @Test
    void actualizarExcluyeElPropioTurno() {
        Turno existente = turno(7L, false);
        when(repo.findById(7L)).thenReturn(Optional.of(existente));
        when(usuarios.obtener(2L)).thenReturn(OBSERVADOR);
        when(repo.contarSolapamientos(any(), any(), any(), eq(2L), eq(3), eq(7L))).thenReturn(0L);
        when(repo.save(existente)).thenReturn(existente);

        Turno t = service.actualizar(7L, request(2L, 3, "12:00", "13:00", MANANA));

        assertThat(t.getAcceso()).isEqualTo(3);
        assertThat(t.getHoraInicio()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    void actualizarTurnoAnuladoLanza409() {
        when(repo.findById(7L)).thenReturn(Optional.of(turno(7L, true)));
        assertThatThrownBy(() -> service.actualizar(7L, requestValido())).satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
    }

    // ---------- anular / restaurar ----------

    @Test
    void anularTurnoActivo() {
        Turno t = turno(1L, null);
        when(repo.findById(1L)).thenReturn(Optional.of(t));
        when(repo.save(t)).thenReturn(t);
        assertThat(service.anular(1L).getAnulado()).isTrue();
    }

    @Test
    void anularTurnoYaAnuladoLanza409() {
        when(repo.findById(1L)).thenReturn(Optional.of(turno(1L, true)));
        assertThatThrownBy(() -> service.anular(1L)).satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
    }

    @Test
    void restaurarTurnoAnulado() {
        Turno t = turno(1L, true);
        when(repo.findById(1L)).thenReturn(Optional.of(t));
        when(repo.contarSolapamientos(t.getFecha(), t.getHoraInicio(), t.getHoraFin(), 2L, 1, 1L)).thenReturn(0L);
        when(repo.save(t)).thenReturn(t);
        assertThat(service.restaurar(1L).getAnulado()).isFalse();
    }

    @Test
    void restaurarTurnoConTramoOcupadoLanza409() {
        Turno t = turno(1L, true);
        when(repo.findById(1L)).thenReturn(Optional.of(t));
        when(repo.contarSolapamientos(any(), any(), any(), any(), any(), anyLong())).thenReturn(2L);
        assertThatThrownBy(() -> service.restaurar(1L)).satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
    }

    @Test
    void restaurarTurnoActivoLanza409() {
        when(repo.findById(1L)).thenReturn(Optional.of(turno(1L, false)));
        assertThatThrownBy(() -> service.restaurar(1L)).satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
    }

    @Test
    void anularDeUsuarioAnulaTodosSusTurnos() {
        List<Turno> turnos = List.of(turno(1L, false), turno(2L, false));
        when(repo.findByUsuarioIdAndAnuladoFalse(2L)).thenReturn(turnos);

        service.anularDeUsuario(2L);

        assertThat(turnos).allMatch(Turno::getAnulado);
        verify(repo).saveAll(turnos);
    }
}
