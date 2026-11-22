package cl.duocuc.turnos_ms.dto;

public record TurnoResponse(
        Long id,
        Long usuarioId,
        String observadorNombre,
        Integer acceso,
        String horaInicio,
        String horaFin,
        String fecha,
        Boolean anulado
) {}
