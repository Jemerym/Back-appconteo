package cl.duocuc.usuarios_ms.service;

import cl.duocuc.usuarios_ms.client.RolClient;
import cl.duocuc.usuarios_ms.dto.ActualizarUsuarioRequest;
import cl.duocuc.usuarios_ms.dto.CambiarContrasenaRequest;
import cl.duocuc.usuarios_ms.dto.LoginRequest;
import cl.duocuc.usuarios_ms.dto.RecuperarClaveRequest;
import cl.duocuc.usuarios_ms.dto.RegistroRequest;
import cl.duocuc.usuarios_ms.dto.RolDto;
import cl.duocuc.usuarios_ms.dto.UsuarioResponse;
import cl.duocuc.usuarios_ms.model.Usuario;
import cl.duocuc.usuarios_ms.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repo;
    @Mock
    private RolClient rolClient;
    @Mock
    private AlmacenamientoService almacenamiento;
    @Mock
    private PasswordEncoder encoder;

    private UsuarioService service;

    private static final RolDto ADMIN = new RolDto(1L, "Administrador");
    private static final RolDto OBSERVADOR = new RolDto(2L, "Observador");
    private static final String CLAVE_OK = "Clave123!";

    @BeforeEach
    void setUp() {
        service = new UsuarioService(repo, rolClient, almacenamiento, encoder);
        when(rolClient.obtener(1L)).thenReturn(ADMIN);
        when(rolClient.obtener(2L)).thenReturn(OBSERVADOR);
        when(encoder.encode(anyString())).thenAnswer(inv -> "hash:" + inv.getArgument(0));
        when(encoder.matches(anyString(), anyString()))
                .thenAnswer(inv -> ("hash:" + inv.getArgument(0)).equals(inv.getArgument(1)));
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        // urlAvatar() arma la URL a partir de la petición actual
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static Usuario usuario(Long id, Long rolId, Boolean activo) {
        return Usuario.builder()
                .id(id)
                .nombre("Camila")
                .correo("camila@duocuc.cl")
                .contrasenaHash("hash:" + CLAVE_OK)
                .palabraSecretaHash("hash:duoc")
                .rolId(rolId)
                .activo(activo)
                .build();
    }

    private static void assertStatus(Throwable e, HttpStatus estado) {
        assertThat(e).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(estado);
    }

    // ---------- listar / obtener ----------

    @Test
    void listarSinBusquedaFiltraAnulados() {
        Usuario activo = usuario(1L, 1L, true);
        Usuario anulado = usuario(2L, 2L, false);
        Usuario sinRolConocido = usuario(3L, 9L, null);
        when(repo.findAllByOrderByNombreAsc()).thenReturn(List.of(activo, anulado, sinRolConocido));
        when(rolClient.listar()).thenReturn(List.of(ADMIN, OBSERVADOR));

        List<UsuarioResponse> lista = service.listar(null, false);

        assertThat(lista).extracting(UsuarioResponse::id).containsExactly(1L, 3L);
        assertThat(lista.get(0).rolNombre()).isEqualTo("Administrador");
        assertThat(lista.get(1).rolNombre()).isEmpty();
        assertThat(lista.get(1).activo()).isTrue();
    }

    @Test
    void listarConBusquedaIncluyendoAnulados() {
        when(repo.findByNombreContainingIgnoreCaseOrCorreoContainingIgnoreCaseOrderByNombreAsc("cam", "cam"))
                .thenReturn(List.of(usuario(1L, 2L, true), usuario(2L, 2L, false)));
        when(rolClient.listar()).thenReturn(List.of(OBSERVADOR));

        assertThat(service.listar("  cam ", true)).hasSize(2);
        assertThat(service.listar("   ", true)).isEmpty();
    }

    @Test
    void obtenerConAvatarArmaLaUrl() {
        Usuario u = usuario(5L, 2L, true);
        u.setAvatar("foto.jpg");
        when(repo.findById(5L)).thenReturn(Optional.of(u));

        UsuarioResponse r = service.obtener(5L);

        assertThat(r.avatarUrl()).isEqualTo("http://localhost/api/usuarios/5/avatar?v=foto.jpg");
        assertThat(r.rolNombre()).isEqualTo("Observador");
    }

    @Test
    void obtenerConRolInexistenteDejaNombreVacio() {
        when(repo.findById(5L)).thenReturn(Optional.of(usuario(5L, 7L, true)));
        when(rolClient.obtener(7L)).thenReturn(null);

        UsuarioResponse r = service.obtener(5L);
        assertThat(r.rolNombre()).isEmpty();
        assertThat(r.avatarUrl()).isNull();
    }

    @Test
    void obtenerInexistenteLanza404() {
        when(repo.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtener(5L)).satisfies(e -> assertStatus(e, HttpStatus.NOT_FOUND));
    }

    // ---------- registrar ----------

    @Test
    void registrarSiempreCreaObservador() {
        when(repo.existsByCorreoIgnoreCase("nuevo@duocuc.cl")).thenReturn(false);

        UsuarioResponse r = service.registrar(new RegistroRequest(" Nuevo ", " Nuevo@DuocUC.cl ", CLAVE_OK, " Duoc ", 1L));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Nuevo");
        assertThat(guardado.getCorreo()).isEqualTo("nuevo@duocuc.cl");
        assertThat(guardado.getContrasenaHash()).isEqualTo("hash:" + CLAVE_OK);
        assertThat(guardado.getPalabraSecretaHash()).isEqualTo("hash:duoc");
        assertThat(guardado.getRolId()).isEqualTo(2L);
        assertThat(r.rolNombre()).isEqualTo("Observador");
    }

    @Test
    void registrarValidaCamposObligatorios() {
        List<RegistroRequest> invalidos = List.of(
                new RegistroRequest(null, "a@b.cl", CLAVE_OK, "duoc", null),
                new RegistroRequest("A", " ", CLAVE_OK, "duoc", null),
                new RegistroRequest("A", "a@b.cl", null, "duoc", null),
                new RegistroRequest("A", "a@b.cl", CLAVE_OK, "", null),
                new RegistroRequest("A", "no-es-correo", CLAVE_OK, "duoc", null)
        );
        for (RegistroRequest req : invalidos) {
            assertThatThrownBy(() -> service.registrar(req)).satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        }
    }

    @Test
    void registrarCorreoRepetidoLanza409() {
        when(repo.existsByCorreoIgnoreCase("a@b.cl")).thenReturn(true);
        assertThatThrownBy(() -> service.registrar(new RegistroRequest("A", "a@b.cl", CLAVE_OK, "duoc", null)))
                .satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
    }

    @Test
    void registrarValidaLaPoliticaDeContrasena() {
        for (String clave : List.of("Ab1!", "abcdefg1!", "ABCDEFG1!", "Abcdefgh!", "Abcdefg12")) {
            assertThatThrownBy(() -> service.registrar(new RegistroRequest("A", "a@b.cl", clave, "duoc", null)))
                    .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        }
        verify(repo, never()).save(any());
    }

    // ---------- login ----------

    @Test
    void iniciarSesionCorrecto() {
        when(repo.findByCorreoIgnoreCase("camila@duocuc.cl")).thenReturn(Optional.of(usuario(1L, 2L, true)));

        UsuarioResponse r = service.iniciarSesion(new LoginRequest(" Camila@DuocUC.cl ", CLAVE_OK));
        assertThat(r.id()).isEqualTo(1L);
    }

    @Test
    void iniciarSesionFallido() {
        when(repo.findByCorreoIgnoreCase("camila@duocuc.cl")).thenReturn(Optional.of(usuario(1L, 2L, true)));
        when(repo.findByCorreoIgnoreCase("nadie@duocuc.cl")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.iniciarSesion(new LoginRequest(null, CLAVE_OK)))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.iniciarSesion(new LoginRequest("camila@duocuc.cl", " ")))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.iniciarSesion(new LoginRequest("camila@duocuc.cl", "otra")))
                .satisfies(e -> assertStatus(e, HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> service.iniciarSesion(new LoginRequest("nadie@duocuc.cl", CLAVE_OK)))
                .satisfies(e -> assertStatus(e, HttpStatus.UNAUTHORIZED));
    }

    @Test
    void iniciarSesionUsuarioAnuladoLanza401() {
        when(repo.findByCorreoIgnoreCase("camila@duocuc.cl")).thenReturn(Optional.of(usuario(1L, 2L, false)));
        assertThatThrownBy(() -> service.iniciarSesion(new LoginRequest("camila@duocuc.cl", CLAVE_OK)))
                .satisfies(e -> assertStatus(e, HttpStatus.UNAUTHORIZED));
    }

    // ---------- actualizar ----------

    @Test
    void actualizarDatosSinCambiarRol() {
        Usuario u = usuario(1L, 2L, true);
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        when(repo.existsByCorreoIgnoreCaseAndIdNot("nuevo@duocuc.cl", 1L)).thenReturn(false);

        UsuarioResponse r = service.actualizar(1L, new ActualizarUsuarioRequest(" Cami ", "Nuevo@duocuc.cl", null));
        assertThat(r.nombre()).isEqualTo("Cami");
        assertThat(r.correo()).isEqualTo("nuevo@duocuc.cl");

        service.actualizar(1L, new ActualizarUsuarioRequest("Cami", "nuevo@duocuc.cl", 2L));
        verify(rolClient, never()).obtener(3L);
    }

    @Test
    void actualizarCambiaElRol() {
        Usuario u = usuario(1L, 2L, true);
        when(repo.findById(1L)).thenReturn(Optional.of(u));

        UsuarioResponse r = service.actualizar(1L, new ActualizarUsuarioRequest("Cami", "cami@duocuc.cl", 1L));
        assertThat(r.rolId()).isEqualTo(1L);
    }

    @Test
    void actualizarAdminConVariosAdminsPuedeCambiarRol() {
        Usuario u = usuario(1L, 1L, true);
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        when(repo.countByRolIdAndActivoTrue(1L)).thenReturn(2L);

        assertThat(service.actualizar(1L, new ActualizarUsuarioRequest("A", "a@b.cl", 2L)).rolId()).isEqualTo(2L);
    }

    @Test
    void actualizarConErrores() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuario(1L, 1L, true)));
        when(repo.existsByCorreoIgnoreCaseAndIdNot("usado@b.cl", 1L)).thenReturn(true);
        when(repo.countByRolIdAndActivoTrue(1L)).thenReturn(1L);
        when(rolClient.obtener(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.actualizar(1L, new ActualizarUsuarioRequest(" ", "a@b.cl", null)))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.actualizar(1L, new ActualizarUsuarioRequest("A", null, null)))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.actualizar(1L, new ActualizarUsuarioRequest("A", "malo", null)))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.actualizar(1L, new ActualizarUsuarioRequest("A", "usado@b.cl", null)))
                .satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
        assertThatThrownBy(() -> service.actualizar(1L, new ActualizarUsuarioRequest("A", "a@b.cl", 9L)))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        // último administrador: no puede dejar de serlo
        assertThatThrownBy(() -> service.actualizar(1L, new ActualizarUsuarioRequest("A", "a@b.cl", 2L)))
                .satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
    }

    // ---------- contraseñas ----------

    @Test
    void cambiarContrasenaCorrecta() {
        Usuario u = usuario(1L, 2L, true);
        when(repo.findById(1L)).thenReturn(Optional.of(u));

        service.cambiarContrasena(1L, new CambiarContrasenaRequest(CLAVE_OK, "Nueva123#"));

        assertThat(u.getContrasenaHash()).isEqualTo("hash:Nueva123#");
        verify(repo).save(u);
    }

    @Test
    void cambiarContrasenaConErrores() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuario(1L, 2L, true)));

        assertThatThrownBy(() -> service.cambiarContrasena(1L, new CambiarContrasenaRequest(null, "Nueva123#")))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.cambiarContrasena(1L, new CambiarContrasenaRequest(CLAVE_OK, "")))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.cambiarContrasena(1L, new CambiarContrasenaRequest("mala", "Nueva123#")))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.cambiarContrasena(1L, new CambiarContrasenaRequest(CLAVE_OK, "debil")))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        verify(repo, never()).save(any());
    }

    @Test
    void recuperarClaveCorrecta() {
        Usuario u = usuario(1L, 2L, true);
        when(repo.findByCorreoIgnoreCase("camila@duocuc.cl")).thenReturn(Optional.of(u));

        service.recuperarClave(new RecuperarClaveRequest("Camila@duocuc.cl", " DUOC ", "Nueva123#"));

        assertThat(u.getContrasenaHash()).isEqualTo("hash:Nueva123#");
        verify(repo).save(u);
    }

    @Test
    void recuperarClaveConErrores() {
        when(repo.findByCorreoIgnoreCase("camila@duocuc.cl")).thenReturn(Optional.of(usuario(1L, 2L, true)));
        when(repo.findByCorreoIgnoreCase("anulado@duocuc.cl")).thenReturn(Optional.of(usuario(2L, 2L, false)));

        assertThatThrownBy(() -> service.recuperarClave(new RecuperarClaveRequest(null, "duoc", "Nueva123#")))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.recuperarClave(new RecuperarClaveRequest("camila@duocuc.cl", " ", "Nueva123#")))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.recuperarClave(new RecuperarClaveRequest("camila@duocuc.cl", "duoc", null)))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.recuperarClave(new RecuperarClaveRequest("camila@duocuc.cl", "otra", "Nueva123#")))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.recuperarClave(new RecuperarClaveRequest("camila@duocuc.cl", "duoc", "debil")))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.recuperarClave(new RecuperarClaveRequest("anulado@duocuc.cl", "duoc", "Nueva123#")))
                .satisfies(e -> assertStatus(e, HttpStatus.UNAUTHORIZED));
        verify(repo, never()).save(any());
    }

    // ---------- avatar ----------

    @Test
    void subirAvatarReemplazaElAnterior() {
        Usuario u = usuario(1L, 2L, true);
        u.setAvatar("vieja.jpg");
        MockMultipartFile archivo = new MockMultipartFile("archivo", "f.jpg", "image/jpeg", new byte[]{1});
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        when(almacenamiento.guardar(archivo)).thenReturn("nueva.jpg");

        UsuarioResponse r = service.subirAvatar(1L, archivo);

        assertThat(r.avatarUrl()).endsWith("/api/usuarios/1/avatar?v=nueva.jpg");
        verify(almacenamiento).eliminar("vieja.jpg");
    }

    @Test
    void obtenerAvatar() {
        Usuario u = usuario(1L, 2L, true);
        u.setAvatar("foto.jpg");
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        when(almacenamiento.leer("foto.jpg")).thenReturn(new byte[]{1, 2, 3});

        assertThat(service.obtenerAvatar(1L)).containsExactly(1, 2, 3);
    }

    @Test
    void obtenerAvatarSinFotoLanza404() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuario(1L, 2L, true)));
        assertThatThrownBy(() -> service.obtenerAvatar(1L)).satisfies(e -> assertStatus(e, HttpStatus.NOT_FOUND));
    }

    // ---------- anular / restaurar ----------

    @Test
    void anularObservador() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuario(1L, 2L, true)));
        assertThat(service.anular(1L).activo()).isFalse();
    }

    @Test
    void anularAdminConOtrosAdmins() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuario(1L, 1L, true)));
        when(repo.countByRolIdAndActivoTrue(1L)).thenReturn(3L);
        assertThat(service.anular(1L).activo()).isFalse();
    }

    @Test
    void anularConErrores() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuario(1L, 2L, false)));
        when(repo.findById(2L)).thenReturn(Optional.of(usuario(2L, 1L, true)));
        when(repo.countByRolIdAndActivoTrue(1L)).thenReturn(1L);

        assertThatThrownBy(() -> service.anular(1L)).satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
        assertThatThrownBy(() -> service.anular(2L)).satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
    }

    @Test
    void restaurar() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuario(1L, 2L, false)));
        when(repo.findById(2L)).thenReturn(Optional.of(usuario(2L, 2L, true)));

        assertThat(service.restaurar(1L).activo()).isTrue();
        assertThatThrownBy(() -> service.restaurar(2L)).satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
    }
}
