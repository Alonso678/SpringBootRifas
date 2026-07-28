package com.rifas.publicas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rifas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rifa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(name = "imagen_url") // O el nombre de la columna en tu BD si aplica
    private String imagenUrl;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioBoleto;

    @Column(nullable = false)
    private Integer totalBoletos;

    @Column(name = "fecha_sorteo", nullable = false)
    private LocalDateTime fechaSorteo;

    // Nuevo campo para almacenar el enlace del video del sorteo (YouTube, Vimeo, etc.)
    @Column(length = 500)
    private String videoUrl;

    @Column(nullable = false, length = 20)
    private String estado; // ACTIVA, FINALIZADA

    // --- Getters y Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecioBoleto() {
        return precioBoleto;
    }

    public void setPrecioBoleto(BigDecimal precioBoleto) {
        this.precioBoleto = precioBoleto;
    }

    public Integer getTotalBoletos() {
        return totalBoletos;
    }

    public void setCantidadBoletos(Integer cantidadBoletos) {
        this.totalBoletos = cantidadBoletos;
    }

    public LocalDateTime getFechaSorteo() {
        return fechaSorteo;
    }

    public void setFechaSorteo(LocalDateTime fechaSorteo) {
        this.fechaSorteo = fechaSorteo;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    // Getters y Setters
    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }
}
