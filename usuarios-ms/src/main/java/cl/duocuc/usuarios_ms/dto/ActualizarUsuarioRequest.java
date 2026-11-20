package cl.duocuc.usuarios_ms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ActualizarUsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo inválido") String correo,
        
        Long rolId   // opcional: si viene null se mantiene el rol actual
) {}
