package cl.duocuc.registros_ms.service;

import cl.duocuc.registros_ms.dto.ConteoResponse;
import cl.duocuc.registros_ms.dto.ModificarRegistroAforoRequest;
import cl.duocuc.registros_ms.dto.RegistroAforoRequest;
import cl.duocuc.registros_ms.model.Registro;
import cl.duocuc.registros_ms.repository.RegistroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroServiceTest {

    @Mock
    private RegistroRepository repo;

    @InjectMocks
    private RegistroService service;

    // ---------- helpers ----------

    private static RegistroAforoRequest request(Long usuarioId, String nombre, Long turnoId, Integer acceso,
                                                String tipo, String grupo, String rango, String boleta) {
        return new RegistroAforoRequest(usuarioId, nombre, turnoId, acceso, tipo, grupo, rango, boleta);
    }

    private static RegistroAforoRequest requestValido() {
        return request(1L, " Camila ", 5L, 2, "Cliente", "Solo", "18-24", " B-001 ");
    }

    private static Registro registro(Long id, Boolean anulado, String boleta) {
        Registro r = new Registro();
        r.setId(id);
        r.setUsuarioId(1L);
        r.setObservadorNombre("Camila");
        r.setAcceso(1);
        r.setTipoRegistro("Cliente");
        r.setTipoGrupo("Solo");
        r.setRangoEtario("18-24");
        r.setCodigoBoleta(boleta);
        r.setFecha(LocalDate.of(2026, 11, 21));
        r.setHora(LocalTime.of(14, 5));
        r.setAnulado(anulado);
        return r;
    }

    private static void assertStatus(Throwable e, HttpStatus estado) {
        assertThat(e).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(estado);
    }

    // ---------- listar ----------

    @Test
    void listarSinFechaNoFiltraPorDia() {
        when(repo.buscar(1L, 2L, null, true)).thenReturn(List.of(registro(1L, false, null)));

        assertThat(service.listar(1L, 2L, null, true)).hasSize(1);
        assertThat(service.listar(1L, 2L, "  ", true)).hasSize(1);
    }

    @Test
    void listarConFechaLaConvierte() {
        LocalDate dia = LocalDate.of(2026, 11, 21);
        when(repo.buscar(null, null, dia, false)).thenReturn(List.of());

        assertThat(service.listar(null, null, " 2026-11-21 ", false)).isEmpty();
        verify(repo).buscar(null, null, dia, false);
    }

    @Test
    void listarConFechaInvalidaLanza400() {
        Throwable e = org.assertj.core.api.Assertions.catchThrowable(() -> service.listar(null, null, "21/11/2026", false));
        assertStatus(e, HttpStatus.BAD_REQUEST);
    }

    // ---------- obtener ----------

    @Test
    void obtenerExistente() {
        Registro r = registro(7L, false, null);
        when(repo.findById(7L)).thenReturn(Optional.of(r));
        assertThat(service.obtener(7L)).isSameAs(r);
    }

    @Test
    void obtenerInexistenteLanza404() {
        when(repo.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtener(7L)).satisfies(e -> assertStatus(e, HttpStatus.NOT_FOUND));
    }

    // ---------- existeBoleta ----------

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void existeBoletaVaciaEsFalse(String codigo) {
        assertThat(service.existeBoleta(codigo)).isFalse();
        verify(repo, never()).existsByCodigoBoletaAndAnuladoFalse(anyString());
    }

    @Test
    void existeBoletaConsultaRecortada() {
        when(repo.existsByCodigoBoletaAndAnuladoFalse("B-1")).thenReturn(true);
        assertThat(service.existeBoleta(" B-1 ")).isTrue();
    }

    // ---------- conteo ----------

    @Test
    void conteoDeUnaFecha() {
        LocalDate dia = LocalDate.of(2026, 11, 21);
        when(repo.contar(dia, RegistroService.CLIENTE, 3L)).thenReturn(4L);
        when(repo.contar(dia, RegistroService.TRANSITO, 3L)).thenReturn(6L);

        ConteoResponse c = service.conteo("2026-11-21", 3L);

        assertThat(c.fecha()).isEqualTo("2026-11-21");
        assertThat(c.clientes()).isEqualTo(4);
        assertThat(c.transito()).isEqualTo(6);
        assertThat(c.total()).isEqualTo(10);
    }

    @Test
    void conteoSinFechaUsaHoy() {
        when(repo.contar(any(), eq(RegistroService.CLIENTE), isNull())).thenReturn(1L);
        when(repo.contar(any(), eq(RegistroService.TRANSITO), isNull())).thenReturn(2L);

        ConteoResponse c = service.conteo(null, null);

        LocalDate hoy = LocalDate.now(ZoneId.of("America/Santiago"));
        assertThat(c.fecha()).isIn(hoy.toString(), hoy.minusDays(1).toString(), hoy.plusDays(1).toString());
        assertThat(c.total()).isEqualTo(3);
        assertThat(service.conteo(" ", null).total()).isEqualTo(3);
    }

    // ---------- crear ----------

    @Test
    void crearRegistroValido() {
        when(repo.existsByCodigoBoletaAndAnuladoFalse("B-001")).thenReturn(false);
        when(repo.save(any(Registro.class))).thenAnswer(inv -> inv.getArgument(0));

        Registro r = service.crear(requestValido());

        assertThat(r.getUsuarioId()).isEqualTo(1L);
        assertThat(r.getObservadorNombre()).isEqualTo("Camila");
        assertThat(r.getTurnoId()).isEqualTo(5L);
        assertThat(r.getAcceso()).isEqualTo(2);
        assertThat(r.getTipoRegistro()).isEqualTo("Cliente");
        assertThat(r.getTipoGrupo()).isEqualTo("Solo");
        assertThat(r.getRangoEtario()).isEqualTo("18-24");
        assertThat(r.getCodigoBoleta()).isEqualTo("B-001");
        assertThat(r.getFecha()).isNotNull();
        assertThat(r.getHora().getSecond()).isZero();
        assertThat(r.getAnulado()).isFalse();
    }

    @Test
    void crearTransitoSinBoletaNiTurno() {
        when(repo.save(any(Registro.class))).thenAnswer(inv -> inv.getArgument(0));

        Registro r = service.crear(request(1L, "Diego", null, 4, "Tránsito", "Grupo (3+)", "+ 50", "  "));
        assertThat(r.getCodigoBoleta()).isNull();
        assertThat(r.getTurnoId()).isNull();

        Registro r2 = service.crear(request(1L, "Diego", 0L, 1, "Tránsito", "Acompañado (2)", "< 18", null));
        assertThat(r2.getTurnoId()).isNull();
        verify(repo, never()).existsByCodigoBoletaAndAnuladoFalse(anyString());
    }

    @Test
    void crearConBoletaRepetidaLanza409() {
        when(repo.existsByCodigoBoletaAndAnuladoFalse("B-001")).thenReturn(true);
        assertThatThrownBy(() -> service.crear(requestValido())).satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
        verify(repo, never()).save(any());
    }

    @Test
    void crearValidaLosDatos() {
        List<RegistroAforoRequest> invalidos = List.of(
                request(null, "Camila", 1L, 1, "Cliente", "Solo", "18-24", null),
                request(1L, null, 1L, 1, "Cliente", "Solo", "18-24", null),
                request(1L, "  ", 1L, 1, "Cliente", "Solo", "18-24", null),
                request(1L, "Camila", 1L, null, "Cliente", "Solo", "18-24", null),
                request(1L, "Camila", 1L, 0, "Cliente", "Solo", "18-24", null),
                request(1L, "Camila", 1L, 5, "Cliente", "Solo", "18-24", null),
                request(1L, "Camila", 1L, 1, null, "Solo", "18-24", null),
                request(1L, "Camila", 1L, 1, "Otro", "Solo", "18-24", null),
                request(1L, "Camila", 1L, 1, "Cliente", "Muchos", "18-24", null),
                request(1L, "Camila", 1L, 1, "Cliente", "Solo", "99", null)
        );
        for (RegistroAforoRequest req : invalidos) {
            assertThatThrownBy(() -> service.crear(req)).satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        }
        verify(repo, never()).save(any());
    }

    // ---------- modificar ----------

    @Test
    void modificarRegistroVigente() {
        Registro r = registro(1L, false, null);
        when(repo.findById(1L)).thenReturn(Optional.of(r));
        when(repo.save(r)).thenReturn(r);

        Registro modificado = service.modificar(1L, new ModificarRegistroAforoRequest(3, "Grupo (3+)", "35-50"));

        assertThat(modificado.getAcceso()).isEqualTo(3);
        assertThat(modificado.getTipoGrupo()).isEqualTo("Grupo (3+)");
        assertThat(modificado.getRangoEtario()).isEqualTo("35-50");
    }

    @Test
    void modificarRegistroAnuladoLanza409() {
        when(repo.findById(1L)).thenReturn(Optional.of(registro(1L, true, null)));
        assertThatThrownBy(() -> service.modificar(1L, new ModificarRegistroAforoRequest(3, "Solo", "35-50")))
                .satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
    }

    @Test
    void modificarConDatosInvalidosLanza400() {
        when(repo.findById(1L)).thenReturn(Optional.of(registro(1L, null, null)));
        assertThatThrownBy(() -> service.modificar(1L, new ModificarRegistroAforoRequest(9, "Solo", "35-50")))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.modificar(1L, new ModificarRegistroAforoRequest(1, "X", "35-50")))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.modificar(1L, new ModificarRegistroAforoRequest(1, "Solo", null)))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
    }

    // ---------- anular / restaurar ----------

    @Test
    void anularRegistroVigente() {
        Registro r = registro(1L, false, null);
        when(repo.findById(1L)).thenReturn(Optional.of(r));
        when(repo.save(r)).thenReturn(r);

        assertThat(service.anular(1L).getAnulado()).isTrue();
    }

    @Test
    void anularRegistroYaAnuladoLanza409() {
        when(repo.findById(1L)).thenReturn(Optional.of(registro(1L, true, null)));
        assertThatThrownBy(() -> service.anular(1L)).satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
    }

    @Test
    void restaurarRegistroAnuladoSinBoleta() {
        Registro r = registro(1L, true, null);
        when(repo.findById(1L)).thenReturn(Optional.of(r));
        when(repo.save(r)).thenReturn(r);

        assertThat(service.restaurar(1L).getAnulado()).isFalse();
        verify(repo, never()).existsByCodigoBoletaAndAnuladoFalseAndIdNot(anyString(), any());
    }

    @Test
    void restaurarRegistroAnuladoConBoletaLibre() {
        Registro r = registro(1L, true, "B-9");
        when(repo.findById(1L)).thenReturn(Optional.of(r));
        when(repo.existsByCodigoBoletaAndAnuladoFalseAndIdNot("B-9", 1L)).thenReturn(false);
        when(repo.save(r)).thenReturn(r);

        assertThat(service.restaurar(1L).getAnulado()).isFalse();
    }

    @Test
    void restaurarConBoletaOcupadaLanza409() {
        when(repo.findById(1L)).thenReturn(Optional.of(registro(1L, true, "B-9")));
        when(repo.existsByCodigoBoletaAndAnuladoFalseAndIdNot("B-9", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.restaurar(1L)).satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
    }

    @Test
    void restaurarRegistroVigenteLanza409() {
        when(repo.findById(1L)).thenReturn(Optional.of(registro(1L, false, null)));
        assertThatThrownBy(() -> service.restaurar(1L)).satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
    }
}
