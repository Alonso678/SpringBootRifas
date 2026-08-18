package com.rifas.publicas.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe contener exactamente 10 dígitos numéricos")
    private String telefono;

    @Column(nullable = false, length = 30)
    private String rol; // ROLE_USER, ROLE_ADMIN

    @Column(name = "token")
    private String token;

    // Modificaciones sugeridas en la entidad User (Spring Data JPA)

    @Column(unique = true, length = 20)
    private String codigoReferido;

    @Column(nullable = false)
    private Integer puntos = 0; // O saldoComision según prefieras manejarlo

    // En Usuario.java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referido_por_id")
    private Usuario referidoPor;

    // En Usuario.java
    private BigDecimal saldoMonedero = BigDecimal.ZERO;
    private LocalDateTime fechaMetaCompletada; // Cuando llegó a los $150
    private boolean reclamoBloqueado = false;
    private int boletosVendidosTrasBloqueo = 0;

    // Getters y Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
