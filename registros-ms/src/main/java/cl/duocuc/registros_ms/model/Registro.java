package cl.duocuc.registros_ms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.LocalTime;

/** Una persona contada por un observador (reemplaza la tabla historial_aforo de SQLite). */
@Entity
@Table(name = "registros")
@Getter @Setter @NoArgsConstructor
public class Registro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Solo ids: el usuario vive en usuarios-service y el turno en turnos-service (sin claves foráneas). */
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /** Copia del nombre del observador al momento de registrar (se muestra sin consultar a usuarios-service). */
    @Column(name = "observador_nombre", nullable = false, length = 100)
    private String observadorNombre;

    @Column(name = "turno_id")
    private Long turnoId;

    @Column(nullable = false)
    private Integer acceso;

    /** "Cliente" o "Tránsito" */
    @Column(name = "tipo_registro", nullable = false, length = 30)
    private String tipoRegistro;

    @Column(name = "tipo_grupo", nullable = false, length = 30)
    private String tipoGrupo;

    @Column(name = "rango_etario", nullable = false, length = 20)
    private String rangoEtario;

    @Column(name = "codigo_boleta", length = 100)
    private String codigoBoleta;

    /** Fecha y hora las pone el SERVIDOR al crear el registro (hora de Chile). */
    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    // ANULAR en vez de eliminar: el registro se conserva, deja de contar y se puede restaurar.
    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean anulado = false;
}
