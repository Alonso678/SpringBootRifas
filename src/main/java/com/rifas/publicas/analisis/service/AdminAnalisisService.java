package com.rifas.publicas.analisis.service;

import com.rifas.publicas.analisis.model.AnalisisDashboardDTO;
import com.rifas.publicas.model.Rifa;
import com.rifas.publicas.repository.RifaRepository;
import com.rifas.publicas.repository.BoletoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AdminAnalisisService {

    private final RifaRepository rifaRepository;

    private final BoletoRepository boletoRepository;

    AdminAnalisisService(RifaRepository rifaRepository, BoletoRepository boletoRepository) {
        this.rifaRepository = rifaRepository;
        this.boletoRepository = boletoRepository;
    }

    public List<Rifa> obtenerTodasLasRifas() {
        return rifaRepository.findAll();
    }

    public AnalisisDashboardDTO calcularMetricasRifa(Long rifaId) {
        Rifa rifa = rifaRepository.findById(rifaId)
                .orElseThrow(() -> new IllegalArgumentException("Rifa no encontrada"));

        // método para contar boletos pagados/vendidos por Rifa
        int boletosVendidos = (int) boletoRepository.countByRifaIdAndPagado(rifa.getId());
        
        AnalisisDashboardDTO dto = new AnalisisDashboardDTO();
        dto.setIdRifa(rifa.getId());
        dto.setTituloRifa(rifa.getTitulo());
        dto.setTotalBoletos(rifa.getTotalBoletos());
        dto.setBoletosVendidos(boletosVendidos);
        dto.setPrecioPorBoleto(rifa.getPrecioBoleto());
        
        // Asumiendo que agregaste 'costoPremio' a tu entidad Rifa. Si no, puedes
        // definirlo en 0 por ahora.
        BigDecimal costoPremio = rifa.getCostoPremio() != null ? rifa.getCostoPremio() : BigDecimal.ZERO;
        dto.setCostoPremio(costoPremio);

        // Cálculos financieros
        BigDecimal ingresos = rifa.getPrecioBoleto().multiply(new BigDecimal(boletosVendidos));
        dto.setIngresosTotales(ingresos);
        dto.setUtilidadNeta(ingresos.subtract(costoPremio));

        // Porcentaje
        double porcentaje = 0.0;
        if (rifa.getTotalBoletos() > 0) {
            porcentaje = ((double) boletosVendidos / rifa.getTotalBoletos()) * 100;
        }
        
        // Redondear a 2 decimales
        BigDecimal bdPorcentaje = new BigDecimal(porcentaje).setScale(2, RoundingMode.HALF_UP);
        dto.setPorcentajeVendido(bdPorcentaje.doubleValue());

        return dto;
    }
}
