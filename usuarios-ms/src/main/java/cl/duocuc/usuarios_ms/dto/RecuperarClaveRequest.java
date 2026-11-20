package cl.duocuc.usuarios_ms.dto;

public record RecuperarClaveRequest(
    String correo, 
    String palabraSecreta, 
    String nuevaClave
) {}
