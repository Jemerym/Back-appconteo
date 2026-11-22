package cl.duocuc.registros_ms.dto;

/** Lo que envía la app al registrar una persona. La fecha y la hora las pone el servidor. */
public record RegistroAforoRequest(
        Long usuarioId,
        String observadorNombre,
        Long turnoId,
        Integer acceso,
        String tipoRegistro,     // "Cliente" o "Tránsito"
        String tipoGrupo,        // "Solo", "Acompañado (2)", "Grupo (3+)"
        String rangoEtario,      // "< 18", "18-24", "25-34", "35-50", "+ 50"
        String codigoBoleta      // solo clientes; null en tránsito
) {}
