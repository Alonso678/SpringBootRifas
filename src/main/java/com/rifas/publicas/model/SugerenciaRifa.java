package com.rifas.publicas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sugerencias_rifas")
public class SugerenciaRifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "premio_sugerido", nullable = false)
    private String premioSugerido;

    @Column(name = "contacto_usuario")
    private String contactoUsuario;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPremioSugerido() {
        return premioSugerido;
    }

    public void setPremioSugerido(String premioSugerido) {
        this.premioSugerido = premioSugerido;
    }

    public String getContactoUsuario() {
        return contactoUsuario;
    }

    public void setContactoUsuario(String contactoUsuario) {
        this.contactoUsuario = contactoUsuario;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}