package com.rifas.publicas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

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

    @Column(name = "imagen_url")
    private String imagenUrl;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioBoleto;

    @Column(nullable = false)
    private Integer totalBoletos;

    @NotNull(message = "La fecha del sorteo es obligatoria")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "fecha_sorteo", nullable = false)
    private LocalDateTime fechaSorteo;

    @Column(length = 500)
    private String videoUrl;

    @Column(nullable = false, length = 20)
    private String estado; // ACTIVA, FINALIZADA

    @Column(name = "costo_premio")
    private BigDecimal costoPremio;

    // --- Único atributo nuevo requerido ---
    @Column(name = "porcentaje_minimo_ventas", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeMinimoVentas = new BigDecimal("70.00");

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

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public BigDecimal getCostoPremio() {
        return costoPremio;
    }

    public void setCostoPremio(BigDecimal costoPremio) {
        this.costoPremio = costoPremio;
    }

    public BigDecimal getPorcentajeMinimoVentas() {
        return porcentajeMinimoVentas;
    }

    public void setPorcentajeMinimoVentas(BigDecimal porcentajeMinimoVentas) {
        this.porcentajeMinimoVentas = porcentajeMinimoVentas;
    }
}