package cl.duocuc.turnos_ms.repository;
import cl.duocuc.turnos_ms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Long> {

    List<Turno> findAllByOrderByIdDesc();

    List<Turno> findByUsuarioIdOrderByIdDesc(Long usuarioId);

    List<Turno> findByUsuarioIdAndAnuladoFalse(Long usuarioId);

    // Misma regla que haySolapamientoTurno del DbHelper:
    // mismo día, tramos que se cruzan y mismo observador O mismo acceso (sin contar el propio turno).
    // Los turnos anulados no ocupan el tramo.
    @Query("""
            SELECT COUNT(t) FROM Turno t
            WHERE t.fecha = :fecha
              AND t.horaInicio < :fin AND t.horaFin > :inicio
              AND (t.usuarioId = :usuarioId OR t.acceso = :acceso)
              AND t.id <> :excluirId
              AND t.anulado = false
            """)
    long contarSolapamientos(@Param("fecha") LocalDate fecha,
                             @Param("inicio") LocalTime inicio,
                             @Param("fin") LocalTime fin,
                             @Param("usuarioId") Long usuarioId,
                             @Param("acceso") Integer acceso,
                             @Param("excluirId") Long excluirId);

    // Equivale a obtenerTurnoActualObjeto: el turno del observador que cubre la hora indicada.
    @Query("""
            SELECT t FROM Turno t
            WHERE t.usuarioId = :usuarioId AND t.fecha = :fecha
              AND :hora >= t.horaInicio AND :hora < t.horaFin
              AND t.anulado = false
            """)
    List<Turno> buscarActivos(@Param("usuarioId") Long usuarioId,
                              @Param("fecha") LocalDate fecha,
                              @Param("hora") LocalTime hora);
}
