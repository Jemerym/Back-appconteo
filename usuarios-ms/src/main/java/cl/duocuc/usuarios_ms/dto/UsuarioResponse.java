package cl.duocuc.usuarios_ms.dto;

public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        Long rolId,
        String rolNombre,
        String avatarUrl
) {
}
