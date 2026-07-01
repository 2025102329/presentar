package com.delacruz.fragancias.entity;

import jakarta.persistence.*;

/** Arista ponderada del grafo de reparto. */
@Entity
@Table(name = "conexiones_ruta")
public class ConexionRuta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String origen;

    @Column(nullable = false, length = 80)
    private String destino;

    @Column(nullable = false)
    private double distanciaKm;

    @Column(nullable = false)
    private int tiempoMinutos;

    @Column(nullable = false, length = 20)
    private String trafico;

    public Long getId() { return id; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public double getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(double distanciaKm) { this.distanciaKm = distanciaKm; }
    public int getTiempoMinutos() { return tiempoMinutos; }
    public void setTiempoMinutos(int tiempoMinutos) { this.tiempoMinutos = tiempoMinutos; }
    public String getTrafico() { return trafico; }
    public void setTrafico(String trafico) { this.trafico = trafico; }
}
