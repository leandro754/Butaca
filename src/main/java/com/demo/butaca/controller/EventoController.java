package com.demo.butaca.controller;

import com.demo.butaca.dto.EntradaResponseDTO;
import com.demo.butaca.dto.EventoRequestDTO;
import com.demo.butaca.dto.CancelarEventoRequest;
import com.demo.butaca.dto.EventoResponseDTO;
import com.demo.butaca.service.EntradaService;
import com.demo.butaca.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;
    private final EntradaService entradaService;

    public EventoController(EventoService eventoService, EntradaService entradaService) {
        this.eventoService = eventoService;
        this.entradaService = entradaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventoResponseDTO crearEvento(@Valid @RequestBody EventoRequestDTO request) {
        return eventoService.crearEvento(request);
    }

    @GetMapping
    public List<EventoResponseDTO> listarPublicados(@RequestParam(required = false) Long categoriaId) {
        return eventoService.listarEventosPublicados(categoriaId);
    }

    @GetMapping("/{id}")
    public EventoResponseDTO obtenerEvento(@PathVariable Long id) {
        return eventoService.obtenerEvento(id);
    }

    @PutMapping("/{id}")
    public EventoResponseDTO editarEvento(@PathVariable Long id, @Valid @RequestBody EventoRequestDTO request) {
        return eventoService.editarEvento(id, request);
    }

    @GetMapping("/organizador/{id}")
    public List<EventoResponseDTO> listarPorOrganizador(@PathVariable Long id) {
        return eventoService.listarEventosPorOrganizador(id);
    }

    @GetMapping("/categoria/{id}")
    public List<EventoResponseDTO> listarPorCategoria(@PathVariable Long id) {
        return eventoService.listarEventosPorCategoria(id);
    }

    @GetMapping("/{id}/entradas")
    public List<EntradaResponseDTO> listarEntradasPorEvento(@PathVariable Long id) {
        return entradaService.listarEntradasPorEvento(id);
    }

    @PutMapping("/{id}/publicar")
    public void publicarEvento(@PathVariable Long id) {
        eventoService.publicarEvento(id);
    }

    @PutMapping("/{id}/cancelar")
    public void cancelarEvento(@PathVariable Long id, @Valid @RequestBody CancelarEventoRequest request) {
        eventoService.cancelarEvento(id, request);
    }

}
