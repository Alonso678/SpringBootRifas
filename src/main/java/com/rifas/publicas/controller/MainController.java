package com.rifas.publicas.controller;

import com.rifas.publicas.model.*;
import com.rifas.publicas.repository.*;
import com.rifas.publicas.service.EmailService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;

@Controller
public class MainController {

    private final RifaRepository rifaRepository;
    private final BoletoRepository boletoRepository;
    private final CompraRepository compraRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final Compraboletosrepository compraBoletosRepository;

    // 1. Inyectamos la URL base desde el properties
    @Value("${app.base-url}")
    private String baseUrl;

    public MainController(RifaRepository rifaRepository, BoletoRepository boletoRepository,
            CompraRepository compraRepository, UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder, EmailService emailService, Compraboletosrepository compraBoletosRepository) {
        this.rifaRepository = rifaRepository;
        this.boletoRepository = boletoRepository;
        this.compraRepository = compraRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.compraBoletosRepository = compraBoletosRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        // Carga las rifas que estén ACTIVA o AGOTADA, y cuya fecha de sorteo aún no haya pasado
        List<Rifa> rifasVisibles = rifaRepository.findAll().stream()
                .filter(r -> "ACTIVA".equals(r.getEstado()) || "AGOTADA".equals(r.getEstado()))
                .filter(r -> r.getFechaSorteo() == null || r.getFechaSorteo().isAfter(LocalDateTime.now()))
                .toList();

        model.addAttribute("rifas", rifasVisibles);
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
            BindingResult bindingResult,
            @RequestParam(value = "ref", required = false) String codigoRef,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors())
            return "registro";

        // 1. Validar si el correo ya está registrado
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            redirectAttributes.addFlashAttribute("mensajeError", "El correo electrónico ya está registrado.");
            return "redirect:/registro";
        }

        try {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            usuario.setRol("ROLE_USER");
            usuario.setCodigoReferido(new com.rifas.publicas.utils.util().generarCodigoReferido(usuario.getNombre()));

            // Vincular con el embajador
            if (codigoRef != null && !codigoRef.isEmpty()) {
                usuarioRepository.findByCodigoReferido(codigoRef).ifPresent(usuario::setReferidoPor);
            }

            usuarioRepository.save(usuario);
            redirectAttributes.addFlashAttribute("mensajeExito", "Registro exitoso.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Ocurrió un error inesperado al registrar.");
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

            String link = baseUrl + "/reset-password?token=" + token;
            emailService.enviarEmail(email, "Recuperación de contraseña",
                    "Haz clic aquí para restablecer tu contraseña: " + link);
        }

        ra.addFlashAttribute("mensajeExito", "Si el correo existe, recibirás instrucciones.");
        return "redirect:/login";
    }

    @GetMapping("/reset-password")
    public String mostrarFormularioReset(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password-view"; 
    }

    @PostMapping("/reset-password")
    public String procesarResetPassword(@RequestParam("token") String token,
            @RequestParam("nuevaPassword") String nuevaPassword,
            Model model) {

        System.out.println("Token recibido: " + token);
        System.out.println("Nueva contraseña recibida: " + nuevaPassword);

        Usuario usuario = usuarioRepository.findByToken(token).orElse(null);

        if (usuario == null) {
            model.addAttribute("error", "El token de recuperación es inválido o ha expirado.");
            model.addAttribute("token", token);
            return "reset-password-view"; 
        }

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setToken(null);
        usuarioRepository.save(usuario);

        return "redirect:/login?resetSuccess=true";
    }

    @GetMapping("/rifas/{id}")
    public String detalleRifa(@PathVariable("id") Long id, Model model) {
        Rifa rifa = rifaRepository.findById(id).orElseThrow(() -> new RuntimeException("Rifa no encontrada"));
        List<Boleto> boletos = boletoRepository.findByRifaIdOrderByNumeroBoletoAsc(id);
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

        model.addAttribute("usuario", usuario); 
        model.addAttribute("compras", compras);
        model.addAttribute("totalPendiente", totalPendiente);
        model.addAttribute("tienePendientes", tienePendientes);
        model.addAttribute("rifasActivas", rifaRepository.findByEstado("ACTIVA"));

        return "mis-compras";
    }

    @GetMapping("/admin/rifas")
    public String adminRifas(Model model) {
        model.addAttribute("rifas", rifaRepository.findAll());
        model.addAttribute("nuevaRifa", new Rifa());
        return "admin-rifas";
    }

    // @GetMapping("/admin/rifas/nuevo")
    // public String formularioCrear(Model model) {
    //     model.addAttribute("nuevaRifa", new Rifa());
    //     return "admin/form";
    // }

    // @GetMapping("/admin/rifas/editar/{id}")
    // public String formularioEditar(@PathVariable("id") Long id, Model model) {
    //     Rifa rifa = rifaRepository.findById(id)
    //             .orElseThrow(() -> new RuntimeException("Rifa no encontrada"));
    //     model.addAttribute("nuevaRifa", rifa);
    //     return "admin/form";
    // }

    @PostMapping("/admin/rifas/guardar")
    @Transactional
    public String guardarRifa(@ModelAttribute("nuevaRifa") Rifa rifa,
            @RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
            RedirectAttributes redirectAttributes) {

        // Ajuste aplicado: Convertir el archivo subido directamente a Base64 para producción (Render)
        if (imagenFile != null && !imagenFile.isEmpty()) {
            try {
                byte[] bytes = imagenFile.getBytes();
                String base64Image = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
                rifa.setImagenUrl(base64Image);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

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
            Rifa existente = rifaRepository.findById(rifa.getId())
                    .orElseThrow(() -> new RuntimeException("Rifa no encontrada"));

            existente.setTitulo(rifa.getTitulo());
            existente.setDescripcion(rifa.getDescripcion());
            existente.setPrecioBoleto(rifa.getPrecioBoleto());
            existente.setTotalBoletos(rifa.getTotalBoletos());

            // Solo actualiza la imagen si se subió una nueva; de lo contrario conserva la anterior
            if (rifa.getImagenUrl() != null && !rifa.getImagenUrl().isEmpty()) {
                existente.setImagenUrl(rifa.getImagenUrl());
            }

            if (rifa.getEstado() != null && !rifa.getEstado().isEmpty()) {
                existente.setEstado(rifa.getEstado());
            }

            rifaRepository.save(existente);
        }

        redirectAttributes.addFlashAttribute("mensajeExito", "Rifa guardada correctamente.");
        return "redirect:/admin/rifas";
    }

    @GetMapping("/admin/rifas/eliminar/{id}")
    @Transactional
    public String eliminarRifa(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Rifa rifa = rifaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Rifa no encontrada"));

            List<Boleto> boletos = boletoRepository.findByRifaIdOrderByNumeroBoletoAsc(id);
            if (boletos != null && !boletos.isEmpty()) {

                // 1. Validar si hay boletos APARTADOS, PAGADOS u otros activos (Mensaje
                // original requerido)
                boolean tieneApartadosOPagados = boletos.stream()
                        .anyMatch(b -> !"RECHAZADO".equalsIgnoreCase(b.getEstado())
                                && !"DISPONIBLE".equalsIgnoreCase(b.getEstado()));

                if (tieneApartadosOPagados) {
                    redirectAttributes.addFlashAttribute("mensajeError",
                            "No se puede eliminar la rifa porque tiene boletos apartados o pagados.");
                    return "redirect:/admin/rifas";
                }

                // 2. Si solo tienen estados RECHAZADO o DISPONIBLE, limpiamos las tablas
                // intermedias de forma segura
                compraBoletosRepository.eliminarRechazadosPorRifaId(id);
                compraRepository.eliminarComprasRechazadasPorRifaId(id);

                // 3. Borramos los boletos asociados
                boletoRepository.deleteAll(boletos);
            }

            // 4. Finalmente borramos la rifa
            rifaRepository.delete(rifa);
            redirectAttributes.addFlashAttribute("mensajeExito", "Rifa eliminada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "No se pudo eliminar la rifa debido a restricciones en la base de datos: " + e.getMessage());
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
                    .filter(c -> (c.getUsuario() != null && ((c.getUsuario().getNombre() != null
                            && c.getUsuario().getNombre().toLowerCase().contains(f)) ||
                            (c.getUsuario().getEmail() != null && c.getUsuario().getEmail().toLowerCase().contains(f))))
                            ||
                            (c.getEstadoPago() != null && c.getEstadoPago().toLowerCase().contains(f)) ||
                            (c.getBoletos() != null && c.getBoletos().stream()
                                    .anyMatch(b -> String.valueOf(b.getNumeroBoleto()).contains(f))))
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

        if ("PAGADO".equals(estado) && !"PAGADO".equals(compra.getEstadoPago())) {
            Usuario comprador = compra.getUsuario();
            if (comprador.getReferidoPor() != null) {
                Usuario embajador = comprador.getReferidoPor();

                BigDecimal comision = compra.getMontoTotal().multiply(new BigDecimal("0.10"));
                embajador.setSaldoMonedero(embajador.getSaldoMonedero().add(comision));

                if (embajador.getSaldoMonedero().compareTo(new BigDecimal("150")) >= 0
                        && embajador.getFechaMetaCompletada() == null) {
                    embajador.setFechaMetaCompletada(LocalDateTime.now());
                }

                if (embajador.isReclamoBloqueado()) {
                    embajador.setBoletosVendidosTrasBloqueo(embajador.getBoletosVendidosTrasBloqueo() + 1);
                    if (embajador.getBoletosVendidosTrasBloqueo() >= 5) {
                        embajador.setReclamoBloqueado(false);
                        embajador.setBoletosVendidosTrasBloqueo(0);
                    }
                }
                usuarioRepository.save(embajador);
            }
        }

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

    @PostMapping("/reclamar-comision")
    @Transactional
    public String reclamarComision(Principal principal, RedirectAttributes ra) {
        Usuario embajador = usuarioRepository.findByEmail(principal.getName()).orElseThrow();

        if (embajador.isReclamoBloqueado() || embajador.getSaldoMonedero().compareTo(new BigDecimal("150")) < 0) {
            ra.addFlashAttribute("mensajeError", "No cumples con los requisitos para reclamar.");
            return "redirect:/mis-compras";
        }

        if (embajador.getFechaMetaCompletada() != null &&
                embajador.getFechaMetaCompletada().isBefore(LocalDateTime.now().minusHours(24))) {

            embajador.setReclamoBloqueado(true);
            embajador.setFechaMetaCompletada(null);
            usuarioRepository.save(embajador);
            ra.addFlashAttribute("mensajeError", "Tiempo de 24h excedido. Reclamo bloqueado hasta vender 5 boletos.");
            return "redirect:/mis-compras";
        }

        embajador.setSaldoMonedero(BigDecimal.ZERO);
        embajador.setFechaMetaCompletada(null);
        usuarioRepository.save(embajador);

        ra.addFlashAttribute("mensajeExito", "¡Comisión reclamada con éxito! Tu monedero se ha reiniciado.");
        return "redirect:/mis-compras";
    }

    @PostMapping("/reclamar-boleto-gratis")
    @Transactional
    public String reclamarBoletoGratis(@RequestParam("rifaId") Long rifaId, Principal principal,
            RedirectAttributes ra) {
        Usuario embajador = usuarioRepository.findByEmail(principal.getName()).orElseThrow();

        if (embajador.getSaldoMonedero().compareTo(new BigDecimal("150")) < 0 || embajador.isReclamoBloqueado()) {
            ra.addFlashAttribute("mensajeError", "No cumples con los requisitos para reclamar.");
            return "redirect:/mis-compras";
        }

        if (embajador.getFechaMetaCompletada() != null &&
                embajador.getFechaMetaCompletada().isBefore(LocalDateTime.now().minusHours(24))) {

            embajador.setReclamoBloqueado(true);
            embajador.setFechaMetaCompletada(null);
            usuarioRepository.save(embajador);
            ra.addFlashAttribute("mensajeError", "Tiempo de 24h excedido. Reclamo bloqueado hasta vender 5 boletos.");
            return "redirect:/mis-compras";
        }

        Rifa rifa = rifaRepository.findById(rifaId).orElse(null);
        if (rifa == null) {
            ra.addFlashAttribute("mensajeError", "La rifa seleccionada no es válida.");
            return "redirect:/mis-compras";
        }

        Boleto boleto = boletoRepository.findByRifaIdAndEstadoOrderByNumeroBoletoAsc(rifaId, "DISPONIBLE")
        .stream()
        .findFirst()
        .orElse(null);

        if (boleto == null) {
            ra.addFlashAttribute("mensajeError", "Lo sentimos, ya no hay boletos disponibles para esta rifa.");
            return "redirect:/mis-compras";
        }

        Compra compraCortesía = new Compra();
        compraCortesía.setUsuario(embajador);
        compraCortesía.setRifa(rifa);
        compraCortesía.setEstadoPago("CANJEADO");
        compraCortesía.setMontoTotal(BigDecimal.ZERO);
        compraCortesía.setFechaCompra(LocalDateTime.now());
        compraCortesía.setBoletos(List.of(boleto)); 
        compraCortesía = compraRepository.save(compraCortesía);

        boleto.setEstado("CANJEADO");
        boleto.setUsuario(embajador);
        boletoRepository.save(boleto);

        embajador.setSaldoMonedero(BigDecimal.ZERO);
        embajador.setFechaMetaCompletada(null);
        usuarioRepository.save(embajador);

        ra.addFlashAttribute("mensajeExito",
                "El boleto de cortesía asignado es el Boleto no. " + boleto.getNumeroBoleto()
                        + "<br>Tu monedero se ha reiniciado.");
        return "redirect:/mis-compras";
    }
}