package cl.duocuc.registros_ms.controller;

import cl.duocuc.registros_ms.dto.*;

import cl.duocuc.registros_ms.model.Registro;
import cl.duocuc.registros_ms.service.RegistroService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/registros")
@RequiredArgsConstructor
public class RegistroController {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final RegistroService service;

    @Operation(summary = "Listar registros (filtros opcionales: usuarioId, turnoId, fecha yyyy-MM-dd, incluirAnulados)")
    @GetMapping
    public List<RegistroAforoResponse> listar(@RequestParam(required = false) Long usuarioId,
                                         @RequestParam(required = false) Long turnoId,
                                         @RequestParam(required = false) String fecha,
                                         @RequestParam(defaultValue = "false") boolean incluirAnulados) {
        return service.listar(usuarioId, turnoId, fecha, incluirAnulados).stream().map(this::aResponse).toList();
    }

    @Operation(summary = "Totales del día sin anulados (fecha por defecto: hoy; usuarioId opcional)")
    @GetMapping("/conteo")
    public ConteoResponse conteo(@RequestParam(required = false) String fecha,
                                 @RequestParam(required = false) Long usuarioId) {
        return service.conteo(fecha, usuarioId);
    }

    @Operation(summary = "¿La boleta ya está en un registro vigente?")
    @GetMapping("/boleta/existe")
    public Map<String, Boolean> existeBoleta(@RequestParam String codigo) {
        return Map.of("existe", service.existeBoleta(codigo));
    }

    @Operation(summary = "Obtener un registro por id")
    @GetMapping("/{id}")
    public RegistroAforoResponse obtener(@PathVariable Long id) {
        return aResponse(service.obtener(id));
    }

    @Operation(summary = "Registrar una persona (fecha y hora las pone el servidor; 409 si la boleta ya se usó)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroAforoResponse crear(@RequestBody RegistroAforoRequest req) {
        return aResponse(service.crear(req));
    }

    @Operation(summary = "Modificar acceso, grupo y rango etario de un registro")
    @PutMapping("/{id}")
    public RegistroAforoResponse modificar(@PathVariable Long id, @RequestBody ModificarRegistroAforoRequest req) {
        return aResponse(service.modificar(id, req));
    }

    @Operation(summary = "Anular un registro (se conserva y se puede restaurar)")
    @PutMapping("/{id}/anular")
    public RegistroAforoResponse anular(@PathVariable Long id) {
        return aResponse(service.anular(id));
    }

    @Operation(summary = "Restaurar un registro anulado (409 si su boleta ya está en otro registro vigente)")
    @PutMapping("/{id}/restaurar")
    public RegistroAforoResponse restaurar(@PathVariable Long id) {
        return aResponse(service.restaurar(id));
    }

    private RegistroAforoResponse aResponse(Registro r) {
        return new RegistroAforoResponse(
                r.getId(),
                r.getUsuarioId(),
                r.getObservadorNombre(),
                r.getTurnoId(),
                r.getAcceso(),
                r.getTipoRegistro(),
                r.getTipoGrupo(),
                r.getRangoEtario(),
                r.getCodigoBoleta(),
                r.getFecha().toString(),
                r.getHora().format(HORA),
                Boolean.TRUE.equals(r.getAnulado())
        );
    }
}
