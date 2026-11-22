package cl.duocuc.registros_ms.service;

import cl.duocuc.registros_ms.dto.*;

import cl.duocuc.registros_ms.model.Registro;
import cl.duocuc.registros_ms.repository.RegistroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RegistroService {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    private static final int ACCESO_MIN = 1;
    private static final int ACCESO_MAX = 4;

    // Mismos textos que usa la app (enums Clasificacion, TipoCompania y RangoEtario de InfoResgistros.kt)
    public static final String CLIENTE = "Cliente";
    public static final String TRANSITO = "Tránsito";
    private static final Set<String> TIPOS = Set.of(CLIENTE, TRANSITO);
    private static final Set<String> GRUPOS = Set.of("Solo", "Acompañado (2)", "Grupo (3+)");
    private static final Set<String> RANGOS = Set.of("< 18", "18-24", "25-34", "35-50", "+ 50");

    private final RegistroRepository repo;

    // ---------- consultas ----------

    @Transactional(readOnly = true)
    public List<Registro> listar(Long usuarioId, Long turnoId, String fecha, boolean incluirAnulados) {
        LocalDate dia = (fecha == null || fecha.isBlank()) ? null : leerFecha(fecha);
        return repo.buscar(usuarioId, turnoId, dia, incluirAnulados);
    }

    @Transactional(readOnly = true)
    public Registro obtener(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Registro no encontrado"));
    }

    /** Para avisar ANTES de abrir el diálogo si la boleta escaneada ya se usó. */
    @Transactional(readOnly = true)
    public boolean existeBoleta(String codigo) {
        return codigo != null && !codigo.isBlank() && repo.existsByCodigoBoletaAndAnuladoFalse(codigo.trim());
    }

    /** Totales del día (sin anulados). fecha null = hoy; usuarioId null = todos los observadores. */
    @Transactional(readOnly = true)
    public ConteoResponse conteo(String fecha, Long usuarioId) {
        LocalDate dia = (fecha == null || fecha.isBlank()) ? LocalDate.now(ZONA) : leerFecha(fecha);
        long clientes = repo.contar(dia, CLIENTE, usuarioId);
        long transito = repo.contar(dia, TRANSITO, usuarioId);
        return new ConteoResponse(dia.toString(), clientes, transito, clientes + transito);
    }

    // ---------- modificaciones ----------

    @Transactional
    public Registro crear(RegistroAforoRequest req) {
        if (req.usuarioId() == null)
            throw error(HttpStatus.BAD_REQUEST, "Falta el observador (usuarioId)");
        if (vacio(req.observadorNombre()))
            throw error(HttpStatus.BAD_REQUEST, "Falta el nombre del observador");
        validarAcceso(req.acceso());
        validarOpcion(req.tipoRegistro(), TIPOS, "Tipo de registro inválido (Cliente o Tránsito)");
        validarOpcion(req.tipoGrupo(), GRUPOS, "Tipo de grupo inválido");
        validarOpcion(req.rangoEtario(), RANGOS, "Rango etario inválido");

        String boleta = vacio(req.codigoBoleta()) ? null : req.codigoBoleta().trim();
        if (boleta != null && repo.existsByCodigoBoletaAndAnuladoFalse(boleta))
            throw error(HttpStatus.CONFLICT, "Esta boleta ya fue registrada");

        // Fecha y hora del SERVIDOR (hora de Chile), no las del teléfono
        LocalDateTime ahora = LocalDateTime.now(ZONA);

        Registro r = new Registro();
        r.setUsuarioId(req.usuarioId());
        r.setObservadorNombre(req.observadorNombre().trim());
        r.setTurnoId(req.turnoId() != null && req.turnoId() > 0 ? req.turnoId() : null);
        r.setAcceso(req.acceso());
        r.setTipoRegistro(req.tipoRegistro());
        r.setTipoGrupo(req.tipoGrupo());
        r.setRangoEtario(req.rangoEtario());
        r.setCodigoBoleta(boleta);
        r.setFecha(ahora.toLocalDate());
        r.setHora(ahora.toLocalTime().truncatedTo(ChronoUnit.MINUTES));
        r.setAnulado(false);
        return repo.save(r);
    }

    @Transactional
    public Registro modificar(Long id, ModificarRegistroAforoRequest req) {
        Registro r = obtener(id);
        if (estaAnulado(r))
            throw error(HttpStatus.CONFLICT, "El registro está anulado: restáuralo antes de modificarlo");
        validarAcceso(req.acceso());
        validarOpcion(req.tipoGrupo(), GRUPOS, "Tipo de grupo inválido");
        validarOpcion(req.rangoEtario(), RANGOS, "Rango etario inválido");

        r.setAcceso(req.acceso());
        r.setTipoGrupo(req.tipoGrupo());
        r.setRangoEtario(req.rangoEtario());
        return repo.save(r);
    }

    /** ANULAR en vez de eliminar: deja de contar, pero se conserva y se puede restaurar. */
    @Transactional
    public Registro anular(Long id) {
        Registro r = obtener(id);
        if (estaAnulado(r))
            throw error(HttpStatus.CONFLICT, "El registro ya está anulado");
        r.setAnulado(true);
        return repo.save(r);
    }

    /** Si su boleta se volvió a registrar mientras estaba anulado, no se puede restaurar (quedaría duplicada). */
    @Transactional
    public Registro restaurar(Long id) {
        Registro r = obtener(id);
        if (!estaAnulado(r))
            throw error(HttpStatus.CONFLICT, "El registro ya está vigente");
        if (r.getCodigoBoleta() != null
                && repo.existsByCodigoBoletaAndAnuladoFalseAndIdNot(r.getCodigoBoleta(), r.getId()))
            throw error(HttpStatus.CONFLICT, "No se puede restaurar: su boleta ya está en otro registro vigente");
        r.setAnulado(false);
        return repo.save(r);
    }

    // ---------- validaciones ----------

    private void validarAcceso(Integer acceso) {
        if (acceso == null || acceso < ACCESO_MIN || acceso > ACCESO_MAX)
            throw error(HttpStatus.BAD_REQUEST, "El acceso debe estar entre 1 y 4");
    }

    private void validarOpcion(String valor, Set<String> permitidos, String mensaje) {
        if (valor == null || !permitidos.contains(valor))
            throw error(HttpStatus.BAD_REQUEST, mensaje);
    }

    private LocalDate leerFecha(String texto) {
        try {
            return LocalDate.parse(texto.trim());   // yyyy-MM-dd
        } catch (DateTimeParseException e) {
            throw error(HttpStatus.BAD_REQUEST, "La fecha debe tener el formato yyyy-MM-dd");
        }
    }

    // null cuenta como vigente (filas creadas antes de existir la columna)
    private boolean estaAnulado(Registro r) {
        return Boolean.TRUE.equals(r.getAnulado());
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}
