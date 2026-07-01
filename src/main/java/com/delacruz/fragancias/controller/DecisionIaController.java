package com.delacruz.fragancias.controller;

import com.delacruz.fragancias.entity.DecisionIa;
import com.delacruz.fragancias.repository.DecisionIaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/decisiones-ia")
public class DecisionIaController {

    private final DecisionIaRepository repository;

    public DecisionIaController(DecisionIaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<DecisionIa> listar() {
        return repository.findTop50ByOrderByFechaDesc();
    }
}
