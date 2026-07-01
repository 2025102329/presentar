package com.delacruz.fragancias.controller;

import com.delacruz.fragancias.dto.ArbolClienteNode;
import com.delacruz.fragancias.dto.ClienteRequest;
import com.delacruz.fragancias.dto.ClienteResponse;
import com.delacruz.fragancias.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    public List<ClienteResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse registrar(@Valid @RequestBody ClienteRequest request) {
        return service.registrar(request);
    }

    @GetMapping("/arbol")
    public ArbolClienteNode arbol() {
        return service.obtenerArbol();
    }
}
