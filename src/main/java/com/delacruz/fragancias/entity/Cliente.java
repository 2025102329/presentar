package com.delacruz.fragancias.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Cliente de la perfumería.
 *
 * Además de sus datos normales, esta tabla guarda la ubicación que la IA
 * escogió dentro del árbol binario. Así el árbol conserva su forma incluso
 * después de reiniciar el sistema.
 */
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(nullable = false, length = 60)
    private String apellidoPaterno;

    @Column(nullable = false, length = 60)
    private String apellidoMaterno;

    @Column(nullable = false, length = 220)
    private String nombreCompleto;

    @Column(nullable = false, length = 40)
    private String abreviatura;

    @Column(length = 20)
    private String dni;

    @Column(length = 30)
    private String telefono;

    @Column(length = 120)
    private String email;

    @Column(nullable = false, length = 220)
    private String direccion;

    @Column(nullable = false, length = 80)
    private String zona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "padre_id")
    private Cliente padre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LadoArbol lado;

    @Column(nullable = false)
    private int nivel;

    @Column(length = 600)
    private String explicacionInsercion;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    void antesDeGuardar() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidoPaterno() { return apellidoPaterno; }
    public void setApellidoPaterno(String apellidoPaterno) { this.apellidoPaterno = apellidoPaterno; }
    public String getApellidoMaterno() { return apellidoMaterno; }
    public void setApellidoMaterno(String apellidoMaterno) { this.apellidoMaterno = apellidoMaterno; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getAbreviatura() { return abreviatura; }
    public void setAbreviatura(String abreviatura) { this.abreviatura = abreviatura; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getZona() { return zona; }
    public void setZona(String zona) { this.zona = zona; }
    public Cliente getPadre() { return padre; }
    public void setPadre(Cliente padre) { this.padre = padre; }
    public LadoArbol getLado() { return lado; }
    public void setLado(LadoArbol lado) { this.lado = lado; }
    public int getNivel() { return nivel; }
    public void setNivel(int nivel) { this.nivel = nivel; }
    public String getExplicacionInsercion() { return explicacionInsercion; }
    public void setExplicacionInsercion(String explicacionInsercion) { this.explicacionInsercion = explicacionInsercion; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
}
