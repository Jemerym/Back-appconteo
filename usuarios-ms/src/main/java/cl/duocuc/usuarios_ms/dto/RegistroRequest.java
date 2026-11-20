package cl.duocuc.usuarios_ms.dto;

public record RegistroRequest(
        String nombre,
        String correo,
        String contrasena,
        String palabraSecreta,
        Long rolId
) {
}
