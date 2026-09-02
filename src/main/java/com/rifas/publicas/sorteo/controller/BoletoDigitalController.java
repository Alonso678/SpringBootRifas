package com.rifas.publicas.sorteo.controller;

import com.rifas.publicas.model.Boleto;
import com.rifas.publicas.model.Rifa;
import com.rifas.publicas.repository.BoletoRepository;
import com.rifas.publicas.repository.RifaRepository;
import com.rifas.publicas.sorteo.model.BoletoDigital;
import com.rifas.publicas.sorteo.model.ConfigSorteo;
import com.rifas.publicas.sorteo.repository.BoletoDigitalRepository;
import com.rifas.publicas.sorteo.repository.ConfigSorteoRepository;
import com.rifas.publicas.sorteo.service.BoletoDigitalService;
import com.rifas.publicas.sorteo.service.CryptoService;
import com.rifas.publicas.sorteo.service.SorteoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
public class BoletoDigitalController {

    private static final Logger log = LoggerFactory.getLogger(BoletoDigitalController.class);
    

    
    private final BoletoDigitalService boletoDigitalService;

    private final BoletoRepository boletoRepository;

    private final BoletoDigitalRepository boletoDigitalRepository;

    private final ConfigSorteoRepository configSorteoRepository;

    private final RifaRepository rifaRepository;

    private final SorteoService sorteoService;

    BoletoDigitalController(BoletoDigitalService boletoDigitalService, BoletoRepository boletoRepository, BoletoDigitalRepository boletoDigitalRepository, ConfigSorteoRepository configSorteoRepository, RifaRepository rifaRepository, SorteoService sorteoService) {
        this.boletoDigitalService = boletoDigitalService;
        this.boletoRepository = boletoRepository;
        this.boletoDigitalRepository = boletoDigitalRepository;
        this.configSorteoRepository = configSorteoRepository;
        this.rifaRepository = rifaRepository;
        this.sorteoService = sorteoService; 
    }

    /**
     * Vista web del boleto digital (Disponible para el usuario dueño del boleto).
     */
    @GetMapping("/boleto/digital/{boletoId}")
    public String verBoletoDigital(@PathVariable Long boletoId, Model model) {
        Boleto boleto = boletoRepository.findById(boletoId)
                .orElseThrow(() -> new RuntimeException("Boleto no encontrado"));
        BoletoDigital boletoDigital = boletoDigitalRepository.findByBoletoId(boletoId)
                .orElseThrow(() -> new RuntimeException("El boleto digital aún no ha sido emitido"));

        String qrBase64 = boletoDigitalService.generarQrBoletoDigitalBase64(boletoId);

        model.addAttribute("boleto", boleto);
        model.addAttribute("boletoDigital", boletoDigital);
        model.addAttribute("qrCode", qrBase64);

        return "sorteo/boleto-digital";
    }

    /**
     * Endpoint para que el usuario descargue su boleto oficial en formato PDF.
     */
    @GetMapping("/boleto/digital/{boletoId}/pdf")
    public ResponseEntity<byte[]> descargarBoletoPdf(@PathVariable Long boletoId) {
        try {
            byte[] pdfBytes = boletoDigitalService.generarPdfBoleto(boletoId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "BoletoDigital-" + boletoId + ".pdf");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            // Maneja el error apropiadamente (puedes registrarlo con un log)
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint exclusivo de administración para verificar la autenticidad del QR,
     * la fecha del sorteo y si el boleto es ganador utilizando el registro
     * persistido.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/sorteo/verificar")
    public String verificarBoletoQr(
            @RequestParam Long id,
            @RequestParam String rs,
            @RequestParam String sello,
            Principal principal,
            Model model) {

        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Unauthorized: Debe iniciar sesión como administrador.");
        }

        // 1. Buscar el boleto y su rifa asociada
        Boleto boleto = boletoRepository.findById(id).orElse(null);
        if (boleto == null) {
            model.addAttribute("valido", false);
            model.addAttribute("mensaje", "El boleto no existe en el sistema.");
            return "sorteo/resultado-verificacion";
        }

        var rifa = boleto.getRifa();

        // 2. Buscar el registro digital oficial guardado en base de datos
        BoletoDigital boletoDigital = boletoDigitalRepository.findByBoletoId(id).orElse(null);
        if (boletoDigital == null) {
            model.addAttribute("valido", false);
            model.addAttribute("mensaje", "¡ADVERTENCIA! No existe registro digital oficial para este boleto.");
            model.addAttribute("boleto", boleto);
            return "sorteo/resultado-verificacion";
        }

        // 3. Verificación Segura por Coincidencia Directa (Persistida)
        boolean esAutentico = boletoDigital.getRandomState().equals(rs)
                && boletoDigital.getSelloDigital().equals(sello);

        if (!esAutentico) {
            model.addAttribute("valido", false);
            model.addAttribute("mensaje",
                    "¡ADVERTENCIA! Posible fraude detectado: Los elementos de seguridad no coinciden con el registro oficial.");
            model.addAttribute("boleto", boleto);
            return "sorteo/resultado-verificacion";
        }

        // 4. Validación de la Fecha del Sorteo
        java.time.LocalDate hoy = java.time.LocalDate.now();
        boolean sorteoRealizado = rifa.getFechaSorteo() != null &&
                !hoy.isBefore(rifa.getFechaSorteo().toLocalDate());

        if (!sorteoRealizado) {
            model.addAttribute("valido", true);
            model.addAttribute("sorteoRealizado", false);
            model.addAttribute("mensaje",
                    "Boleto Auténtico, pero el sorteo aún no se ha llevado a cabo (Fecha programada: "
                            + rifa.getFechaSorteo() + ").");
            model.addAttribute("boleto", boleto);
            return "sorteo/resultado-verificacion";
        }

        // 5. Validación de si es Ganador
        boolean esGanador = false; // Cambiar cuando implementes la lógica o columna en BD

        model.addAttribute("valido", true);
        model.addAttribute("sorteoRealizado", true);
        model.addAttribute("esGanador", esGanador);
        model.addAttribute("boleto", boleto);

        if (esGanador) {
            model.addAttribute("mensaje", "¡FELICIDADES! El boleto es AUTÉNTICO y ¡ES GANADOR del premio!");
        } else {
            model.addAttribute("mensaje",
                    "El boleto es auténtico y el sorteo ya pasó, pero este número no resultó ganador.");
        }

        return "sorteo/resultado-verificacion";
    }

    /**
     * Dashboard de administración ajustado: Reemplazados los valores quemados (ej. totalBoletos = 10) 
     * por consultas reales a la base de datos y validación dinámica de metas.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/sorteo/dashboard/{rifaId}")
    public String verDashboardSorteo(@PathVariable Long rifaId, Model model) {
        log.info("Cargando dashboard de sorteo para la rifa ID: {}", rifaId);

        Rifa rifa = rifaRepository.findById(rifaId)
                .orElseThrow(() -> new RuntimeException("Rifa no encontrada"));
        
        ConfigSorteo config = configSorteoRepository.findByRifaId(rifaId).orElse(null);
        
        // Datos reales obtenidos dinámicamente de la entidad y el repositorio[cite: 6]
        int totalBoletos = rifa.getTotalBoletos();
        long boletosVendidos = boletoRepository.countByRifaIdAndPagado(rifaId);
        long boletosPendientes = boletoRepository.countByRifaIdAndPendiente(rifaId);
        
        // Uso del servicio centralizado en lugar de lógica hardcodeada >= 0.70[cite: 6]
        boolean cumpleMeta = sorteoService.cumpleUmbralMinimo(rifaId);

        model.addAttribute("rifaId", rifaId);
        model.addAttribute("rifa", rifa);
        model.addAttribute("totalBoletos", totalBoletos);
        model.addAttribute("boletosVendidos", boletosVendidos);
        model.addAttribute("boletosPendientes", boletosPendientes);
        model.addAttribute("cumpleMeta", cumpleMeta);
        model.addAttribute("configSorteo", config);

        return "sorteo/admin-dashboard";
    }

    // @PreAuthorize("hasRole('ADMIN')")
    // @GetMapping("/admin/sorteo/dashboard/{rifaId}")
    // public String verDashboardSorteo(@PathVariable Long rifaId, Model model) {
    //     ConfigSorteo config = configSorteoRepository.findByRifaId(rifaId).orElse(null);
        
    //     // Simulación de métricas para la vista
    //     long totalBoletos = 10; // Para tu prueba de 10 boletos
    //     long boletosVendidos = boletoRepository.countByRifaIdAndPagado(rifaId ,"PAGADO");
    //     boolean cumpleMeta = (double) boletosVendidos / totalBoletos >= 0.70;

    //     model.addAttribute("rifaId", rifaId);
    //     model.addAttribute("totalBoletos", totalBoletos);
    //     model.addAttribute("boletosVendidos", boletosVendidos);
    //     model.addAttribute("cumpleMeta", cumpleMeta);
    //     model.addAttribute("configSorteo", config);

    //     return "sorteo/admin-dashboard";
    // }
}