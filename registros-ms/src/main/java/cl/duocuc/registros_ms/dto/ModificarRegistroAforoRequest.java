package cl.duocuc.registros_ms.dto;

/** Lo que se puede corregir de un registro (igual que el diálogo de edición de la app). */
public record ModificarRegistroAforoRequest(
        Integer acceso,
        String tipoGrupo,
        String rangoEtario
) {}
