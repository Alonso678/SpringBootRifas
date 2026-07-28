package com.rifas.publicas.controller;

import com.rifas.publicas.model.*;
import com.rifas.publicas.repository.*;

import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
public class MainController {

    private final RifaRepository rifaRepository;
    private final BoletoRepository boletoRepository;
    private final CompraRepository compraRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public MainController(RifaRepository rifaRepository, BoletoRepository boletoRepository,
            CompraRepository compraRepository, UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.rifaRepository = rifaRepository;
        this.boletoRepository = boletoRepository;
        this.compraRepository = compraRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("rifas", rifaRepository.findAll());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute Usuario usuario, RedirectAttributes redirectAttributes) {
        try {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            usuario.setRol("ROLE_USER");
            usuarioRepository.save(usuario);
            redirectAttributes.addFlashAttribute("mensajeExito", "Registro exitoso. Ahora puede iniciar sesión.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "El correo ya está registrado.");
            return "redirect:/registro";
        }
        return "redirect:/login";
    }

    @GetMapping("/rifas/{id}")
    public String detalleRifa(@PathVariable("id") Long id, Model model) {
        Rifa rifa = rifaRepository.findById(id).orElseThrow(() -> new RuntimeException("Rifa no encontrada"));
        List<Boleto> boletos = boletoRepository.findByRifaId(id);
        model.addAttribute("rifa", rifa);
        model.addAttribute("boletos", boletos);
        return "detalle-rifa";
    }

    @PostMapping("/comprar")
    public String comprarBoletos(@RequestParam("rifaId") Long rifaId,
            @RequestParam("boletosSeleccionados") List<Long> boletoIds,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Rifa rifa = rifaRepository.findById(rifaId)
                .orElseThrow(() -> new RuntimeException("Rifa no encontrada"));

        List<Boleto> boletosComprados = new ArrayList<>();
        for (Long bId : boletoIds) {
            Boleto b = boletoRepository.findById(bId).orElseThrow();
            if (!"DISPONIBLE".equals(b.getEstado())) {
                redirectAttributes.addFlashAttribute("mensajeError", "Uno o más boletos ya no están disponibles.");
                return "redirect:/rifas/" + rifaId;
            }
            b.setEstado("APARTADO");
            b.setUsuario(usuario);
            boletoRepository.save(b);
            boletosComprados.add(b);
        }

        Compra compra = new Compra();
        compra.setUsuario(usuario);
        compra.setRifa(rifa);
        compra.setBoletos(boletosComprados);
        compra.setMontoTotal(rifa.getPrecioBoleto().multiply(BigDecimal.valueOf(boletosComprados.size())));
        compra.setEstadoPago("PENDIENTE");
        compra.setFechaCompra(LocalDateTime.now());
        compraRepository.save(compra);

        redirectAttributes.addFlashAttribute("mensajeExito",
                "¡Boletos apartados con éxito! Realice su pago para validarlos.");
        return "redirect:/mis-compras";
    }

    @GetMapping("/mis-compras")
    public String misCompras(Principal principal, Model model) {
        if (principal == null)
            return "redirect:/login";
        Usuario usuario = usuarioRepository.findByEmail(principal.getName()).orElseThrow();
        List<Compra> compras = compraRepository.findByUsuarioId(usuario.getId());

        // Calcular la suma total de los montos de las compras que están pendientes
        BigDecimal totalPendiente = compras.stream()
                .filter(c -> c != null && "PENDIENTE".equals(c.getEstadoPago()) && c.getMontoTotal() != null)
                .map(c -> c.getMontoTotal())
                .reduce(BigDecimal.ZERO, (acumulado, actual) -> acumulado.add(actual));

        // Evaluar directamente en Java si hay montos pendientes mayores a cero
        boolean tienePendientes = totalPendiente.compareTo(BigDecimal.ZERO) > 0;

        model.addAttribute("compras", compras);
        model.addAttribute("totalPendiente", totalPendiente);
        model.addAttribute("tienePendientes", tienePendientes);

        return "mis-compras";
    }

    @GetMapping("/admin/rifas")
    public String adminRifas(Model model) {
        model.addAttribute("rifas", rifaRepository.findAll());
        model.addAttribute("nuevaRifa", new Rifa());
        return "admin-rifas";
    }

    @PostMapping("/admin/rifas/guardar")
    public String guardarRifa(@ModelAttribute Rifa rifa) {
        rifa.setEstado("ACTIVA");
        Rifa guardada = rifaRepository.save(rifa);
        
        // Generar boletos automáticamente
        for (int i = 1; i <= rifa.getTotalBoletos(); i++) {
            Boleto b = new Boleto();
            b.setNumeroBoleto(i);
            b.setRifa(guardada);
            b.setEstado("DISPONIBLE");
            boletoRepository.save(b);
        }
        return "redirect:/admin/rifas";
    }
    
    @GetMapping("/admin/compras")
    public String adminCompras(
            @RequestParam(value = "filtro", required = false) String filtro,
            Model model) {
        
        List<Compra> compras;
        
        if (filtro != null && !filtro.trim().isEmpty()) {
            String f = filtro.trim().toLowerCase();
            compras = compraRepository.findAll().stream()
                .filter(c -> 
                    (c.getUsuario() != null && (
                        (c.getUsuario().getNombre() != null && c.getUsuario().getNombre().toLowerCase().contains(f)) ||
                        (c.getUsuario().getEmail() != null && c.getUsuario().getEmail().toLowerCase().contains(f))
                    )) ||
                    (c.getEstadoPago() != null && c.getEstadoPago().toLowerCase().contains(f)) ||
                    (c.getBoletos() != null && c.getBoletos().stream().anyMatch(b -> String.valueOf(b.getNumeroBoleto()).contains(f)))
                )
                .toList();
        } else {
            compras = compraRepository.findAll();
        }

        model.addAttribute("compras", compras);
        model.addAttribute("filtroActual", filtro);
        return "admin-compras";
    }

    // @GetMapping("/admin/compras")
    // public String adminCompras(Model model) {
    //     model.addAttribute("compras", compraRepository.findAll());
    //     return "admin-compras";
    // }

    @PostMapping("/admin/compras/validar/{id}")
    public String validarPago(@PathVariable("id") @NonNull Long compraId, @RequestParam("estado") String estado) {
        Compra compra = compraRepository.findById(compraId).orElseThrow();
        compra.setEstadoPago(estado);
        compraRepository.save(compra);

        for (Boleto b : compra.getBoletos()) {
            if ("PAGADO".equals(estado)) {
                b.setEstado("PAGADO");
            } else {
                b.setEstado("DISPONIBLE");
                b.setUsuario(null);
            }
            boletoRepository.save(b);
        }
        return "redirect:/admin/compras";
    }
}
