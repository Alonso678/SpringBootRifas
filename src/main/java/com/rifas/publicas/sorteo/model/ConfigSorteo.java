package com.rifas.publicas.sorteo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "config_sorteo", schema = "sorteos") // <-- Apunta al nuevo esquema
public class ConfigSorteo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rifa_id", nullable = false, unique = true)
    private Long rifaId;

    @Column(name = "boletos_a_descartar", nullable = false)
    private int boletosADescartar;

    @Column(name = "sorteo_realizado", nullable = false)
    private boolean sorteoRealizado = false;

    @Column(name = "boleto_ganador_id")
    private Long boletoGanadorId;

    @Column(name = "fecha_sorteo")
    private LocalDateTime fechaSorteo;

    // Constructores, Getters y Setters...
    public ConfigSorteo() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRifaId() {
        return rifaId;
    }

    public void setRifaId(Long rifaId) {
        this.rifaId = rifaId;
    }

    public int getBoletosADescartar() {
        return boletosADescartar;
    }

    public void setBoletosADescartar(int boletosADescartar) {
        this.boletosADescartar = boletosADescartar;
    }

    public boolean isSorteoRealizado() {
        return sorteoRealizado;
    }

    public void setSorteoRealizado(boolean sorteoRealizado) {
        this.sorteoRealizado = sorteoRealizado;
    }

    public Long getBoletoGanadorId() {
        return boletoGanadorId;
    }

    public void setBoletoGanadorId(Long boletoGanadorId) {
        this.boletoGanadorId = boletoGanadorId;
    }

    public LocalDateTime getFechaSorteo() {
        return fechaSorteo;
    }

    public void setFechaSorteo(LocalDateTime fechaSorteo) {
        this.fechaSorteo = fechaSorteo;
    }
}