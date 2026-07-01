package com.delacruz.fragancias.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Guarda la última ruta generada para poder consultarla después. */
@Entity
@Table(name = "rutas_planificadas")
public class RutaPlanificada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Lob
    @Column(nullable = false)
    private String ordenZonasJson;

    @Lob
    @Column(nullable = false)
    private String recorridoCompletoJson;

    @Lob
    @Column(nullable = false)
    private String entregasJson;

    @Column(nullable = false)
    private double distanciaTotalKm;

    @Column(nullable = false)
    private int tiempoTotalMinutos;

    @Column(nullable = false, length = 160)
    private String criterio;

    @Column(nullable = false, length = 900)
    private String explicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenDecision origenDecision;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    void antesDeGuardar() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getOrdenZonasJson() { return ordenZonasJson; }
    public void setOrdenZonasJson(String ordenZonasJson) { this.ordenZonasJson = ordenZonasJson; }
    public String getRecorridoCompletoJson() { return recorridoCompletoJson; }
    public void setRecorridoCompletoJson(String recorridoCompletoJson) { this.recorridoCompletoJson = recorridoCompletoJson; }
    public String getEntregasJson() { return entregasJson; }
    public void setEntregasJson(String entregasJson) { this.entregasJson = entregasJson; }
    public double getDistanciaTotalKm() { return distanciaTotalKm; }
    public void setDistanciaTotalKm(double distanciaTotalKm) { this.distanciaTotalKm = distanciaTotalKm; }
    public int getTiempoTotalMinutos() { return tiempoTotalMinutos; }
    public void setTiempoTotalMinutos(int tiempoTotalMinutos) { this.tiempoTotalMinutos = tiempoTotalMinutos; }
    public String getCriterio() { return criterio; }
    public void setCriterio(String criterio) { this.criterio = criterio; }
    public String getExplicacion() { return explicacion; }
    public void setExplicacion(String explicacion) { this.explicacion = explicacion; }
    public OrigenDecision getOrigenDecision() { return origenDecision; }
    public void setOrigenDecision(OrigenDecision origenDecision) { this.origenDecision = origenDecision; }
    public LocalDateTime getFecha() { return fecha; }
}
