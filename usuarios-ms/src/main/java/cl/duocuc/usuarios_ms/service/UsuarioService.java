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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Long ROL_ADMINISTRADOR = 1L;
    private static final Long ROL_OBSERVADOR = 2L;
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UsuarioRepository usuarioRepository;
    private final RolClient rolClient;
    private final AlmacenamientoService almacenamientoService;
    private final PasswordEncoder passwordEncoder;

    // incluirAnulados = false -> solo usuarios activos (lo normal); true -> todos (pantalla de administración)
    public List<UsuarioResponse> listar(String buscar, boolean incluirAnulados) {
        String texto = buscar == null ? "" : buscar.trim();
        List<Usuario> usuarios = (texto.isEmpty()
                ? usuarioRepository.findAllByOrderByNombreAsc()
                : usuarioRepository.findByNombreContainingIgnoreCaseOrCorreoContainingIgnoreCaseOrderByNombreAsc(texto, texto))
                .stream()
                .filter(u -> incluirAnulados || estaActivo(u))
                .toList();

        Map<Long, String> roles = rolClient.listar().stream()
                .collect(Collectors.toMap(RolDto::id, RolDto::nombre));

        return usuarios.stream()
                .map(usuario -> aRespuesta(usuario, roles.getOrDefault(usuario.getRolId(), "")))
                .toList();
    }

    public UsuarioResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    public UsuarioResponse registrar(RegistroRequest datos) {
        if (vacio(datos.nombre()) || vacio(datos.correo()) || vacio(datos.contrasena()) || vacio(datos.palabraSecreta())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nombre, correo, contraseña y palabra secreta son obligatorios");
        }
        String correo = normalizar(datos.correo());
        validarCorreo(correo);
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        validarContrasena(datos.contrasena());

        // SEGURIDAD: el registro es público, así que SIEMPRE crea un Observador.
        // Se ignora datos.rolId(): si no, cualquiera podría registrarse como Administrador.
        Long rolId = ROL_OBSERVADOR;

        Usuario nuevo = Usuario.builder()
                .nombre(datos.nombre().trim())
                .correo(correo)
                .contrasenaHash(passwordEncoder.encode(datos.contrasena()))
                .palabraSecretaHash(passwordEncoder.encode(normalizar(datos.palabraSecreta())))
                .rolId(rolId)
                .build();
        return aRespuesta(usuarioRepository.save(nuevo));
    }

    public UsuarioResponse iniciarSesion(LoginRequest datos) {
        if (vacio(datos.correo()) || vacio(datos.contrasena())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correo y contraseña son obligatorios");
        }
        // Mismo mensaje si falla el correo o la clave: no se revela cuál de los dos falló.
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(normalizar(datos.correo()))
                .filter(u -> passwordEncoder.matches(datos.contrasena(), u.getContrasenaHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));
        // Se revisa DESPUÉS de validar la clave: así no se revela a un extraño qué cuentas están anuladas.
        validarActivo(usuario);
        return aRespuesta(usuario);
    }

    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest datos) {
        Usuario usuario = buscar(id);

        if (vacio(datos.nombre()) || vacio(datos.correo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre y correo son obligatorios");
        }
        String correo = normalizar(datos.correo());
        validarCorreo(correo);
        if (usuarioRepository.existsByCorreoIgnoreCaseAndIdNot(correo, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        if (datos.rolId() != null && !datos.rolId().equals(usuario.getRolId())) {
            validarRol(datos.rolId());
            if (esUltimoAdministrador(usuario)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Debe existir al menos un administrador");
            }
            usuario.setRolId(datos.rolId());
        }

        usuario.setNombre(datos.nombre().trim());
        usuario.setCorreo(correo);
        return aRespuesta(usuarioRepository.save(usuario));
    }

    public void cambiarContrasena(Long id, CambiarContrasenaRequest datos) {
        Usuario usuario = buscar(id);

        if (vacio(datos.actual()) || vacio(datos.nueva())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual y la nueva son obligatorias");
        }
        if (!passwordEncoder.matches(datos.actual(), usuario.getContrasenaHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta");
        }
        validarContrasena(datos.nueva());

        usuario.setContrasenaHash(passwordEncoder.encode(datos.nueva()));
        usuarioRepository.save(usuario);
    }

    public void recuperarClave(RecuperarClaveRequest datos) {
        if (vacio(datos.correo()) || vacio(datos.palabraSecreta()) || vacio(datos.nuevaClave())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Correo, palabra secreta y contraseña nueva son obligatorios");
        }
        // Mensaje genérico: no se revela si el correo existe o si falló la palabra secreta.
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(normalizar(datos.correo()))
                .filter(u -> passwordEncoder.matches(normalizar(datos.palabraSecreta()), u.getPalabraSecretaHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correo o palabra secreta incorrectos"));
        validarActivo(usuario);
        validarContrasena(datos.nuevaClave());

        usuario.setContrasenaHash(passwordEncoder.encode(datos.nuevaClave()));
        usuarioRepository.save(usuario);
    }

    public UsuarioResponse subirAvatar(Long id, MultipartFile archivo) {
        Usuario usuario = buscar(id);
        String anterior = usuario.getAvatar();

        usuario.setAvatar(almacenamientoService.guardar(archivo));
        Usuario guardado = usuarioRepository.save(usuario);

        almacenamientoService.eliminar(anterior);
        return aRespuesta(guardado);
    }

    public byte[] obtenerAvatar(Long id) {
        Usuario usuario = buscar(id);
        if (usuario.getAvatar() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no tiene foto");
        }
        return almacenamientoService.leer(usuario.getAvatar());
    }

    // ANULAR en vez de eliminar: el usuario queda guardado (con su foto y su historial)
    // pero no puede iniciar sesión hasta que un administrador lo restaure.
    public UsuarioResponse anular(Long id) {
        Usuario usuario = buscar(id);
        if (!estaActivo(usuario)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya está anulado");
        }
        if (esUltimoAdministrador(usuario)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede anular al último administrador");
        }
        usuario.setActivo(false);
        return aRespuesta(usuarioRepository.save(usuario));
    }

    public UsuarioResponse restaurar(Long id) {
        Usuario usuario = buscar(id);
        if (estaActivo(usuario)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya está activo");
        }
        usuario.setActivo(true);
        return aRespuesta(usuarioRepository.save(usuario));
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private void validarRol(Long rolId) {
        if (rolClient.obtener(rolId) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol indicado no existe");
        }
    }

    // No se puede dejar el sistema sin administradores ACTIVOS.
    private boolean esUltimoAdministrador(Usuario usuario) {
        return ROL_ADMINISTRADOR.equals(usuario.getRolId())
                && estaActivo(usuario)
                && usuarioRepository.countByRolIdAndActivoTrue(ROL_ADMINISTRADOR) <= 1;
    }

    // null cuenta como activo (filas creadas antes de existir la columna)
    private boolean estaActivo(Usuario usuario) {
        return !Boolean.FALSE.equals(usuario.getActivo());
    }

    private void validarActivo(Usuario usuario) {
        if (!estaActivo(usuario)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario anulado, contacta al administrador");
        }
    }

    private void validarCorreo(String correo) {
        if (!CORREO.matcher(correo).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correo inválido");
        }
    }

    // Misma política que tiene la app en Validaciones.kt, pero aplicada en el servidor.
    private void validarContrasena(String clave) {
        if (clave == null || clave.length() < 8) {
            throw errorClave("La contraseña debe tener mínimo 8 caracteres");
        }
        if (clave.chars().noneMatch(Character::isUpperCase)) {
            throw errorClave("La contraseña debe tener una mayúscula");
        }
        if (clave.chars().noneMatch(Character::isLowerCase)) {
            throw errorClave("La contraseña debe tener una minúscula");
        }
        if (clave.chars().noneMatch(Character::isDigit)) {
            throw errorClave("La contraseña debe tener un número");
        }
        if (clave.chars().allMatch(Character::isLetterOrDigit)) {
            throw errorClave("La contraseña debe tener un carácter especial (ej: ! # $ %)");
        }
    }

    private ResponseStatusException errorClave(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }

    private UsuarioResponse aRespuesta(Usuario usuario) {
        RolDto rol = rolClient.obtener(usuario.getRolId());
        return aRespuesta(usuario, rol != null ? rol.nombre() : "");
    }

    private UsuarioResponse aRespuesta(Usuario usuario, String rolNombre) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRolId(),
                rolNombre,
                urlAvatar(usuario),
                estaActivo(usuario)
        );
    }

    private String urlAvatar(Usuario usuario) {
        if (usuario.getAvatar() == null) {
            return null;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/usuarios/" + usuario.getId() + "/avatar")
                .queryParam("v", usuario.getAvatar())
                .toUriString();
    }

    private String normalizar(String texto) {
        return texto.trim().toLowerCase();
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
