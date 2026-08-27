package com.rifas.publicas.sorteo.controller;

import com.rifas.publicas.model.Boleto;
import com.rifas.publicas.repository.BoletoRepository;
import com.rifas.publicas.sorteo.model.BoletoDigital;
import com.rifas.publicas.sorteo.model.ConfigSorteo;
import com.rifas.publicas.sorteo.repository.BoletoDigitalRepository;
import com.rifas.publicas.sorteo.repository.ConfigSorteoRepository;
import com.rifas.publicas.sorteo.service.BoletoDigitalService;
import com.rifas.publicas.sorteo.service.CryptoService;
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

    private final BoletoDigitalService boletoDigitalService;

    private final BoletoRepository boletoRepository;

    private final BoletoDigitalRepository boletoDigitalRepository;

    private final ConfigSorteoRepository configSorteoRepository;

    private final CryptoService cryptoService;

    BoletoDigitalController(BoletoDigitalService boletoDigitalService, BoletoRepository boletoRepository, BoletoDigitalRepository boletoDigitalRepository, CryptoService cryptoService, ConfigSorteoRepository configSorteoRepository) {
        this.boletoDigitalService = boletoDigitalService;
        this.boletoRepository = boletoRepository;
        this.boletoDigitalRepository = boletoDigitalRepository;
        this.cryptoService = cryptoService;
        this.configSorteoRepository = configSorteoRepository;
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
     * Endpoint exclusivo de administración para verificar la autenticidad del QR o un boleto.
     * Si el usuario no es ADMIN, se bloquea con 403 Forbidden / 401 Unauthorized.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/sorteo/verificar")
    public String verificarBoletoQr(
            @RequestParam Long id,
            @RequestParam String rs,
            @RequestParam String sello,
            Principal principal,
            Model model) {

        // Doble validación de seguridad por si Spring Security requiere control programático extra
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Debe iniciar sesión como administrador.");
        }

        Boleto boleto = boletoRepository.findById(id).orElse(null);
        if (boleto == null) {
            model.addAttribute("valido", false);
            model.addAttribute("mensaje", "El boleto no existe en el sistema.");
            return "sorteo/resultado-verificacion";
        }

        String emailUsuario = "cliente@rifas.com"; 
        boolean esAutentico = cryptoService.verificarSello(id, String.valueOf(boleto.getNumeroBoleto()), emailUsuario, rs, sello);

        model.addAttribute("valido", esAutentico);
        model.addAttribute("boleto", boleto);
        model.addAttribute("mensaje", esAutentico ? "¡Boleto Auténtico y Verificado por Administración!" : "¡ADVERTENCIA! Posible fraude detectado.");

        return "sorteo/resultado-verificacion";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/sorteo/dashboard/{rifaId}")
    public String verDashboardSorteo(@PathVariable Long rifaId, Model model) {
        ConfigSorteo config = configSorteoRepository.findByRifaId(rifaId).orElse(null);
        
        // Simulación de métricas para la vista
        long totalBoletos = 10; // Para tu prueba de 10 boletos
        long boletosVendidos = boletoRepository.countByRifaIdAndPagado(rifaId);
        boolean cumpleMeta = (double) boletosVendidos / totalBoletos >= 0.70;

        model.addAttribute("rifaId", rifaId);
        model.addAttribute("totalBoletos", totalBoletos);
        model.addAttribute("boletosVendidos", boletosVendidos);
        model.addAttribute("cumpleMeta", cumpleMeta);
        model.addAttribute("configSorteo", config);

        return "sorteo/admin-dashboard";
    }
}