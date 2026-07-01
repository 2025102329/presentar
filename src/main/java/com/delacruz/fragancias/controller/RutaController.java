package com.delacruz.fragancias.controller;

import com.delacruz.fragancias.dto.GrafoResponse;
import com.delacruz.fragancias.dto.RutaRequest;
import com.delacruz.fragancias.dto.RutaResponse;
import com.delacruz.fragancias.service.GrafoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rutas")
public class RutaController {

    private final GrafoService service;

    public RutaController(GrafoService service) {
        this.service = service;
    }

    @GetMapping("/grafo")
    public GrafoResponse grafo() {
        return service.obtenerGrafo();
    }

    @GetMapping("/ultima")
    public RutaResponse ultima() {
        return service.ultimaRuta();
    }

    @PostMapping("/generar")
    public RutaResponse generar(@RequestBody(required = false) RutaRequest request) {
        return service.generarRuta(request == null ? new RutaRequest(java.util.List.of()) : request);
    }
}
