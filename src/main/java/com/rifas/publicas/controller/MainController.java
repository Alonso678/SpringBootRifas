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
    private final EmailService emailService;

    // 1. Inyectamos la URL base desde el properties
    @Value("${app.base-url}")
    private String baseUrl;

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

        model.addAttribute("usuario", usuario); // <--- AGREGA ESTO
        model.addAttribute("compras", compras);
        model.addAttribute("totalPendiente", totalPendiente);
        model.addAttribute("tienePendientes", tienePendientes);
        model.addAttribute("rifasActivas", rifaRepository.findByEstado("ACTIVA"));

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
        model.addAttribute("nuevaRifa", new Rifa());
        return "admin/form";
    }

    @GetMapping("/admin/rifas/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id, Model model) {
        Rifa rifa = rifaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rifa no encontrada"));
        model.addAttribute("nuevaRifa", rifa);
        return "admin/form";
    }

    @PostMapping("/admin/rifas/guardar")
    public String guardarRifa(@ModelAttribute("nuevaRifa") Rifa rifa) {
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

        // Alterna correctamente entre ACTIVA y FINALIZADA respetando tu modelo
        rifa.setEstado("ACTIVA".equals(rifa.getEstado()) ? "FINALIZADA" : "ACTIVA");

        rifaRepository.save(rifa);

        // Redirige de vuelta al panel de administración de rifas
        return "redirect:/admin/rifas";
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

        // --- INICIO DE LÓGICA DE REFERIDOS ---
        if ("PAGADO".equals(estado) && !"PAGADO".equals(compra.getEstadoPago())) {
            Usuario comprador = compra.getUsuario();
            if (comprador.getReferidoPor() != null) {
                Usuario embajador = comprador.getReferidoPor();

                // 1. Acumular el 10% del total
                BigDecimal comision = compra.getMontoTotal().multiply(new BigDecimal("0.10"));
                embajador.setSaldoMonedero(embajador.getSaldoMonedero().add(comision));

                // 2. Verificar si alcanzó la meta de $150 y no estaba bloqueado
                if (embajador.getSaldoMonedero().compareTo(new BigDecimal("150")) >= 0
                        && embajador.getFechaMetaCompletada() == null) {
                    embajador.setFechaMetaCompletada(LocalDateTime.now());
                }

                // 3. Incrementar contador de ventas si está bloqueado
                if (embajador.isReclamoBloqueado()) {
                    embajador.setBoletosVendidosTrasBloqueo(embajador.getBoletosVendidosTrasBloqueo() + 1);
                    // Si vendió 5, desbloqueamos
                    if (embajador.getBoletosVendidosTrasBloqueo() >= 5) {
                        embajador.setReclamoBloqueado(false);
                        embajador.setBoletosVendidosTrasBloqueo(0);
                    }
                }
                usuarioRepository.save(embajador);
            }
        }
        // --- FIN DE LÓGICA DE REFERIDOS ---

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

        // Validar el tiempo de las 24 horas
        if (embajador.getFechaMetaCompletada() != null &&
                embajador.getFechaMetaCompletada().isBefore(LocalDateTime.now().minusHours(24))) {

            embajador.setReclamoBloqueado(true);
            embajador.setFechaMetaCompletada(null);
            usuarioRepository.save(embajador);
            ra.addFlashAttribute("mensajeError", "Tiempo de 24h excedido. Reclamo bloqueado hasta vender 5 boletos.");
            return "redirect:/mis-compras";
        }

        // --- RESETEAR MONEDERO ---
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

        // Validar el tiempo de las 24 horas también para el boleto
        if (embajador.getFechaMetaCompletada() != null &&
                embajador.getFechaMetaCompletada().isBefore(LocalDateTime.now().minusHours(24))) {

            embajador.setReclamoBloqueado(true);
            embajador.setFechaMetaCompletada(null);
            usuarioRepository.save(embajador);
            ra.addFlashAttribute("mensajeError", "Tiempo de 24h excedido. Reclamo bloqueado hasta vender 5 boletos.");
            return "redirect:/mis-compras";
        }

        // 1. Buscar la rifa seleccionada para cumplir con la relación obligatoria
        Rifa rifa = rifaRepository.findById(rifaId).orElse(null);
        if (rifa == null) {
            ra.addFlashAttribute("mensajeError", "La rifa seleccionada no es válida.");
            return "redirect:/mis-compras";
        }

        // 2. Buscar exactamente UN boleto disponible en esa rifa
        Boleto boleto = boletoRepository.findByRifaIdAndEstadoOrderByNumeroBoletoAsc(rifaId, "DISPONIBLE")
        .stream()
        .findFirst()
        .orElse(null);

        if (boleto == null) {
            ra.addFlashAttribute("mensajeError", "Lo sentimos, ya no hay boletos disponibles para esta rifa.");
            return "redirect:/mis-compras";
        }

        // 3. Crear el registro de Compra de cortesía (con montoTotal en 0 y su Rifa
        // asociada)
        Compra compraCortesía = new Compra();
        compraCortesía.setUsuario(embajador);
        compraCortesía.setRifa(rifa);
        compraCortesía.setEstadoPago("CANJEADO");
        compraCortesía.setMontoTotal(BigDecimal.ZERO);
        compraCortesía.setFechaCompra(LocalDateTime.now());
        compraCortesía.setBoletos(List.of(boleto)); // Asignamos el boleto a la lista de la compra
        compraCortesía = compraRepository.save(compraCortesía);

        // 4. Actualizar el estado del boleto
        boleto.setEstado("CANJEADO");
        boleto.setUsuario(embajador);
        boletoRepository.save(boleto);

        // 5. Resetear monedero del embajador
        embajador.setSaldoMonedero(BigDecimal.ZERO);
        embajador.setFechaMetaCompletada(null);
        usuarioRepository.save(embajador);

        ra.addFlashAttribute("mensajeExito",
                "El boleto de cortesía asignado es el Boleto no. " + boleto.getNumeroBoleto()
                        + "<br>Tu monedero se ha reiniciado.");
        return "redirect:/mis-compras";
    }
}