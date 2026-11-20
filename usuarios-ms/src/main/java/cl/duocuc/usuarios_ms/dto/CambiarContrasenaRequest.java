package cl.duocuc.usuarios_ms.dto;

public record CambiarContrasenaRequest(
    String actual, 
    String nueva
) {}
