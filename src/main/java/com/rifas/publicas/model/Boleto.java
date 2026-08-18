package com.rifas.publicas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "boletos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Boleto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer numeroBoleto;

    @ManyToOne
    @JoinColumn(name = "rifa_id", nullable = false)
    private Rifa rifa;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 30)
    private String estado; // DISPONIBLE, APARTADO, PAGADO, CANJEADO
}
