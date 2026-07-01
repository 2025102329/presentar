package com.delacruz.fragancias.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Auditoría de cada decisión tomada por Gemini
 * o por el algoritmo local.
 */
@Entity
@Table(name = "decisiones_ia")
public class DecisionIa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoDecisionIa tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenDecision origen;

    /*
     * En PostgreSQL se utiliza TEXT en lugar de @Lob.
     * Esto evita problemas con columnas OID al consultar la tabla.
     */
    @Column(name = "entrada_json", nullable = false, columnDefinition = "TEXT")
    private String entradaJson;

    @Column(name = "salida_json", nullable = false, columnDefinition = "TEXT")
    private String salidaJson;

    @Column(name = "decision_final", nullable = false, length = 240)
    private String decisionFinal;

    @Column(nullable = false, length = 800)
    private String explicacion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    void antesDeGuardar() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public TipoDecisionIa getTipo() {
        return tipo;
    }

    public void setTipo(TipoDecisionIa tipo) {
        this.tipo = tipo;
    }

    public OrigenDecision getOrigen() {
        return origen;
    }

    public void setOrigen(OrigenDecision origen) {
        this.origen = origen;
    }

    public String getEntradaJson() {
        return entradaJson;
    }

    public void setEntradaJson(String entradaJson) {
        this.entradaJson = entradaJson;
    }

    public String getSalidaJson() {
        return salidaJson;
    }

    public void setSalidaJson(String salidaJson) {
        this.salidaJson = salidaJson;
    }

    public String getDecisionFinal() {
        return decisionFinal;
    }

    public void setDecisionFinal(String decisionFinal) {
        this.decisionFinal = decisionFinal;
    }

    public String getExplicacion() {
        return explicacion;
    }

    public void setExplicacion(String explicacion) {
        this.explicacion = explicacion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}