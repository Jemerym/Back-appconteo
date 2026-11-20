package cl.duocuc.roles_ms.controller;

import cl.duocuc.roles_ms.model.Rol;
import cl.duocuc.roles_ms.service.RolService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;
    
    @Operation(summary = "Listar roles")
    @GetMapping
    public List<Rol> listar() {
        return rolService.listar();
    }

    @Operation(summary = "Obtener un rol por id")
    @GetMapping("/{id}")
    public Rol obtener(@PathVariable Long id) {
        return rolService.obtener(id);
    }
}
