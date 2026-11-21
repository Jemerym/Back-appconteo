package cl.duocuc.usuarios_ms.repository;

import cl.duocuc.usuarios_ms.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    List<Usuario> findAllByOrderByNombreAsc();

    List<Usuario> findByNombreContainingIgnoreCaseOrCorreoContainingIgnoreCaseOrderByNombreAsc(String nombre, String correo);

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    // Solo cuentan los administradores ACTIVOS (uno anulado no administra nada).
    long countByRolIdAndActivoTrue(Long rolId);
}
