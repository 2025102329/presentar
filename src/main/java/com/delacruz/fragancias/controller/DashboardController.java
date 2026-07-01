package com.delacruz.fragancias.controller;

import com.delacruz.fragancias.dto.DashboardResponse;
import com.delacruz.fragancias.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardResponse resumen() {
        return service.resumen();
    }
}
