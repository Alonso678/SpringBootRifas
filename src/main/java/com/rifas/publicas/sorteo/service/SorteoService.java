package com.rifas.publicas.sorteo.service;

import com.rifas.publicas.model.Boleto;
import com.rifas.publicas.model.Rifa;
import com.rifas.publicas.repository.BoletoRepository;
import com.rifas.publicas.repository.RifaRepository;
import com.rifas.publicas.sorteo.model.ConfigSorteo;
import com.rifas.publicas.sorteo.repository.ConfigSorteoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SorteoService {

    // Logger para auditoría y monitoreo del comportamiento del servicio[cite: 3]
    private static final Logger log = LoggerFactory.getLogger(SorteoService.class);

    private final BoletoRepository boletoRepository;
    private final ConfigSorteoRepository configSorteoRepository;
    private final RifaRepository rifaRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    SorteoService(BoletoRepository boletoRepository, ConfigSorteoRepository configSorteoRepository, RifaRepository rifaRepository) {
        this.boletoRepository = boletoRepository;
        this.configSorteoRepository = configSorteoRepository;
        this.rifaRepository = rifaRepository;
    }

    /**
     * @deprecated Mantenido por compatibilidad, pero se recomienda usar cumpleUmbralMinimo.
     */
    public boolean validarMetaVentas(Long rifaId, int totalBoletos) {
        long boletosVendidos = boletoRepository.countByRifaIdAndPagado(rifaId);
        double porcentajeVenta = (double) boletosVendidos / totalBoletos;
        return porcentajeVenta >= 0.70;
    }

    @Transactional
    public Boleto ejecutarSorteo(Long rifaId, int boletosADescartar) {
        // ... [Lógica existente de ejecución preservada intacta para estabilidad]
        ConfigSorteo config = configSorteoRepository.findByRifaId(rifaId).orElseGet(() -> {
            ConfigSorteo nuevo = new ConfigSorteo();
            nuevo.setRifaId(rifaId);
            nuevo.setBoletosADescartar(boletosADescartar);
            return nuevo;
        });

        if (config.isSorteoRealizado()) {
            throw new IllegalStateException("El sorteo para esta rifa ya fue realizado previamente.");
        }

        List<Boleto> boletosDisponibles = boletoRepository.findByRifaIdAndPagado(rifaId);
        
        if (boletosDisponibles.isEmpty()) {
            throw new IllegalStateException("No hay boletos vendidos disponibles para realizar el sorteo.");
        }

        int indiceGanador = secureRandom.nextInt(boletosDisponibles.size());
        Boleto boletoGanador = boletosDisponibles.get(indiceGanador);

        config.setSorteoRealizado(true);
        config.setBoletoGanadorId(boletoGanador.getId());
        config.setFechaSorteo(LocalDateTime.now());
        configSorteoRepository.save(config);

        return boletoGanador;
    }

    /**
     * Valida de forma dinámica y precisa si la rifa alcanza el porcentaje mínimo de ventas requerido.
     * Utiliza BigDecimal para evitar pérdida de precisión en divisiones monetarias/porcentuales.
     */
    public boolean cumpleUmbralMinimo(Long rifaId) {
        log.info("Iniciando validación de umbral mínimo de ventas para la rifa ID: {}", rifaId);

        Rifa rifa = rifaRepository.findById(rifaId)
                .orElseThrow(() -> {
                    log.error("Error crítico: Rifa con ID {} no encontrada en BD al validar umbral", rifaId);
                    return new RuntimeException("Rifa no encontrada");
                });

        long boletosVendidos = boletoRepository.countByRifaIdAndPagado(rifaId);
        int totalBoletos = rifa.getTotalBoletos();

        if (totalBoletos <= 0) {
            log.warn("La rifa ID {} tiene un total de boletos configurado en cero o negativo: {}", rifaId, totalBoletos);
            return false;
        }

        // Cálculo seguro de porcentaje con BigDecimal
        BigDecimal vendidosBD = BigDecimal.valueOf(boletosVendidos);
        BigDecimal totalBD = BigDecimal.valueOf(totalBoletos);
        BigDecimal porcentajeVendido = vendidosBD
                .multiply(BigDecimal.valueOf(100))
                .divide(totalBD, 2, RoundingMode.HALF_UP);

        BigDecimal minimoRequerido = rifa.getPorcentajeMinimoVentas(); // Ej: 70.00[cite: 2]

        boolean cumple = porcentajeVendido.compareTo(minimoRequerido) >= 0;

        log.info("Resultado evaluación rifa ID [{}]: {}/{} boletos pagados. Porcentaje alcanzado: {}% | Requerido: {}% -> ¿Cumple meta?: {}",
                rifaId, boletosVendidos, totalBoletos, porcentajeVendido, minimoRequerido, cumple);

        return cumple;
    }
}