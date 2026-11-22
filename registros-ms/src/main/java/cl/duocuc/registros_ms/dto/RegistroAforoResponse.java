package cl.duocuc.registros_ms.dto;

public record RegistroAforoResponse(
        Long id,
        Long usuarioId,
        String observadorNombre,
        Long turnoId,
        Integer acceso,
        String tipoRegistro,
        String tipoGrupo,
        String rangoEtario,
        String codigoBoleta,
        String fecha,     // "2026-11-21"
        String hora,      // "14:05"
        Boolean anulado
) {}
