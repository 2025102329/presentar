package com.delacruz.fragancias.controller;

import com.delacruz.fragancias.dto.PedidoRequest;
import com.delacruz.fragancias.dto.PedidoResponse;
import com.delacruz.fragancias.entity.EstadoPedido;
import com.delacruz.fragancias.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @GetMapping
    public List<PedidoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/cola")
    public List<PedidoResponse> cola() {
        return service.colaFifo();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse registrar(@Valid @RequestBody PedidoRequest request) {
        return service.registrar(request);
    }

    @PostMapping("/atender-siguiente")
    public PedidoResponse atenderSiguiente() {
        return service.atenderSiguiente();
    }

    @PatchMapping("/{id}/estado")
    public PedidoResponse cambiarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        EstadoPedido estado = EstadoPedido.valueOf(body.getOrDefault("estado", "").toUpperCase());
        return service.cambiarEstado(id, estado);
    }
}
