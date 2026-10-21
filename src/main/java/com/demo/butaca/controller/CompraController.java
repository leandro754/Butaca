package com.demo.butaca.controller;

import com.demo.butaca.dto.CompraRequestDTO;
import com.demo.butaca.dto.CompraResponseDTO;
import com.demo.butaca.dto.VerificacionCruceDTO;
import com.demo.butaca.service.CompraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompraResponseDTO comprarEntrada(@Valid @RequestBody CompraRequestDTO request) {
        return compraService.comprarEntrada(request);
    }

    @GetMapping("/{id}")
    public CompraResponseDTO obtenerCompra(@PathVariable Long id) {
        return compraService.obtenerCompra(id);
    }

    @GetMapping("/{id}/verificar-cruce")
    public VerificacionCruceDTO verificarCruce(@PathVariable Long id) {
        return compraService.verificarCruce(id);
    }

    @GetMapping("/usuario/{id}")
    public List<CompraResponseDTO> listarComprasUsuario(@PathVariable Long id) {
        return compraService.listarComprasUsuario(id);
    }

    @GetMapping("/organizador/{id}")
    public List<CompraResponseDTO> listarComprasOrganizador(@PathVariable Long id) {
        return compraService.listarComprasOrganizador(id);
    }
}
