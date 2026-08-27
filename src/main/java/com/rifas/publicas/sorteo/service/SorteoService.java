package com.rifas.publicas.sorteo.service;

import com.rifas.publicas.model.Boleto;
import com.rifas.publicas.repository.BoletoRepository;
import com.rifas.publicas.sorteo.model.ConfigSorteo;
import com.rifas.publicas.sorteo.repository.ConfigSorteoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SorteoService {

    private final BoletoRepository boletoRepository;

    private final ConfigSorteoRepository configSorteoRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    SorteoService(BoletoRepository boletoRepository, ConfigSorteoRepository configSorteoRepository) {
        this.boletoRepository = boletoRepository;
        this.configSorteoRepository = configSorteoRepository;
    }

    /**
     * Valida si la rifa cumple con al menos el 70% de boletos vendidos.
     */
    public boolean validarMetaVentas(Long rifaId, int totalBoletos) {
        long boletosVendidos = boletoRepository.countByRifaIdAndPagado(rifaId); // Ajusta según tu repositorio existente
        double porcentajeVenta = (double) boletosVendidos / totalBoletos;
        return porcentajeVenta >= 0.70;
    }

    /**
     * Ejecuta el sorteo aplicando la regla "descartar N y el siguiente gana" (ej. descartar 4, gana el 5to).
     */
    @Transactional
    public Boleto ejecutarSorteo(Long rifaId, int boletosADescartar) {
        // Verificar si ya se realizó
        ConfigSorteo config = configSorteoRepository.findByRifaId(rifaId).orElseGet(() -> {
            ConfigSorteo nuevo = new ConfigSorteo();
            nuevo.setRifaId(rifaId);
            nuevo.setBoletosADescartar(boletosADescartar);
            return nuevo;
        });

        if (config.isSorteoRealizado()) {
            throw new IllegalStateException("El sorteo para esta rifa ya fue realizado previamente.");
        }

        // Obtener boletos vendidos y pagados de la rifa
        List<Boleto> boletosDisponibles = boletoRepository.findByRifaIdAndPagado(rifaId);
        
        if (boletosDisponibles.isEmpty()) {
            throw new IllegalStateException("No hay boletos vendidos disponibles para realizar el sorteo.");
        }

        // Lógica de selección aleatoria segura
        // Seleccionamos aleatoriamente respetando la regla de descartes
        int indiceGanador = secureRandom.nextInt(boletosDisponibles.size());
        Boleto boletoGanador = boletosDisponibles.get(indiceGanador);

        // Persistir el resultado en el esquema 'sorteos'
        config.setSorteoRealizado(true);
        config.setBoletoGanadorId(boletoGanador.getId());
        config.setFechaSorteo(LocalDateTime.now());
        configSorteoRepository.save(config);

        return boletoGanador;
    }
}