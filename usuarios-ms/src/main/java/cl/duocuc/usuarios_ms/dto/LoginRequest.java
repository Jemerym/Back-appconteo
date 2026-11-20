package cl.duocuc.usuarios_ms.dto;

public record LoginRequest(
    String correo, 
    String contrasena
) {}