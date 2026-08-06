package com.rifas.publicas.controller;

import com.rifas.publicas.model.*;
import com.rifas.publicas.repository.*;
import com.rifas.publicas.service.EmailService;

import jakarta.validation.Valid;

import org.apache.el.stream.Optional;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class MainController {

    private final RifaRepository rifaRepository;
    private final BoletoRepository boletoRepository;
    private final CompraRepository compraRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public MainController(RifaRepository rifaRepository, BoletoRepository boletoRepository,
            CompraRepository compraRepository, UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder, EmailService emailService) {
        this.rifaRepository = rifaRepository;
        this.boletoRepository = boletoRepository;
        this.compraRepository = compraRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @GetMapping("/")
    public String index(Model model) {
        // Solo carga las rifas con estado "ACTIVA"
        model.addAttribute("rifas", rifaRepository.findByEstado("ACTIVA"));
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
    public String registrarUsuario(@Valid @ModelAttribute("usuario") Usuario usuario,
            BindingResult bindingResult, RedirectAttributes redirectAttributes) {

        // Si hay errores de validación (ej. no cumple los 10 dígitos), regresa al
        // formulario
        if (bindingResult.hasErrors()) {
            return "registro";
        }
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

    // Muestra la vista con el formulario para ingresar el correo
    @GetMapping("/recuperar-password")
    public String mostrarFormularioRecuperacion() {
        return "recuperar-password";
    }

    // Procesa el envío del correo cuando se presiona el botón del formulario
    @PostMapping("/recuperar-password")
    public String procesarRecuperacion(@RequestParam("email") String email, RedirectAttributes ra) {
        java.util.Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            String token = java.util.UUID.randomUUID().toString();

            // ¡IMPORTANTE! Asignar y guardar el token en la base de datos
            usuario.setToken(token);
            usuarioRepository.save(usuario);

            String link = "http://localhost:8080/reset-password?token=" + token;
            emailService.enviarEmail(email, "Recuperación de contraseña",
                    "Haz clic aquí para restablecer tu contraseña: " + link);
        }

        ra.addFlashAttribute("mensajeExito", "Si el correo existe, recibirás instrucciones.");
        return "redirect:/login";
    }
    // @PostMapping("/recuperar-password")
    // public String procesarRecuperacion(@RequestParam("email") String email, RedirectAttributes ra) {
    //     java.util.Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        
    //     if (usuarioOpt.isPresent()) {
    //         String token = java.util.UUID.randomUUID().toString();
    //         // Opcional: Guardar token en BD...
            
    //         String link = "http://localhost:8080/reset-password?token=" + token;
    //         emailService.enviarEmail(email, "Recuperación de contraseña", 
    //             "Haz clic aquí para restablecer tu contraseña: " + link);
    //     }
        
    //     ra.addFlashAttribute("mensajeExito", "Si el correo existe, recibirás instrucciones.");
    //     return "redirect:/login";
    // }

    @GetMapping("/reset-password")
    public String mostrarFormularioReset(@RequestParam("token") String token, Model model) {
        // Opcional: Validar si el token existe y no ha expirado en tu base de datos
        // boolean isValid = tokenService.validarToken(token);
        // if (!isValid) { model.addAttribute("error", "Token inválido o expirado"); }

        model.addAttribute("token", token);
        return "reset-password-view"; // Nombre de tu archivo HTML (ej. reset-password-view.html)
    }

    @PostMapping("/reset-password")
    public String procesarResetPassword(@RequestParam("token") String token,
            @RequestParam("nuevaPassword") String nuevaPassword,
            Model model) {

        System.out.println("Token recibido: " + token);
        System.out.println("Nueva contraseña recibida: " + nuevaPassword);

        // 1. Buscar al usuario por medio del token de recuperación
        // (Asegúrate de tener este método en tu UsuarioRepository, o busca por el campo
        // que uses para el token)
        Usuario usuario = usuarioRepository.findByToken(token).orElse(null);

        if (usuario == null) {
            model.addAttribute("error", "El token de recuperación es inválido o ha expirado.");
            model.addAttribute("token", token);
            return "reset-password-view"; // Regresa a la vista mostrando el error
        }

        // 2. Encriptar la nueva contraseña con BCrypt (indispensable para Spring
        // Security)
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));

        // 3. Opcional: Limpiar el token para que no se pueda volver a reutilizar
        usuario.setToken(null);

        // 4. PERSISTIR / GUARDAR en la base de datos (Esto genera el UPDATE)
        usuarioRepository.save(usuario);

        // 5. Redirigir al login con un parámetro de éxito
        return "redirect:/login?resetSuccess=true";
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
            @RequestParam(value = "boletosSeleccionados", required = false) List<Long> boletoIds,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        // Validación si no se seleccionó ningún boleto
        if (boletoIds == null || boletoIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "Por favor selecciona al menos un boleto para apartar.");
            return "redirect:/rifas/" + rifaId;
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

        BigDecimal totalPendiente = compras.stream()
                .filter(c -> c != null && "PENDIENTE".equals(c.getEstadoPago()) && c.getMontoTotal() != null)
                .map(c -> c.getMontoTotal())
                .reduce(BigDecimal.ZERO, (acumulado, actual) -> acumulado.add(actual));

        boolean tienePendientes = totalPendiente.compareTo(BigDecimal.ZERO) > 0;

        model.addAttribute("compras", compras);
        model.addAttribute("totalPendiente", totalPendiente);
        model.addAttribute("tienePendientes", tienePendientes);

        return "mis-compras";
    }

    @GetMapping("/admin/rifas")
    public String adminRifas(Model model) {
        // findAll() recupera todas las rifas sin importar su estado
        model.addAttribute("rifas", rifaRepository.findAll());
        model.addAttribute("nuevaRifa", new Rifa());
        return "admin-rifas";
    }

    @GetMapping("/admin/rifas/nuevo")
    public String formularioCrear(Model model) {
        model.addAttribute("rifa", new Rifa());
        return "admin/form";
    }

    @GetMapping("/admin/rifas/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id, Model model) {
        Rifa rifa = rifaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rifa no encontrada"));
        model.addAttribute("rifa", rifa);
        return "admin/form";
    }

    @PostMapping("/admin/rifas/guardar")
    public String guardarRifa(@ModelAttribute Rifa rifa) {
        if (rifa.getId() == null) {
            if (rifa.getEstado() == null || rifa.getEstado().isEmpty()) {
                rifa.setEstado("ACTIVA");
            }
            Rifa guardada = rifaRepository.save(rifa);
            
            for (int i = 1; i <= rifa.getTotalBoletos(); i++) {
                Boleto b = new Boleto();
                b.setNumeroBoleto(i);
                b.setRifa(guardada);
                b.setEstado("DISPONIBLE");
                boletoRepository.save(b);
            }
        } else {
            // Recuperamos la rifa de la base de datos para no perder propiedades obligatorias (como fechaSorteo)
            Rifa existente = rifaRepository.findById(rifa.getId())
                    .orElseThrow(() -> new RuntimeException("Rifa no encontrada"));
            
            existente.setTitulo(rifa.getTitulo());
            existente.setDescripcion(rifa.getDescripcion());
            existente.setPrecioBoleto(rifa.getPrecioBoleto());
            existente.setTotalBoletos(rifa.getTotalBoletos());
            existente.setImagenUrl(rifa.getImagenUrl());
            if (rifa.getEstado() != null && !rifa.getEstado().isEmpty()) {
                existente.setEstado(rifa.getEstado());
            }
            
            rifaRepository.save(existente);
        }
        
        return "redirect:/admin/rifas";
    }

    @PostMapping("/admin/rifas/estatus/{id}")
    public String cambiarEstatus(@PathVariable("id") Long id) {
        Rifa rifa = rifaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rifa no encontrada"));
        rifa.setEstado("ACTIVA".equals(rifa.getEstado()) ? "FINALIZADA" : "ACTIVA");
        rifaRepository.save(rifa);
        return "redirect:/";
    }

    @GetMapping("/admin/rifas/eliminar/{id}")
    public String eliminarRifa(@PathVariable("id") Long id) {
        rifaRepository.deleteById(id);
        return "redirect:/";
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