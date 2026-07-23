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

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioBoleto;

    @Column(nullable = false)
    private Integer totalBoletos;

    @Column(name = "fecha_sorteo", nullable = false)
    private LocalDateTime fechaSorteo;

    @Column(nullable = false, length = 20)
    private String estado; // ACTIVA, FINALIZADA
}
