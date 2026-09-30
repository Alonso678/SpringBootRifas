package com.rifas.publicas.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(exclude = "usuario") // Excluye para evitar bucles
@ToString(exclude = "usuario")
@Entity
public class DatosBancarios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String banco;

    @Column(name = "clabe_interbancaria", nullable = false, length = 18, unique = true)
    private String clabeInterbancaria;

    @Column(name = "titular_cuenta", nullable = false, length = 150)
    private String titularCuenta;

    // Relación OneToOne o ManyToOne con Usuario (Administrador)
    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
