package cl.duocuc.turnos_ms.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "turnos")
@Getter @Setter @NoArgsConstructor
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Solo el id: el usuario vive en usuarios-service, no hay clave foránea. */
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /** Copia del nombre al momento de programar (se muestra sin consultar a usuarios-service). */
    @Column(name = "observador_nombre", nullable = false, length = 100)
    private String observadorNombre;

    @Column(nullable = false)
    private Integer acceso;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    private LocalDate fecha;

    // ANULAR en vez de eliminar: el turno se conserva (los registros lo siguen referenciando)
    // y se puede restaurar. Un turno anulado no ocupa el tramo ni cuenta como turno activo.
    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean anulado = false;
}
