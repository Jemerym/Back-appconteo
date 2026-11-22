package cl.duocuc.turnos_ms.service;

import cl.duocuc.turnos_ms.client.*;
import cl.duocuc.turnos_ms.dto.TurnoRequest;
import cl.duocuc.turnos_ms.model.Turno;
import cl.duocuc.turnos_ms.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int ACCESO_MIN = 1;
    private static final int ACCESO_MAX = 4;
    private static final long ROL_OBSERVADOR = 2L;

    private final TurnoRepository repo;
    private final UsuarioClient usuarios;

    // ---------- consultas ----------

    @Transactional(readOnly = true)
    public List<Turno> listar(Long usuarioId) {
        return usuarioId == null
                ? repo.findAllByOrderByIdDesc()
                : repo.findByUsuarioIdOrderByIdDesc(usuarioId);
    }

    @Transactional(readOnly = true)
    public Turno obtener(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
    }

    /** Turno que cubre la hora actual del servidor (hora de Chile). Vacío si no tiene turno activo. */
    @Transactional(readOnly = true)
    public Optional<Turno> obtenerActual(Long usuarioId) {
        LocalDateTime ahora = LocalDateTime.now(ZONA);
        return repo.buscarActivos(usuarioId, ahora.toLocalDate(), ahora.toLocalTime()).stream().findFirst();
    }

    // ---------- modificaciones ----------

    @Transactional
    public Turno crear(TurnoRequest req) {
        return guardar(new Turno(), req);
    }

    @Transactional
    public Turno actualizar(Long id, TurnoRequest req) {
        Turno turno = obtener(id);
        if (estaAnulado(turno))
            throw error(HttpStatus.CONFLICT, "El turno está anulado: restáuralo antes de modificarlo");
        return guardar(turno, req);
    }

    /** ANULAR en vez de eliminar: el turno queda guardado y se puede restaurar. */
    @Transactional
    public Turno anular(Long id) {
        Turno turno = obtener(id);
        if (estaAnulado(turno))
            throw error(HttpStatus.CONFLICT, "El turno ya está anulado");
        turno.setAnulado(true);
        return repo.save(turno);
    }

    /** Restaurar vuelve a ocupar el tramo, así que se revisa que nadie lo haya tomado mientras estaba anulado. */
    @Transactional
    public Turno restaurar(Long id) {
        Turno turno = obtener(id);
        if (!estaAnulado(turno))
            throw error(HttpStatus.CONFLICT, "El turno ya está activo");
        if (repo.contarSolapamientos(turno.getFecha(), turno.getHoraInicio(), turno.getHoraFin(),
                turno.getUsuarioId(), turno.getAcceso(), turno.getId()) > 0)
            throw error(HttpStatus.CONFLICT,
                    "Ya hay un turno vigente con esos datos");
        turno.setAnulado(false);
        return repo.save(turno);
    }

    /** Al anular un usuario se anulan sus turnos (antes se borraban con ON DELETE CASCADE). */
    @Transactional
    public void anularDeUsuario(Long usuarioId) {
        List<Turno> turnos = repo.findByUsuarioIdAndAnuladoFalse(usuarioId);
        turnos.forEach(t -> t.setAnulado(true));
        repo.saveAll(turnos);
    }

    // null cuenta como activo (filas creadas antes de existir la columna)
    private boolean estaAnulado(Turno turno) {
        return Boolean.TRUE.equals(turno.getAnulado());
    }

    // ---------- reglas de negocio ----------

    private Turno guardar(Turno turno, TurnoRequest req) {
        if (req.usuarioId() == null)
            throw error(HttpStatus.BAD_REQUEST, "Selecciona un observador");
        if (req.acceso() == null || req.acceso() < ACCESO_MIN || req.acceso() > ACCESO_MAX)
            throw error(HttpStatus.BAD_REQUEST, "El acceso debe estar entre 1 y 4");

        LocalDate fecha = leerFecha(req.fecha());
        LocalTime inicio = leerHora(req.horaInicio());
        LocalTime fin = leerHora(req.horaFin());

        if (!inicio.isBefore(fin))
            throw error(HttpStatus.BAD_REQUEST, "La hora de inicio debe ser anterior a la hora de fin");
        if (fecha.isBefore(LocalDate.now(ZONA)))
            throw error(HttpStatus.BAD_REQUEST, "No se puede programar un turno en una fecha pasada");

        // El observador debe existir en usuarios-service (de ahí sale también su nombre).
        UsuarioRemoto observador = usuarios.obtener(req.usuarioId());
        if (observador.rolId() == null || observador.rolId() != ROL_OBSERVADOR)
            throw error(HttpStatus.BAD_REQUEST, "Solo se pueden asignar turnos a observadores");
        if (Boolean.FALSE.equals(observador.activo()))
            throw error(HttpStatus.BAD_REQUEST, "El observador está anulado");

        // Al editar se excluye el propio turno; al crear, 0 no coincide con ningún id.
        Long excluirId = turno.getId() == null ? 0L : turno.getId();
        if (repo.contarSolapamientos(fecha, inicio, fin, req.usuarioId(), req.acceso(), excluirId) > 0)
            throw error(HttpStatus.CONFLICT,
                    "Conflicto: El guardia o puerta ya tiene un turno");

        turno.setUsuarioId(observador.id());
        turno.setObservadorNombre(observador.nombre());
        turno.setAcceso(req.acceso());
        turno.setFecha(fecha);
        turno.setHoraInicio(inicio);
        turno.setHoraFin(fin);
        return repo.save(turno);
    }

    private LocalDate leerFecha(String texto) {
        try {
            return LocalDate.parse(texto);   // yyyy-MM-dd
        } catch (DateTimeParseException | NullPointerException e) {
            throw error(HttpStatus.BAD_REQUEST, "La fecha debe tener el formato yyyy-MM-dd");
        }
    }

    private LocalTime leerHora(String texto) {
        try {
            return LocalTime.parse(texto, HORA);
        } catch (DateTimeParseException | NullPointerException e) {
            throw error(HttpStatus.BAD_REQUEST, "La hora debe tener el formato HH:mm");
        }
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}
