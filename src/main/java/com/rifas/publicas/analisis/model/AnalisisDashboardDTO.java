package com.rifas.publicas.analisis.model;

import java.math.BigDecimal;

public class AnalisisDashboardDTO {
    private Long idRifa;
    private String tituloRifa;
    private int totalBoletos;
    private int boletosVendidos;
    private int boletosPendientes;
    private BigDecimal precioPorBoleto;
    private BigDecimal costoPremio;
    private BigDecimal ingresosTotales;
    private BigDecimal utilidadNeta;
    private double porcentajeVendido;
    private String estadoRifa;

    // Genera los Getters y Setters o usa @Data si tienes Lombok

    public Long getIdRifa() {
        return idRifa;
    }

    public void setIdRifa(Long idRifa) {
        this.idRifa = idRifa;
    }

    public String getTituloRifa() {
        return tituloRifa;
    }

    public void setTituloRifa(String tituloRifa) {
        this.tituloRifa = tituloRifa;
    }

    public int getTotalBoletos() {
        return totalBoletos;
    }

    public void setTotalBoletos(int totalBoletos) {
        this.totalBoletos = totalBoletos;
    }

    public int getBoletosVendidos() {
        return boletosVendidos;
    }

    public void setBoletosVendidos(int boletosVendidos) {
        this.boletosVendidos = boletosVendidos;
    }

    public BigDecimal getPrecioPorBoleto() {
        return precioPorBoleto;
    }

    public void setPrecioPorBoleto(BigDecimal precioPorBoleto) {
        this.precioPorBoleto = precioPorBoleto;
    }

    public BigDecimal getCostoPremio() {
        return costoPremio;
    }

    public void setCostoPremio(BigDecimal costoPremio) {
        this.costoPremio = costoPremio;
    }

    public BigDecimal getIngresosTotales() {
        return ingresosTotales;
    }

    public void setIngresosTotales(BigDecimal ingresosTotales) {
        this.ingresosTotales = ingresosTotales;
    }

    public BigDecimal getUtilidadNeta() {
        return utilidadNeta;
    }

    public void setUtilidadNeta(BigDecimal utilidadNeta) {
        this.utilidadNeta = utilidadNeta;
    }

    public double getPorcentajeVendido() {
        return porcentajeVendido;
    }

    public void setPorcentajeVendido(double porcentajeVendido) {
        this.porcentajeVendido = porcentajeVendido;
    }

    public int getBoletosPendientes() {
        return boletosPendientes;
    }
    public void setBoletosPendientes(int boletosPendientes) {
        this.boletosPendientes = boletosPendientes;
    }

    public String getEstadoRifa() {
        return estadoRifa;
    }
    
    public void setEstadoRifa(String estadoRifa) {
        this.estadoRifa = estadoRifa;
    }
}
