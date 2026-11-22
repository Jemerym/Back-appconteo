package cl.duocuc.turnos_ms.client;

/** Solo los campos que turnos necesita de la respuesta de usuarios-service. */
public record UsuarioRemoto(Long id, String nombre, Long rolId, Boolean activo) {}
