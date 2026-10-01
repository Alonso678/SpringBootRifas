package com.rifas.publicas.analisis.service;

import com.rifas.publicas.analisis.model.AnalisisDashboardDTO;
import com.rifas.publicas.model.Rifa;
import com.rifas.publicas.repository.RifaRepository;
import com.rifas.publicas.repository.BoletoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public void verificarYActualizarRifasVencidas(Long administradorId) {
        // 1. Trae todas las rifas que siguen marcadas como ACTIVA para este
        // administrador
        List<Rifa> rifasActivas = rifaRepository.findByAdministradorIdAndEstado(administradorId, "ACTIVA");
        LocalDateTime ahora = LocalDateTime.now();

        // 2. Recórrelas y actualiza las que ya hayan superado su fecha de sorteo
        for (Rifa r : rifasActivas) {
            if (r.getFechaSorteo() != null && r.getFechaSorteo().isBefore(ahora)) {
                r.setEstado("VENCIDA");
                rifaRepository.save(r);
            }
        }
    }

    public AnalisisDashboardDTO calcularMetricasRifa(Long rifaId) {
        Rifa rifa = rifaRepository.findById(rifaId)
                .orElseThrow(() -> new IllegalArgumentException("Rifa no encontrada"));

        // Validar si la fecha de sorteo ya pasó y sigue ACTIVA
        if ("ACTIVA".equals(rifa.getEstado()) && rifa.getFechaSorteo() != null
                && rifa.getFechaSorteo().isBefore(LocalDateTime.now())) {
            rifa.setEstado("VENCIDA");
            rifaRepository.save(rifa); // Persiste el cambio en la base de datos
        }

        // método para contar boletos pagados/vendidos por Rifa
        int boletosVendidos = (int) boletoRepository.countByRifaIdAndPagado(rifa.getId());
        int boletosPendientes = (int) boletoRepository.countByRifaIdAndPendiente(rifa.getId()); // O el estado que utilices para pendientes
        
        AnalisisDashboardDTO dto = new AnalisisDashboardDTO();
        dto.setIdRifa(rifa.getId());
        dto.setTituloRifa(rifa.getTitulo());
        dto.setTotalBoletos(rifa.getTotalBoletos());
        dto.setBoletosVendidos(boletosVendidos);
        dto.setBoletosPendientes(boletosPendientes);
        dto.setPrecioPorBoleto(rifa.getPrecioBoleto());
        dto.setEstadoRifa(rifa.getEstado());
        
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

    public Map<String, Object> obterAnalisisGlobal() {
        // Recupera todas as rifas registradas na plataforma
        List<Rifa> todasRifas = rifaRepository.findAll();

        // Calcula as estatísticas (aproveitando o método auxiliar que você já possui)
        return calcularEstadisticas(todasRifas);
    }

    private Map<String, Object> calcularEstadisticas(List<Rifa> rifas) {
        long totalRifas = rifas.size();
        long rifasActivas = rifas.stream().filter(r -> "ACTIVA".equalsIgnoreCase(r.getEstado())).count();
        long rifasVencidas = rifas.stream().filter(r -> "VENCIDA".equalsIgnoreCase(r.getEstado())).count();

        BigDecimal ingresosTotalesPlataforma = BigDecimal.ZERO;
        BigDecimal utilidadTotalPlataforma = BigDecimal.ZERO;
        int boletosVendidosTotales = 0;

        // Se recorren las rifas utilizando el método existente para garantizar coherencia financiera
        for (Rifa rifa : rifas) {
            AnalisisDashboardDTO metricas = calcularMetricasRifa(rifa.getId());
            ingresosTotalesPlataforma = ingresosTotalesPlataforma.add(metricas.getIngresosTotales());
            utilidadTotalPlataforma = utilidadTotalPlataforma.add(metricas.getUtilidadNeta());
            boletosVendidosTotales += metricas.getBoletosVendidos();
        }

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("totalRifas", totalRifas);
        estadisticas.put("rifasActivas", rifasActivas);
        estadisticas.put("rifasVencidas", rifasVencidas);
        estadisticas.put("ingresosTotalesPlataforma", ingresosTotalesPlataforma);
        estadisticas.put("utilidadTotalPlataforma", utilidadTotalPlataforma);
        estadisticas.put("boletosVendidosTotales", boletosVendidosTotales);

        return estadisticas;
    }

    public Map<String, Object> obtenerAnalisisGlobal() {
        List<Rifa> todasLasRifas = rifaRepository.findAll();
        Map<String, Object> estadisticas = calcularEstadisticas(todasLasRifas);

        // Agregamos la lista detallada de cada rifa individual para el Super Admin
        List<AnalisisDashboardDTO> detallesRifas = todasLasRifas.stream()
                .map(r -> calcularMetricasRifa(r.getId()))
                .toList();

        estadisticas.put("detallesRifas", detallesRifas);
        return estadisticas;
    }
}
