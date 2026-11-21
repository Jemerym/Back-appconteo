package cl.duocuc.usuarios_ms.controller;

import cl.duocuc.usuarios_ms.dto.ActualizarUsuarioRequest;
import cl.duocuc.usuarios_ms.dto.CambiarContrasenaRequest;
import cl.duocuc.usuarios_ms.dto.LoginRequest;
import cl.duocuc.usuarios_ms.dto.RecuperarClaveRequest;
import cl.duocuc.usuarios_ms.dto.RegistroRequest;
import cl.duocuc.usuarios_ms.dto.UsuarioResponse;
import cl.duocuc.usuarios_ms.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(required = false) String buscar,
                                        @RequestParam(defaultValue = "false") boolean incluirAnulados) {
        return usuarioService.listar(buscar, incluirAnulados);
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@RequestBody RegistroRequest datos) {
        return usuarioService.registrar(datos);
    }

    @PostMapping("/login")
    public UsuarioResponse iniciarSesion(@RequestBody LoginRequest datos) {
        return usuarioService.iniciarSesion(datos);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @RequestBody ActualizarUsuarioRequest datos) {
        return usuarioService.actualizar(id, datos);
    }

    @PutMapping("/{id}/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarContrasena(@PathVariable Long id, @RequestBody CambiarContrasenaRequest datos) {
        usuarioService.cambiarContrasena(id, datos);
    }

    @PostMapping("/recuperar-clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recuperarClave(@RequestBody RecuperarClaveRequest datos) {
        usuarioService.recuperarClave(datos);
    }

    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UsuarioResponse subirAvatar(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo) {
        return usuarioService.subirAvatar(id, archivo);
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<byte[]> obtenerAvatar(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(usuarioService.obtenerAvatar(id));
    }

    // Ya no se elimina: se ANULA y se puede RESTAURAR (se conserva el registro del usuario).
    @PutMapping("/{id}/anular")
    public UsuarioResponse anular(@PathVariable Long id) {
        return usuarioService.anular(id);
    }

    @PutMapping("/{id}/restaurar")
    public UsuarioResponse restaurar(@PathVariable Long id) {
        return usuarioService.restaurar(id);
    }
}
