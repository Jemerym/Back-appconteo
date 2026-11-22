package cl.duocuc.turnos_ms.dto;

public record TurnoRequest(
        Long usuarioId,
        Integer acceso,
        String horaInicio,   // "10:00"
        String horaFin,      // "10:30"
        String fecha         // "2026-11-19"
) {}
