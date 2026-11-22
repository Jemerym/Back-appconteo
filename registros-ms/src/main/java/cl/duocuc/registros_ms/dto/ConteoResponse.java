package cl.duocuc.registros_ms.dto;

/** Totales de un día (solo registros vigentes, sin anulados). */
public record ConteoResponse(
        String fecha,
        long clientes,
        long transito,
        long total
) {}    
