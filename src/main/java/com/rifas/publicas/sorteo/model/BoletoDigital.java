package com.rifas.publicas.sorteo.model;

import com.rifas.publicas.model.Boleto;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "boleto_digital", schema = "sorteos")
public class BoletoDigital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación directa mapeada a la columna boleto_id de la BD
    @OneToOne
    @JoinColumn(name = "boleto_id", nullable = false)
    private Boleto boleto;

    @Column(name = "random_state", unique = true, nullable = false)
    private String randomState;

    @Column(name = "sello_digital", columnDefinition = "TEXT", nullable = false)
    private String selloDigital;

    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision = LocalDateTime.now();

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boleto getBoleto() {
        return boleto;
    }

    public void setBoleto(Boleto boleto) {
        this.boleto = boleto;
    }

    public String getRandomState() {
        return randomState;
    }

    public void setRandomState(String randomState) {
        this.randomState = randomState;
    }

    public String getSelloDigital() {
        return selloDigital;
    }

    public void setSelloDigital(String selloDigital) {
        this.selloDigital = selloDigital;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }
}