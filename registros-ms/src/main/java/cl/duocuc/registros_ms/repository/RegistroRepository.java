package cl.duocuc.registros_ms.repository;

import cl.duocuc.registros_ms.model.Registro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RegistroRepository extends JpaRepository<Registro, Long> {

    // Filtros opcionales: si un parámetro llega null, no filtra por él.
    @Query("""
            SELECT r FROM Registro r
            WHERE (:usuarioId IS NULL OR r.usuarioId = :usuarioId)
              AND (:turnoId IS NULL OR r.turnoId = :turnoId)
              AND (:fecha IS NULL OR r.fecha = :fecha)
              AND (:incluirAnulados = true OR r.anulado = false)
            ORDER BY r.id DESC
            """)
    List<Registro> buscar(@Param("usuarioId") Long usuarioId,
                          @Param("turnoId") Long turnoId,
                          @Param("fecha") LocalDate fecha,
                          @Param("incluirAnulados") boolean incluirAnulados);

    // Una boleta solo puede estar en UN registro vigente (si ese registro se anula, se puede volver a usar).
    boolean existsByCodigoBoletaAndAnuladoFalse(String codigoBoleta);

    boolean existsByCodigoBoletaAndAnuladoFalseAndIdNot(String codigoBoleta, Long id);

    @Query("""
            SELECT COUNT(r) FROM Registro r
            WHERE r.fecha = :fecha AND r.anulado = false AND r.tipoRegistro = :tipo
              AND (:usuarioId IS NULL OR r.usuarioId = :usuarioId)
            """)
    long contar(@Param("fecha") LocalDate fecha,
                @Param("tipo") String tipo,
                @Param("usuarioId") Long usuarioId);
}
