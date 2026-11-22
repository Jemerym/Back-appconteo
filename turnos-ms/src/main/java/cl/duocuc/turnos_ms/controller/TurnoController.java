package cl.duocuc.turnos_ms.controller;

import cl.duocuc.turnos_ms.dto.TurnoRequest;
import cl.duocuc.turnos_ms.dto.TurnoResponse;
import cl.duocuc.turnos_ms.model.Turno;
import cl.duocuc.turnos_ms.service.TurnoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final TurnoService service;

    @Operation(summary = "Listar turnos (opcional: ?usuarioId= para ver solo los de un observador)")
    @GetMapping
    public List<TurnoResponse> listar(@RequestParam(required = false) Long usuarioId) {
        return service.listar(usuarioId).stream().map(this::aResponse).toList();
    }

    @Operation(summary = "Turno activo ahora de un observador (204 si no tiene)")
    @GetMapping("/actual")
    public ResponseEntity<TurnoResponse> actual(@RequestParam Long usuarioId) {
        return service.obtenerActual(usuarioId)
                .map(t -> ResponseEntity.ok(aResponse(t)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Obtener un turno por id")
    @GetMapping("/{id}")
    public TurnoResponse obtener(@PathVariable Long id) {
        return aResponse(service.obtener(id));
    }

    @Operation(summary = "Programar un turno")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TurnoResponse crear(@RequestBody TurnoRequest req) {
        return aResponse(service.crear(req));
    }

    @Operation(summary = "Modificar un turno")
    @PutMapping("/{id}")
    public TurnoResponse actualizar(@PathVariable Long id, @RequestBody TurnoRequest req) {
        return aResponse(service.actualizar(id, req));
    }

    @Operation(summary = "Anular un turno (se conserva y se puede restaurar)")
    @PutMapping("/{id}/anular")
    public TurnoResponse anular(@PathVariable Long id) {
        return aResponse(service.anular(id));
    }

    @Operation(summary = "Restaurar un turno anulado (409 si el tramo ya está ocupado)")
    @PutMapping("/{id}/restaurar")
    public TurnoResponse restaurar(@PathVariable Long id) {
        return aResponse(service.restaurar(id));
    }

    @Operation(summary = "Anular todos los turnos de un usuario (al anular al usuario)")
    @PutMapping("/usuario/{usuarioId}/anular")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void anularDeUsuario(@PathVariable Long usuarioId) {
        service.anularDeUsuario(usuarioId);
    }

    private TurnoResponse aResponse(Turno t) {
        return new TurnoResponse(
                t.getId(),
                t.getUsuarioId(),
                t.getObservadorNombre(),
                t.getAcceso(),
                t.getHoraInicio().format(HORA),
                t.getHoraFin().format(HORA),
                t.getFecha().toString(),
                Boolean.TRUE.equals(t.getAnulado())
        );
    }
}
