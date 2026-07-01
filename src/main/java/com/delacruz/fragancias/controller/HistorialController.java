package com.delacruz.fragancias.controller;

import com.delacruz.fragancias.entity.Historial;
import com.delacruz.fragancias.service.HistorialService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/historial")
public class HistorialController {

    private final HistorialService service;

    public HistorialController(HistorialService service) {
        this.service = service;
    }

    @GetMapping
    public List<Historial> listar() {
        return service.listarPila();
    }

    @DeleteMapping("/ultimo")
    public Historial desapilar() {
        return service.desapilar().orElse(null);
    }
}
